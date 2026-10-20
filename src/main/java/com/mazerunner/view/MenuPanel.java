package com.mazerunner.view;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MenuPanel — Cinematic Main Menu for Maze Runner.
 *
 * Phase 1 (Home Screen): Displays the full, high-brilliance artwork as the launch cover.
 * Phase 2 (Transition): The artwork smoothly fades into a semi-transparent, glowing backdrop.
 * Phase 3 (Interactive Menu): The menu buttons (PLAY, CONTROLS, QUIT) fade in with keyboard & mouse navigation.
 */
public class MenuPanel extends JPanel {

    public interface MenuCallback {
        void onPlay();
        void onQuit();
    }

    private static final String[] MENU_ITEMS = {"PLAY", "CONTROLS", "QUIT"};
    private static final int ITEM_PLAY     = 0;
    private static final int ITEM_CONTROLS = 1;
    private static final int ITEM_QUIT     = 2;

    private int selectedIndex = 0;
    private boolean showControls = false;

    private final MenuCallback callback;

    // ── Intro & Transition State ──────────────────────────────────────────
    private boolean inSplashMode = true;
    private boolean transitioning = false;
    private float transitionProgress = 0.0f; // 0.0 = full artwork showcase, 1.0 = interactive menu backdrop
    private long splashStartTime;

    // ── Artwork Image ─────────────────────────────────────────────────────
    private BufferedImage bgArtwork = null;

    // ── Animation Loop & Starfield ────────────────────────────────────────
    private final Timer animTimer;
    private final List<Star> stars = new ArrayList<>();
    private final Random rng = new Random();

