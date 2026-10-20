package com.mazerunner.model;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SoundManager — Low-latency audio engine for Maze Runner.
 *
 * All sounds are synthesized as PCM byte arrays and pre-loaded into
 * javax.sound.sampled.Clip objects at construction time. Because clips
 * are already open and in memory, playback latency is ~1-3 ms (just a
 * frame-position reset + start call), versus the old approach of opening
 * a new SourceDataLine per play (~50-100 ms).
 *
 * Each clip entry holds two alternating Clip instances (ping-pong pool)
 * so rapid re-triggers (e.g. move sounds) never cut each other off.
 */
public class SoundManager {

    // ── Public sound ID constants ─────────────────────────────────────────
    public static final String MOVE           = "MOVE";
    public static final String GEM_PICKUP     = "GEM_PICKUP";
    public static final String PORTAL_WHOOSH  = "PORTAL_WHOOSH";
    public static final String WALL_SMASH     = "WALL_SMASH";
    public static final String LEVEL_COMPLETE = "LEVEL_COMPLETE";
    public static final String TICK_WARNING   = "TICK_WARNING";

    private static final int SAMPLE_RATE = 44100;
    private static SoundManager instance;

    /** Two clips per sound for ping-pong re-trigger without cutoff */
    private final Map<String, Clip[]> clipPool = new HashMap<>();
    private final Map<String, Integer> pingPong = new HashMap<>();
    private boolean muted = false;

    // ─────────────────────────────────────────────────────────────────────
    // Singleton
    // ─────────────────────────────────────────────────────────────────────

    private SoundManager() {
        register(MOVE,           buildMove(),           2);
        register(GEM_PICKUP,     buildGemPickup(),      2);
        register(PORTAL_WHOOSH,  buildPortalWhoosh(),   1);
        register(WALL_SMASH,     buildWallSmash(),      2);
        register(LEVEL_COMPLETE, buildLevelComplete(),  1);
        register(TICK_WARNING,   buildTickWarning(),    2);
    }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    public void setMuted(boolean m) { muted = m; }
    public boolean isMuted()        { return muted; }
    public void toggleMute()        { muted = !muted; }

