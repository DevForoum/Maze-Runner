package com.mazerunner.view;

import com.mazerunner.model.LevelConfig;
import com.mazerunner.model.LevelManager;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * LevelSelectPanel — shown after the user clicks PLAY on the main menu.
 *
 * Displays one card per level (title + short description + grid size).
 * Clicking a card starts the game at that level.
 * A Back button returns to the main menu.
 *
 * Design: dark slate background matching the rest of the game UI,
 * coloured accent bar per level, hover highlight.
 */
public class LevelSelectPanel extends JPanel {

    /** Callback so Main knows which level was chosen or if user went Back. */
    public interface SelectCallback {
        /** Called when the user picks a level. startIndex is 0-based. */
        void onLevelSelected(int startIndex);
        /** Called when the user clicks Back. */
        void onBack();
    }

    // ── Card layout constants ──────────────────────────────────────────────
    private static final int CARD_W      = 560;
    private static final int CARD_H      = 80;
    private static final int CARD_GAP    = 16;
    private static final int CARD_RADIUS = 14;

    // Accent colours per level (index 0-3, rest use the last colour)
    private static final Color[] ACCENT = {
            new Color(52, 211, 153),   // Level 1 – emerald green
            new Color(59, 130, 246),   // Level 2 – sky blue
            new Color(245, 158, 11),   // Level 3 – amber
            new Color(239, 68,  68),   // Level 4 – red (danger)
    };

    private final SelectCallback callback;
    private final List<LevelConfig> levels = new ArrayList<>();

    /** Which card the mouse is currently hovering over (-1 = none). */
    private int hoveredIndex = -1;
    /** Whether the Back button is hovered. */
    private boolean backHovered = false;

    // ── Back button geometry (computed in paintComponent) ─────────────────
    private int backX, backY, backW, backH;

