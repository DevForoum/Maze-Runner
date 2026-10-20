package com.mazerunner.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MenuPanel — Animated main menu screen for Maze Runner.
 *
 * Displays an animated starfield background, a glowing title, and three
 * selectable menu items. Communicates back to Main via a MenuCallback
 * when the user makes a choice.
 *
 * Navigation: UP/DOWN arrows to select; ENTER/SPACE to confirm.
 */
public class MenuPanel extends JPanel {

    // ── Callback interface ────────────────────────────────────────────────
    public interface MenuCallback {
        void onPlay();
        void onQuit();
    }

    // ── Menu item definitions ─────────────────────────────────────────────
    private static final String[] MENU_ITEMS = {"PLAY", "CONTROLS", "QUIT"};
    private static final int ITEM_PLAY     = 0;
    private static final int ITEM_CONTROLS = 1;
    private static final int ITEM_QUIT     = 2;

    private int selectedIndex = 0;
    private boolean showControls = false;

    private final MenuCallback callback;

    // ── Animation state ───────────────────────────────────────────────────
    private final Timer animTimer;
    private final List<Star> stars = new ArrayList<>();
    private final List<FloatingCell> floatingCells = new ArrayList<>();
    private final Random rng = new Random();

    public MenuPanel(MenuCallback callback) {
        this.callback = callback;
        setBackground(new Color(10, 14, 23));
        setFocusable(true);

        // Generate starfield particles
        for (int i = 0; i < 90; i++) stars.add(new Star());
        // Generate background floating maze cells
        for (int i = 0; i < 18; i++) floatingCells.add(new FloatingCell());

        // 60 fps animation loop
        animTimer = new Timer(16, e -> repaint());
        animTimer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
    }

    private void handleKey(int keyCode) {
        if (showControls) {
            // Any key closes controls overlay
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        long tick = System.currentTimeMillis();

        // ── Background gradient ──
        GradientPaint bg = new GradientPaint(0, 0, new Color(10, 14, 23), 0, h, new Color(16, 24, 44));
        g2d.setPaint(bg);
        g2d.fillRect(0, 0, w, h);

        // ── Floating background maze cells ──
        for (FloatingCell fc : floatingCells) {
            fc.update(w, h);
            fc.draw(g2d);
        }

        // ── Starfield ──
        for (Star star : stars) {
            star.update(w, h);
            star.draw(g2d);
        }

        // ── Title glow ──
        drawTitle(g2d, w, h, tick);

        if (showControls) {
            drawControlsOverlay(g2d, w, h);
        } else {
            drawMenuItems(g2d, w, h, tick);
            drawFooter(g2d, w, h);
        }
    }

    private void drawTitle(Graphics2D g2d, int w, int h, long tick) {
        int titleY = h / 4;

        // Animated glow halo
        double glow = 0.55 + 0.45 * Math.sin(tick / 900.0);
        int haloR = 160;
        Color glowColor = new Color(0, 180, 255, (int)(glow * 45));
        g2d.setColor(glowColor);
        g2d.fillOval(w / 2 - haloR, titleY - haloR / 2 - 20, haloR * 2, haloR);

        // Main title — two-tone "MAZE RUNNER"
        g2d.setFont(new Font("SansSerif", Font.BOLD, 58));
        FontMetrics fm = g2d.getFontMetrics();
        String part1 = "MAZE ";
        String part2 = "RUNNER";
        int totalW = fm.stringWidth(part1 + part2);
        int startX = (w - totalW) / 2;

        // Shadow layer
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.drawString(part1 + part2, startX + 3, titleY + 3);

        // "MAZE" in cyan
        g2d.setColor(new Color(0, 220, 255));
        g2d.drawString(part1, startX, titleY);

        // "RUNNER" in warm white
        g2d.setColor(Color.WHITE);
        g2d.drawString(part2, startX + fm.stringWidth(part1), titleY);

        // Subtitle tagline
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fm = g2d.getFontMetrics();
        String tagline = "Rotate. Drop. Smash. Escape.";
        g2d.setColor(new Color(120, 160, 200, 200));
        g2d.drawString(tagline, (w - fm.stringWidth(tagline)) / 2, titleY + 30);
    }

    private void drawMenuItems(Graphics2D g2d, int w, int h, long tick) {
        int itemStartY = h / 2 + 10;
        int itemSpacing = 60;

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            boolean selected = (i == selectedIndex);
            int iy = itemStartY + i * itemSpacing;

            if (selected) {
                // Pulsating selection highlight bar
                double pulse = 0.7 + 0.3 * Math.sin(tick / 350.0);
                int barW = 280, barH = 44;
                int barX = (w - barW) / 2;
                int barY = iy - 32;

                g2d.setColor(new Color(0, 180, 255, (int)(50 * pulse)));
                g2d.fillRoundRect(barX, barY, barW, barH, 12, 12);
                g2d.setColor(new Color(0, 220, 255, (int)(180 * pulse)));
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawRoundRect(barX, barY, barW, barH, 12, 12);

                // Animated side arrows ▶
                g2d.setColor(new Color(0, 220, 255));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
                int arrowOff = (int)(4 * Math.sin(tick / 250.0));
                g2d.drawString("▶", barX - 28 + arrowOff, iy);
                g2d.drawString("◀", barX + barW + 10 - arrowOff, iy);

                g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
                g2d.setColor(new Color(100, 130, 165));
            }

            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(MENU_ITEMS[i], (w - fm.stringWidth(MENU_ITEMS[i])) / 2, iy);
        }
    }