    /**
     * Play a sound immediately with near-zero latency.
     * Selects the next clip in the ping-pong pool so rapid re-triggers
     * overlap naturally instead of cutting the previous instance off.
     */
    public void play(String id) {
        if (muted) return;
        Clip[] pool = clipPool.get(id);
        if (pool == null) return;

        int idx = pingPong.getOrDefault(id, 0);
        pingPong.put(id, (idx + 1) % pool.length);

        Clip clip = pool[idx];
        // Reset + play on the EDT-safe calling thread (clips are thread-safe)
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Registration helper
    // ─────────────────────────────────────────────────────────────────────

    private void register(String id, byte[] pcm, int poolSize) {
        AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        Clip[] pool = new Clip[poolSize];
        for (int i = 0; i < poolSize; i++) {
            try {
                DataLine.Info info = new DataLine.Info(Clip.class, fmt);
                if (!AudioSystem.isLineSupported(info)) return;
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(fmt, pcm, 0, pcm.length);
                pool[i] = clip;
            } catch (Exception e) {
                // Audio system unavailable on this machine — skip silently
                return;
            }
        }
        clipPool.put(id, pool);
        pingPong.put(id, 0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sound Synthesis  (all samples are 16-bit signed PCM, little-endian)
    // ─────────────────────────────────────────────────────────────────────

    /** Write a normalised amplitude value [-1,1] as two bytes (little-endian 16-bit) */
    private static void writeS16(byte[] buf, int byteOffset, double amplitude) {
        int v = (int) Math.max(-32767, Math.min(32767, amplitude * 32000));
        buf[byteOffset]     = (byte)(v & 0xFF);
        buf[byteOffset + 1] = (byte)((v >> 8) & 0xFF);
    }

    private static byte[] allocSec(double seconds) {
        return new byte[(int)(SAMPLE_RATE * seconds) * 2];
    }

    // ── MOVE: crisp mechanical "tick" — very short noise-burst + 1.2 kHz sine ──
    private byte[] buildMove() {
        byte[] buf = allocSec(0.045);
        int n = buf.length / 2;
        java.util.Random rng = new java.util.Random(7);
        for (int i = 0; i < n; i++) {
            double t   = (double) i / SAMPLE_RATE;
            double env = Math.exp(-t * 90.0);                          // sharp decay
            double sine  = 0.55 * Math.sin(2 * Math.PI * 1200 * t);
            double noise = 0.45 * (rng.nextDouble() * 2 - 1);
            writeS16(buf, i * 2, (sine + noise) * env * 0.55);
        }
        return buf;
    }

    // ── GEM PICKUP: bright 4-note sparkle C5→E5→G5→C6 with harmonic shimmer ──
    private byte[] buildGemPickup() {
        double[] freqs = {523.25, 659.25, 783.99, 1046.5};
        int segSamples  = (int)(SAMPLE_RATE * 0.075);
        int gapSamples  = (int)(SAMPLE_RATE * 0.012);
        byte[] buf = new byte[freqs.length * (segSamples + gapSamples) * 2];
        int byteIdx = 0;
        for (double freq : freqs) {
            for (int i = 0; i < segSamples; i++, byteIdx += 2) {
                double t   = (double) i / SAMPLE_RATE;
                // Bell-like envelope: fast rise, slow decay
                double env = Math.sin(Math.PI * (double) i / segSamples)
                           * Math.pow(1.0 - (double) i / segSamples, 0.5);
                // Fundamental + 2nd harmonic for shimmer
                double wave = 0.65 * Math.sin(2 * Math.PI * freq      * t)
                            + 0.25 * Math.sin(2 * Math.PI * freq * 2  * t)
                            + 0.10 * Math.sin(2 * Math.PI * freq * 3  * t);
                writeS16(buf, byteIdx, wave * env * 0.75);
            }
            // Brief silent gap between notes
            for (int i = 0; i < gapSamples; i++, byteIdx += 2) {
                writeS16(buf, byteIdx, 0);
            }
        }
        return buf;
    }

    // ── PORTAL WHOOSH: rising pitch-sweep 300→1800 Hz with dual-oscillator chorus ──
    private byte[] buildPortalWhoosh() {
        byte[] buf = allocSec(0.32);
        int n = buf.length / 2;
        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double t        = (double) i / SAMPLE_RATE;
            // Exponential frequency sweep feels faster and more dramatic than linear
            double freq     = 300 * Math.pow(6.0, progress);
            // Slight detuned second oscillator creates chorus width
            double freq2    = freq * 1.018;
            // Envelope: fade in briefly, sustain, fade out
            double env      = Math.sin(Math.PI * progress) * 0.9 + 0.1;
            double wave = 0.5 * Math.sin(2 * Math.PI * freq  * t)
                        + 0.5 * Math.sin(2 * Math.PI * freq2 * t);
            writeS16(buf, i * 2, wave * env * 0.7);
        }
        return buf;
    }

    // ── WALL SMASH: pitched-down drum punch — exponential pitch sweep 200→40 Hz + noise burst ──
    private byte[] buildWallSmash() {
        byte[] buf = allocSec(0.38);
        int n = buf.length / 2;
        java.util.Random rng = new java.util.Random(13);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t    = (double) i / SAMPLE_RATE;
            // Pitch exponentially drops from 200 Hz to ~40 Hz (kick-drum curve)
            double freq = 200 * Math.exp(-t * 9.0) + 40;
            phase      += 2 * Math.PI * freq / SAMPLE_RATE;

            double bodyEnv  = Math.exp(-t * 9.0);   // punchy body decay
            double tailEnv  = Math.exp(-t * 4.0);   // noise tail

            double tone  = Math.sin(phase);          // pitched body
            double noise = (rng.nextDouble() * 2 - 1) * 0.35; // texture

            // Transient click at onset for physical impact feel
            double click = (i < 200) ? (rng.nextDouble() * 2 - 1) * 0.8 : 0;

            double sample = tone * bodyEnv * 0.75
                          + noise * tailEnv * 0.4
                          + click;
            writeS16(buf, i * 2, Math.max(-1.0, Math.min(1.0, sample)) * 0.85);
        }
        return buf;
    }

    // ── LEVEL COMPLETE: triumphant G-major rising fanfare ──
    private byte[] buildLevelComplete() {
        // G4 B4 D5 G5 — with each note ringing over the next (overlapping)
        double[] freqs  = {392.0, 493.88, 587.33, 783.99};
        double[] starts = {0.00,  0.13,   0.26,   0.39};   // seconds
        double noteDur  = 0.22;
        double totalDur = 0.65;
        byte[] buf = allocSec(totalDur);
        int n = buf.length / 2;

        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sample = 0;
            for (int k = 0; k < freqs.length; k++) {
                double noteT = t - starts[k];
                if (noteT < 0 || noteT > noteDur) continue;
                double env  = Math.sin(Math.PI * noteT / noteDur);
                // Harmonics for richness
                double wave = 0.60 * Math.sin(2 * Math.PI * freqs[k]      * noteT)
                            + 0.25 * Math.sin(2 * Math.PI * freqs[k] * 2  * noteT)
                            + 0.10 * Math.sin(2 * Math.PI * freqs[k] * 3  * noteT)
                            + 0.05 * Math.sin(2 * Math.PI * freqs[k] * 4  * noteT);
                sample += wave * env * 0.55;
            }
            writeS16(buf, i * 2, Math.max(-1.0, Math.min(1.0, sample)));
        }
        return buf;
    }

    // ── TICK WARNING: sharp precise beep at 880 Hz ──
    private byte[] buildTickWarning() {
        byte[] buf = allocSec(0.06);
        int n = buf.length / 2;
        for (int i = 0; i < n; i++) {
            double t   = (double) i / SAMPLE_RATE;
            double env = Math.sin(Math.PI * (double) i / n);  // smooth bell shape
            writeS16(buf, i * 2, Math.sin(2 * Math.PI * 880 * t) * env * 0.7);
        }
        return buf;
    }
}