    public LevelSelectPanel(SelectCallback callback) {
        this.callback = callback;

        setBackground(new Color(15, 18, 24));
        setFocusable(true);
        setCursor(Cursor.getDefaultCursor());

        // Pull levels from a temporary LevelManager just to read their configs
        LevelManager lm = new LevelManager();
        for (int i = 0; i < lm.getTotalStaticLevels(); i++) {
            levels.add(lm.getCurrentLevel());
            lm.advanceLevel();
        }

        // ── Mouse interaction ──────────────────────────────────────────────
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int prev = hoveredIndex;
                boolean prevBack = backHovered;
                hoveredIndex = getCardAt(e.getX(), e.getY());
                backHovered  = isOverBack(e.getX(), e.getY());
                if (hoveredIndex != prev || backHovered != prevBack) {
                    setCursor(hoveredIndex >= 0 || backHovered
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = getCardAt(e.getX(), e.getY());
                if (idx >= 0) {
                    callback.onLevelSelected(idx);
                } else if (isOverBack(e.getX(), e.getY())) {
                    callback.onBack();
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(700, 600);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // ── Background gradient ────────────────────────────────────────────
        g2.setPaint(new GradientPaint(0, 0, new Color(15, 18, 24), 0, h, new Color(22, 28, 42)));
        g2.fillRect(0, 0, w, h);

        // ── Title ──────────────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.setColor(Color.WHITE);
        String heading = "SELECT LEVEL";
        FontMetrics hfm = g2.getFontMetrics();
        g2.drawString(heading, (w - hfm.stringWidth(heading)) / 2, 60);

        // Thin decorative line under heading
        g2.setColor(new Color(59, 130, 246));
        int lineW = 180;
        g2.fillRoundRect((w - lineW) / 2, 68, lineW, 3, 3, 3);

        // ── Level cards ───────────────────────────────────────────────────
        int cardX = (w - CARD_W) / 2;
        int startY = 100;

        for (int i = 0; i < levels.size(); i++) {
            LevelConfig cfg = levels.get(i);
            int cardY = startY + i * (CARD_H + CARD_GAP);
            drawCard(g2, cfg, i, cardX, cardY);
        }

        // ── Back button ───────────────────────────────────────────────────
        backW = 120;
        backH = 38;
        backX = (w - backW) / 2;
        backY = startY + levels.size() * (CARD_H + CARD_GAP) + 20;

        Color backBg = backHovered ? new Color(55, 65, 90) : new Color(35, 42, 60);
        g2.setColor(backBg);
        g2.fillRoundRect(backX, backY, backW, backH, 10, 10);
        g2.setColor(new Color(160, 174, 192));
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        g2.drawRoundRect(backX, backY, backW, backH, 10, 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        FontMetrics bfm = g2.getFontMetrics();
        g2.drawString("← BACK", backX + (backW - bfm.stringWidth("← BACK")) / 2,
                backY + (backH + bfm.getAscent() - bfm.getDescent()) / 2);
    }

    /** Draws one level card at the given position. */
    private void drawCard(Graphics2D g2, LevelConfig cfg, int index, int x, int y) {
        boolean hovered = (index == hoveredIndex);

        // Card background
        Color cardBg = hovered ? new Color(32, 40, 58) : new Color(22, 28, 42);
        g2.setColor(cardBg);
        g2.fillRoundRect(x, y, CARD_W, CARD_H, CARD_RADIUS, CARD_RADIUS);

        // Left accent bar
        Color accent = index < ACCENT.length ? ACCENT[index] : ACCENT[ACCENT.length - 1];
        g2.setColor(hovered ? accent.brighter() : accent);
        g2.fillRoundRect(x, y, 6, CARD_H, CARD_RADIUS, CARD_RADIUS);

        // Border
        g2.setColor(hovered ? accent : new Color(45, 55, 72));
        g2.setStroke(new java.awt.BasicStroke(1.8f));
        g2.drawRoundRect(x, y, CARD_W, CARD_H, CARD_RADIUS, CARD_RADIUS);

        // Level number badge
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(accent);
        g2.drawString("LVL " + cfg.getLevelNumber(), x + 20, y + 28);

        // Level title
        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.setColor(Color.WHITE);
        g2.drawString(cfg.getTitle(), x + 20, y + 50);

        // Grid size tag (right side)
        String sizeTag = cfg.getCols() + "×" + cfg.getRows();
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(160, 174, 192));
        FontMetrics sfm = g2.getFontMetrics();
        g2.drawString(sizeTag, x + CARD_W - sfm.stringWidth(sizeTag) - 16,
                y + CARD_H / 2 + sfm.getAscent() / 2);

        // Feature tags (small pills on second row right side)
        drawFeatureTags(g2, cfg, x + CARD_W - 16, y + CARD_H - 18, accent);
    }

    /** Draws small feature tags (PORTALS, CHASER, etc.) anchored to the right. */
    private void drawFeatureTags(Graphics2D g2, LevelConfig cfg, int rightEdge, int y, Color accent) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g2.getFontMetrics();

        List<String> tags = new ArrayList<>();
        if (cfg.isChaserEnabled())      tags.add("CHASER");
        if (cfg.isCrackedWallsEnabled()) tags.add("CRACKED WALLS");
        if (cfg.isPortalsEnabled())      tags.add("PORTALS");
        if (cfg.isGravityEnabled())      tags.add("GRAVITY");

        int rx = rightEdge;
        for (String tag : tags) {
            int tw = fm.stringWidth(tag) + 8;
            rx -= tw;
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
            g2.fillRoundRect(rx, y, tw, 13, 4, 4);
            g2.setColor(accent);
            g2.drawString(tag, rx + 4, y + 10);
            rx -= 4;
        }
    }

    // ── Hit-test helpers ───────────────────────────────────────────────────

    /** Returns the card index under (mx, my), or -1 if none. */
    private int getCardAt(int mx, int my) {
        int cardX = (getWidth() - CARD_W) / 2;
        int startY = 100;
        for (int i = 0; i < levels.size(); i++) {
            int cardY = startY + i * (CARD_H + CARD_GAP);
            if (mx >= cardX && mx <= cardX + CARD_W && my >= cardY && my <= cardY + CARD_H) {
                return i;
            }
        }
        return -1;
    }

    /** Returns true if (mx, my) is over the Back button. */
    private boolean isOverBack(int mx, int my) {
        return mx >= backX && mx <= backX + backW && my >= backY && my <= backY + backH;
    }
}