    private void drawControlsOverlay(Graphics2D g2d, int w, int h) {
        // Dark translucent backdrop
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, w, h);

        int boxW = 480, boxH = 400;
        int boxX = (w - boxW) / 2, boxY = (h - boxH) / 2;

        g2d.setColor(new Color(18, 26, 44));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2d.setColor(new Color(0, 180, 255));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        // Header
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2d.setColor(new Color(0, 220, 255));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "CONTROLS";
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 42);

        g2d.setColor(new Color(55, 70, 90));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawLine(boxX + 20, boxY + 55, boxX + boxW - 20, boxY + 55);

        // Two-column key table
        String[][] controls = {
            {"↑ ↓ ← →",         "Move player"},
            {"Q",                "Rotate maze counter-clockwise"},
            {"E",                "Rotate maze clockwise"},
            {"SPACE / G",        "Gravity drop (slide)"},
            {"Hold S",           "Show BFS radar path"},
            {"R",                "Restart current level"},
            {"ENTER / SPACE",    "Next level (when completed)"},
            {"M",                "Toggle mute sound"},
        };

        int rowY = boxY + 82;
        int rowH = 35;
        for (int i = 0; i < controls.length; i++) {
            int ry = rowY + i * rowH;

            // Alternate row tint
            if (i % 2 == 0) {
                g2d.setColor(new Color(255, 255, 255, 10));
                g2d.fillRoundRect(boxX + 14, ry - 16, boxW - 28, 26, 6, 6);
            }

            // Key badge
            g2d.setColor(new Color(40, 60, 90));
            g2d.fillRoundRect(boxX + 20, ry - 14, 130, 22, 6, 6);
            g2d.setColor(new Color(0, 180, 255));
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawRoundRect(boxX + 20, ry - 14, 130, 22, 6, 6);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2d.setColor(Color.WHITE);
            fm = g2d.getFontMetrics();
            String key = controls[i][0];
            g2d.drawString(key, boxX + 20 + (130 - fm.stringWidth(key)) / 2, ry + 2);

            // Description
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(new Color(180, 200, 220));
            g2d.drawString(controls[i][1], boxX + 168, ry + 2);
        }

        // Close hint
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g2d.setColor(new Color(120, 150, 180));
        fm = g2d.getFontMetrics();
        String hint = "Press any key to go back";
        g2d.drawString(hint, boxX + (boxW - fm.stringWidth(hint)) / 2, boxY + boxH - 16);
    }

    private void drawFooter(Graphics2D g2d, int w, int h) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(70, 90, 110));
        FontMetrics fm = g2d.getFontMetrics();
        String hint = "↑ ↓ Navigate   ENTER Confirm";
        g2d.drawString(hint, (w - fm.stringWidth(hint)) / 2, h - 18);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Animation helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Tiny twinkling star particle */
    private class Star {
        double x, y, size;
        float alpha;
        double speed, phase;

        Star() { reset(800, 600); }

        void reset(int w, int h) {
            x = rng.nextDouble() * w;
            y = rng.nextDouble() * h;
            size = 0.5 + rng.nextDouble() * 1.8;
            speed = 0.1 + rng.nextDouble() * 0.25;
            phase = rng.nextDouble() * Math.PI * 2;
            alpha = 0.2f + rng.nextFloat() * 0.6f;
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

    /** Semi-transparent floating maze cell decoration in the background */
    private class FloatingCell {
        double x, y, size, vx, vy, rot, rotSpeed;
        Color color;

        FloatingCell() { reset(800, 600); }

        void reset(int w, int h) {
            x = rng.nextDouble() * w;
            y = rng.nextDouble() * h;
            size = 14 + rng.nextDouble() * 30;
            vx = (rng.nextDouble() - 0.5) * 0.3;
            vy = 0.15 + rng.nextDouble() * 0.25;
            rot = rng.nextDouble() * Math.PI * 2;
            rotSpeed = (rng.nextDouble() - 0.5) * 0.012;
            int r = rng.nextInt(3);
            if (r == 0) color = new Color(0, 80, 140, 25);
            else if (r == 1) color = new Color(0, 50, 100, 18);
            else color = new Color(20, 60, 120, 20);
        }

        void update(int w, int h) {
            x += vx; y += vy; rot += rotSpeed;
            if (y > h + 40) { y = -40; x = rng.nextDouble() * w; }
            if (x < -40) { x = w + 40; }
            if (x > w + 40) { x = -40; }
        }

        void draw(Graphics2D g2d) {
            AffineTransform old = g2d.getTransform();
            g2d.translate(x, y);
            g2d.rotate(rot);
            int s = (int) size;
            g2d.setColor(color);
            g2d.fillRect(-s / 2, -s / 2, s, s);
            g2d.setColor(new Color(0, 120, 200, 18));
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRect(-s / 2, -s / 2, s, s);
            g2d.setTransform(old);
        }
    }
}