    public MenuPanel(MenuCallback callback) {
        this.callback = callback;
        setPreferredSize(new Dimension(820, 680));
        setBackground(new Color(5, 7, 12));
        setFocusable(true);

        loadArtwork();

        // Starfield particles in the background
        for (int i = 0; i < 70; i++) {
            stars.add(new Star());
        }

        splashStartTime = System.currentTimeMillis();

        // 60 FPS animation loop
        animTimer = new Timer(16, e -> {
            updateTransition();
            repaint();
        });
        animTimer.start();

        // Keyboard navigation
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });

        // Mouse click triggers transition or selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (inSplashMode) {
                    startTransition();
                } else if (!showControls) {
                    // Check if clicked near an item
                    int clickedItem = getMenuItemAtPoint(e.getX(), e.getY());
                    if (clickedItem >= 0) {
                        selectedIndex = clickedItem;
                        confirmSelection();
                    }
                } else {
                    showControls = false;
                    repaint();
                }
            }
        });
    }

    private void loadArtwork() {
        try {
            InputStream is = getClass().getResourceAsStream("/menu_bg.jpg");
            if (is != null) {
                bgArtwork = ImageIO.read(is);
            }
        } catch (Exception ignored) {}

        if (bgArtwork == null) {
            try {
                File f = new File("menu_bg.jpg");
                if (f.exists()) {
                    bgArtwork = ImageIO.read(f);
                } else {
                    File f2 = new File("src/main/resources/menu_bg.jpg");
                    if (f2.exists()) {
                        bgArtwork = ImageIO.read(f2);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void startTransition() {
        if (inSplashMode && !transitioning) {
            transitioning = true;
        }
    }

    private void updateTransition() {
        // Auto-start transition after 3.5 seconds if user hasn't pressed any key yet
        if (inSplashMode && !transitioning && System.currentTimeMillis() - splashStartTime > 3500) {
            transitioning = true;
        }

        if (transitioning) {
            transitionProgress += 0.04f;
            if (transitionProgress >= 1.0f) {
                transitionProgress = 1.0f;
                transitioning = false;
                inSplashMode = false;
            }
        }
    }

    private void handleKey(int keyCode) {
        if (inSplashMode) {
            startTransition();
            return;
        }

        if (showControls) {
            showControls = false;
            repaint();
            return;
        }

        switch (keyCode) {
            case KeyEvent.VK_UP -> {
                selectedIndex = (selectedIndex - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
                repaint();
            }
            case KeyEvent.VK_DOWN -> {
                selectedIndex = (selectedIndex + 1) % MENU_ITEMS.length;
                repaint();
            }
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> confirmSelection();
        }
    }

    private void confirmSelection() {
        switch (selectedIndex) {
            case ITEM_PLAY -> {
                animTimer.stop();
                callback.onPlay();
            }
            case ITEM_CONTROLS -> {
                showControls = true;
                repaint();
            }
            case ITEM_QUIT -> System.exit(0);
        }
    }

    private int getMenuItemAtPoint(int mx, int my) {
        int w = getWidth(), h = getHeight();
        int itemStartY = h / 2 + 55;
        int itemSpacing = 52;
        int barW = 280, barH = 42;
        int barX = (w - barW) / 2;

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            int iy = itemStartY + i * itemSpacing;
            int barY = iy - 30;
            if (mx >= barX && mx <= barX + barW && my >= barY && my <= barY + barH) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        long tick = System.currentTimeMillis();

        // ── 1. Base dark backdrop ──
        g2d.setColor(new Color(6, 8, 14));
        g2d.fillRect(0, 0, w, h);

        // ── 2. Ambient background starfield ──
        for (Star star : stars) {
            star.update(w, h);
            star.draw(g2d);
        }

        // ── 3. Render Artwork (Home Screen or Dimmed Menu Backdrop) ──
        if (bgArtwork != null) {
            drawArtworkBackdrop(g2d, w, h);
        }

        // ── 4. Phase 1: Splash Mode (Prompt overlay) ──
        if (inSplashMode) {
            drawSplashPrompt(g2d, w, h, tick);
        }

        // ── 5. Phase 2 & 3: Interactive Menu Overlay ──
        if (transitionProgress > 0.05f) {
            float menuAlpha = Math.min(1.0f, (transitionProgress - 0.05f) / 0.95f);
            Graphics2D menuG2d = (Graphics2D) g2d.create();
            menuG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, menuAlpha));

            if (showControls) {
                drawControlsOverlay(menuG2d, w, h);
            } else {
                drawMenuItems(menuG2d, w, h, tick);
                drawFooter(menuG2d, w, h);
            }
            menuG2d.dispose();
        }
    }

    /**
     * Renders the artwork image centered, scaling smoothly as it transitions
     * from 100% full brilliance down to ~38% opacity with an ambient vignette.
     */
    private void drawArtworkBackdrop(Graphics2D g2d, int w, int h) {
        int imgW = bgArtwork.getWidth();
        int imgH = bgArtwork.getHeight();

        // Calculate aspect-preserving scale to fill viewport nicely
        double scale = Math.max((double) w / imgW, (double) h / imgH);
        int drawW = (int) (imgW * scale);
        int drawH = (int) (imgH * scale);
        int drawX = (w - drawW) / 2;
        int drawY = (h - drawH) / 2;

        // When in splash mode, opacity is 1.0f. During transition, drops smoothly to 0.40f.
        float artworkAlpha = 1.0f - (transitionProgress * 0.62f);
        Graphics2D imgG2d = (Graphics2D) g2d.create();
        imgG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.15f, artworkAlpha)));
        imgG2d.drawImage(bgArtwork, drawX, drawY, drawW, drawH, null);
        imgG2d.dispose();

        // Ambient dark vignette overlay when transitioning to menu mode for text contrast
        if (transitionProgress > 0.0f) {
            float vignetteAlpha = transitionProgress * 0.58f;
            Graphics2D vigG2d = (Graphics2D) g2d.create();
            vigG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, vignetteAlpha));
            GradientPaint vignette = new GradientPaint(0, 0, new Color(5, 7, 12, 120),
                    0, h, new Color(5, 7, 12, 220));
            vigG2d.setPaint(vignette);
            vigG2d.fillRect(0, 0, w, h);
            vigG2d.dispose();
        }
    }

    private void drawSplashPrompt(Graphics2D g2d, int w, int h, long tick) {
        float promptAlpha = (1.0f - transitionProgress);
        if (promptAlpha <= 0.02f) return;

        double pulse = 0.65 + 0.35 * Math.sin(tick / 320.0);
        int alphaInt = (int) (Math.min(255, 255 * pulse * promptAlpha));

        Graphics2D promptG2d = (Graphics2D) g2d.create();
        promptG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, promptAlpha));

        String prompt = "PRESS ANY KEY TO START";
        promptG2d.setFont(new Font("SansSerif", Font.BOLD, 17));
        FontMetrics fm = promptG2d.getFontMetrics();
        int pw = fm.stringWidth(prompt) + 40;
        int ph = 38;
        int px = (w - pw) / 2;
        int py = h - 68;

        // Glowing backdrop capsule
        promptG2d.setColor(new Color(0, 0, 0, (int)(160 * promptAlpha)));
        promptG2d.fillRoundRect(px, py, pw, ph, 20, 20);

        promptG2d.setColor(new Color(0, 220, 255, (int)(100 * pulse * promptAlpha)));
        promptG2d.setStroke(new BasicStroke(1.8f));
        promptG2d.drawRoundRect(px, py, pw, ph, 20, 20);

        // Glowing text
        promptG2d.setColor(new Color(0, 240, 255, alphaInt));
        promptG2d.drawString(prompt, px + 20, py + 25);

        promptG2d.dispose();
    }

    private void drawMenuItems(Graphics2D g2d, int w, int h, long tick) {
        int itemStartY = h / 2 + 55;
        int itemSpacing = 52;

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            boolean selected = (i == selectedIndex);
            int iy = itemStartY + i * itemSpacing;

            int barW = 270, barH = 42;
            int barX = (w - barW) / 2;
            int barY = iy - 30;

            if (selected) {
                double pulse = 0.7 + 0.3 * Math.sin(tick / 350.0);

                // Selection glowing box
                g2d.setColor(new Color(6, 182, 212, (int)(60 * pulse)));
                g2d.fillRoundRect(barX, barY, barW, barH, 12, 12);

                g2d.setColor(new Color(34, 211, 238, (int)(210 * pulse)));
                g2d.setStroke(new BasicStroke(2.2f));
                g2d.drawRoundRect(barX, barY, barW, barH, 12, 12);

                // Animated side arrows ▶ ◀
                g2d.setColor(new Color(34, 211, 238));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
                int arrowOff = (int)(4 * Math.sin(tick / 240.0));
                g2d.drawString("▶", barX - 26 + arrowOff, iy - 2);
                g2d.drawString("◀", barX + barW + 10 - arrowOff, iy - 2);

                g2d.setFont(new Font("SansSerif", Font.BOLD, 21));
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(new Color(15, 23, 42, 130));
                g2d.fillRoundRect(barX, barY, barW, barH, 12, 12);
                g2d.setColor(new Color(51, 65, 85, 140));
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawRoundRect(barX, barY, barW, barH, 12, 12);

                g2d.setFont(new Font("SansSerif", Font.PLAIN, 19));
                g2d.setColor(new Color(148, 163, 184));
            }

            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(MENU_ITEMS[i], (w - fm.stringWidth(MENU_ITEMS[i])) / 2, iy - 2);
        }
    }

    private void drawControlsOverlay(Graphics2D g2d, int w, int h) {
        g2d.setColor(new Color(0, 0, 0, 215));
        g2d.fillRect(0, 0, w, h);

        int boxW = 500, boxH = 430;
        int boxX = (w - boxW) / 2, boxY = (h - boxH) / 2;

        g2d.setColor(new Color(15, 23, 42));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2d.setColor(new Color(6, 182, 212));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        // Header
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2d.setColor(new Color(34, 211, 238));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "CONTROLS REFERENCE";
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 42);

        g2d.setColor(new Color(51, 65, 85));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawLine(boxX + 20, boxY + 56, boxX + boxW - 20, boxY + 56);

        String[][] controls = {
                {"↑ ↓ ← → / WASD",  "Move player in corridor"},
                {"Q / E",           "Rotate maze 90° (smooth ease-out)"},
                {"SPACE / G",       "Kinetic gravity drop (slides player & chaser)"},
                {"Hold S",          "Active BFS radar path reveal"},
                {"R",               "Instant level retry"},
                {"ENTER / SPACE",   "Advance to next stage upon victory"},
                {"M",               "Toggle mute audio"},
                {"Tactical Stun",   "High-momentum slides stun the chaser for ~3.5s"}
        };

        int rowY = boxY + 84;
        int rowH = 36;
        for (int i = 0; i < controls.length; i++) {
            int ry = rowY + i * rowH;

            if (i % 2 == 0) {
                g2d.setColor(new Color(255, 255, 255, 8));
                g2d.fillRoundRect(boxX + 14, ry - 17, boxW - 28, 27, 6, 6);
            }

            // Key badge
            g2d.setColor(new Color(30, 41, 59));
            g2d.fillRoundRect(boxX + 20, ry - 15, 145, 23, 6, 6);
            g2d.setColor(new Color(6, 182, 212));
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawRoundRect(boxX + 20, ry - 15, 145, 23, 6, 6);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2d.setColor(Color.WHITE);
            fm = g2d.getFontMetrics();
            String key = controls[i][0];
            g2d.drawString(key, boxX + 20 + (145 - fm.stringWidth(key)) / 2, ry + 2);

            // Description
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(new Color(203, 213, 225));
            g2d.drawString(controls[i][1], boxX + 180, ry + 2);
        }

        g2d.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g2d.setColor(new Color(148, 163, 184));
        fm = g2d.getFontMetrics();
        String hint = "Press any key to return";
        g2d.drawString(hint, boxX + (boxW - fm.stringWidth(hint)) / 2, boxY + boxH - 16);
    }

    private void drawFooter(Graphics2D g2d, int w, int h) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(100, 116, 139));
        FontMetrics fm = g2d.getFontMetrics();
        String hint = "↑ ↓ Navigate  •  ENTER Confirm  •  CLICK with Mouse";
        g2d.drawString(hint, (w - fm.stringWidth(hint)) / 2, h - 22);
    }

    /** Twinkling star particle in background */
    private class Star {
        double x, y, size;
        float alpha;
        double speed, phase;

        Star() { reset(820, 680); }

        void reset(int w, int h) {
            x = rng.nextDouble() * w;
            y = rng.nextDouble() * h;
            size = 0.5 + rng.nextDouble() * 1.8;
            speed = 0.08 + rng.nextDouble() * 0.22;
            phase = rng.nextDouble() * Math.PI * 2;
            alpha = 0.2f + rng.nextFloat() * 0.5f;
        }

        void update(int w, int h) {
            y += speed;
            if (y > h) { y = 0; x = rng.nextDouble() * w; }
        }

        void draw(Graphics2D g2d) {
            long t = System.currentTimeMillis();
            float brightness = (float)(0.4 + 0.6 * Math.abs(Math.sin(t / 1100.0 + phase)));
            g2d.setColor(new Color(1.0f, 1.0f, 1.0f, alpha * brightness));
            int d = Math.max(1, (int) size);
            g2d.fillOval((int) x, (int) y, d, d);
        }
    }
}
