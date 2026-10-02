package com.mazerunner.view;

import com.mazerunner.model.Cell;
import com.mazerunner.model.Direction;
import com.mazerunner.model.LevelConfig;
import com.mazerunner.model.MazeModel;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * High-performance double-buffered rendering panel.
 * Handles smooth animated AffineTransform maze rotation (~300ms easing),
 * visual gravity indicator, dynamic cracked walls,
 * momentum visual trail, screen shake, and floating game HUD.
 */
public class MazePanel extends JPanel {
    private static final int VIEW_SIZE = 720;
    private static final int TOP_HUD_HEIGHT = 70;
    private static final int BOTTOM_HUD_HEIGHT = 45;

    private final MazeModel model;
    private final Random random = new Random();

    // Smooth Rotation Easing (~300ms ease-out)
    private double currentRenderAngle = 0.0;
    private double targetRenderAngle = 0.0;

    // Camera & Screen Shake
    private double shakeIntensity = 0.0;
    private double shakeOffsetX = 0.0;
    private double shakeOffsetY = 0.0;
    private final Timer animationTimer;

    // Momentum & Trail Visuals
    private int currentMomentum = 0;
    private final List<Point> momentumTrail = new ArrayList<>();
    private final List<DebrisParticle> particles = new ArrayList<>();

    public MazePanel(MazeModel model) {
        this.model = model;

        setPreferredSize(new Dimension(VIEW_SIZE, VIEW_SIZE + TOP_HUD_HEIGHT + BOTTOM_HUD_HEIGHT));
        setBackground(new Color(15, 18, 24)); // Modern dark charcoal slate
        setFocusable(true);

        // 60 FPS animation loop (smooth rotation easing, screen shake, particles)
        animationTimer = new Timer(16, e -> updateVisualEffects());
        animationTimer.start();
    }

    public void rotateDegrees(double delta) {
        this.targetRenderAngle += delta;
    }

    public boolean isRotating() {
        return Math.abs(targetRenderAngle - currentRenderAngle) > 0.8;
    }

    public void resetAngles() {
        this.currentRenderAngle = 0.0;
        this.targetRenderAngle = 0.0;
    }

    public void triggerScreenShake(double intensity) {
        this.shakeIntensity = Math.min(22.0, intensity);
    }

    public void setMomentum(int momentum, List<Point> trail) {
        this.currentMomentum = momentum;
        this.momentumTrail.clear();
        if (trail != null) {
            this.momentumTrail.addAll(trail);
        }
        repaint();
    }

    public void spawnDebris(int cellRow, int cellCol, Direction wallDir, int cellSize, int originX, int originY) {
        int cx = originX + cellCol * cellSize + cellSize / 2;
        int cy = originY + cellRow * cellSize + cellSize / 2;

        for (int i = 0; i < 32; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = 2.0 + random.nextDouble() * 6.0;
            double vx = Math.cos(angle) * speed + wallDir.getDc() * 2.5;
            double vy = Math.sin(angle) * speed + wallDir.getDr() * 2.5;
            Color color = random.nextBoolean() ? new Color(255, 165, 0) : new Color(255, 215, 0);
            particles.add(new DebrisParticle(cx, cy, vx, vy, color, 22 + random.nextInt(16)));
        }
    }

    private void updateVisualEffects() {
        boolean repaintNeeded = false;

        // Smooth rotation easing (~300ms ease-out)
        double angleDiff = targetRenderAngle - currentRenderAngle;
        if (Math.abs(angleDiff) > 0.2) {
            currentRenderAngle += angleDiff * 0.16;
            repaintNeeded = true;
        } else if (currentRenderAngle != targetRenderAngle) {
            currentRenderAngle = targetRenderAngle;
            repaintNeeded = true;
        }

        // Damp screen shake
        if (shakeIntensity > 0.1) {
            shakeOffsetX = (random.nextDouble() - 0.5) * 2 * shakeIntensity;
            shakeOffsetY = (random.nextDouble() - 0.5) * 2 * shakeIntensity;
            shakeIntensity *= 0.82;
            repaintNeeded = true;
        } else {
            if (shakeOffsetX != 0 || shakeOffsetY != 0) {
                shakeOffsetX = 0;
                shakeOffsetY = 0;
                repaintNeeded = true;
            }
            shakeIntensity = 0;
        }

        // Update debris particles
        if (!particles.isEmpty()) {
            Iterator<DebrisParticle> it = particles.iterator();
            while (it.hasNext()) {
                DebrisParticle p = it.next();
                p.update();
                if (p.isDead()) {
                    it.remove();
                }
            }
            repaintNeeded = true;
        }

        if (repaintNeeded) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Apply screen shake translation
        AffineTransform originalTransform = g2d.getTransform();
        g2d.translate(shakeOffsetX, shakeOffsetY);

        // Maze viewport calculations
        int mazeAreaWidth = getWidth();
        int mazeAreaHeight = getHeight() - TOP_HUD_HEIGHT - BOTTOM_HUD_HEIGHT;
        int maxGridDimension = Math.max(model.getRows(), model.getCols());
        int cellSize = Math.min((mazeAreaWidth - 60) / maxGridDimension, (mazeAreaHeight - 60) / maxGridDimension);
        cellSize = Math.max(cellSize, 16);

        int gridPixelWidth = model.getCols() * cellSize;
        int gridPixelHeight = model.getRows() * cellSize;
        int originX = (mazeAreaWidth - gridPixelWidth) / 2;
        int originY = TOP_HUD_HEIGHT + (mazeAreaHeight - gridPixelHeight) / 2;

        int centerX = originX + gridPixelWidth / 2;
        int centerY = originY + gridPixelHeight / 2;

        // Render Rotated Maze Canvas with smooth interpolated angle
        AffineTransform mazeTransform = g2d.getTransform();
        g2d.rotate(Math.toRadians(currentRenderAngle), centerX, centerY);

        // Grid background backdrop
        g2d.setColor(new Color(24, 28, 38));
        g2d.fillRect(originX, originY, gridPixelWidth, gridPixelHeight);

        // Render Cells and Solution Path
        Cell[][] grid = model.getGrid();
        for (int r = 0; r < model.getRows(); r++) {
            for (int c = 0; c < model.getCols(); c++) {
                drawCell(g2d, grid[r][c], originX, originY, cellSize);
            }
        }

        // Render Goal and Player
        drawGoal(g2d, originX, originY, cellSize);
        drawMomentumTrail(g2d, originX, originY, cellSize);
        drawPlayer(g2d, originX, originY, cellSize);

        // Restore transform for particles
        g2d.setTransform(mazeTransform);

        // Render Debris Particles
        for (DebrisParticle p : particles) {
            p.draw(g2d);
        }

        // Restore base transform for fixed upright HUD
        g2d.setTransform(originalTransform);
        drawHUD(g2d);

        // Render Win / Game Over Overlay
        if (model.isGoalReached()) {
            drawWinOverlay(g2d);
        } else if (model.isTimeExpired()) {
            drawGameOverOverlay(g2d);
        }
    }

    private void drawCell(Graphics2D g2d, Cell cell, int originX, int originY, int cellSize) {
        int x = originX + cell.getCol() * cellSize;
        int y = originY + cell.getRow() * cellSize;

        // Breadth-First Search (BFS) shortest path highlight
        if (cell.isPath()) {
            g2d.setColor(new Color(255, 215, 0, 110)); // Radiant gold
            g2d.fillRect(x + 2, y + 2, cellSize - 4, cellSize - 4);
        }

        boolean[] walls = cell.getWalls();
        boolean[] cracked = cell.getCracked();

        for (int i = 0; i < 4; i++) {
            if (!walls[i]) continue;

            Direction dir = Direction.fromIndex(i);
            boolean isCracked = cracked[i];

            if (isCracked) {
                // Highly visible cracked wall: bright amber/orange with dashed fracture pattern
                g2d.setColor(new Color(255, 140, 0));
                g2d.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 3.0f}, 0.0f));
            } else {
                // Standard structural wall: Solid crisp off-white
                g2d.setColor(new Color(230, 235, 245));
                g2d.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }

            switch (dir) {
                case UP -> g2d.drawLine(x, y, x + cellSize, y);
                case RIGHT -> g2d.drawLine(x + cellSize, y, x + cellSize, y + cellSize);
                case DOWN -> g2d.drawLine(x, y + cellSize, x + cellSize, y + cellSize);
                case LEFT -> g2d.drawLine(x, y, x, y + cellSize);
            }
        }
    }

    private void drawMomentumTrail(Graphics2D g2d, int originX, int originY, int cellSize) {
        if (momentumTrail.size() <= 1) return;

        for (int i = 0; i < momentumTrail.size() - 1; i++) {
            Point p = momentumTrail.get(i);
            int tx = originX + p.x * cellSize + 4;
            int ty = originY + p.y * cellSize + 4;
            float alpha = (float) (i + 1) / (float) momentumTrail.size() * 0.45f;
            g2d.setColor(new Color(0.2f, 0.9f, 0.6f, alpha));
            g2d.fillRect(tx, ty, cellSize - 8, cellSize - 8);
        }
    }

    private void drawPlayer(Graphics2D g2d, int originX, int originY, int cellSize) {
        int px = originX + model.getPlayerCol() * cellSize + 4;
        int py = originY + model.getPlayerRow() * cellSize + 4;
        int pSize = cellSize - 8;

        // Player glow based on momentum accumulation
        if (currentMomentum >= 2) {
            g2d.setColor(new Color(255, 120, 0, 160)); // Fiery kinetic boost glow
            g2d.fillRoundRect(px - 3, py - 3, pSize + 6, pSize + 6, 8, 8);
            g2d.setColor(new Color(255, 235, 60)); // Electric kinetic core
        } else if (currentMomentum == 1) {
            g2d.setColor(new Color(0, 220, 255, 140)); // Speed cyan glow
            g2d.fillRoundRect(px - 2, py - 2, pSize + 4, pSize + 4, 6, 6);
            g2d.setColor(new Color(100, 255, 200));
        } else {
            g2d.setColor(new Color(40, 220, 120)); // Standard emerald green
        }

        g2d.fillRoundRect(px, py, pSize, pSize, 6, 6);

        // Core highlight
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillRoundRect(px + 2, py + 2, Math.max(3, pSize / 3), Math.max(3, pSize / 3), 3, 3);
    }

    private void drawGoal(Graphics2D g2d, int originX, int originY, int cellSize) {
        int gx = originX + (model.getCols() - 1) * cellSize + 4;
        int gy = originY + (model.getRows() - 1) * cellSize + 4;
        int gSize = cellSize - 8;

        // Radiant crimson goal
        g2d.setColor(new Color(240, 50, 75, 130));
        g2d.fillRoundRect(gx - 2, gy - 2, gSize + 4, gSize + 4, 8, 8);
        g2d.setColor(new Color(255, 55, 80));
        g2d.fillRoundRect(gx, gy, gSize, gSize, 6, 6);

        // Inner marker
        g2d.setColor(Color.WHITE);
        g2d.fillOval(gx + gSize / 2 - 3, gy + gSize / 2 - 3, 6, 6);
    }

    private void drawHUD(Graphics2D g2d) {
        LevelConfig config = model.getLevelConfig();

        // Top HUD Header background
        g2d.setColor(new Color(26, 32, 44));
        g2d.fillRect(0, 0, getWidth(), TOP_HUD_HEIGHT);
        g2d.setColor(new Color(45, 55, 72));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(0, TOP_HUD_HEIGHT, getWidth(), TOP_HUD_HEIGHT);

        // Level Title & Number
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 17));
        String titleStr = (config != null) ? "Level " + config.getLevelNumber() + ": " + config.getTitle() : "Maze Runner";
        g2d.drawString(titleStr, 20, 27);

        // Live Timer Badge
        String timeStr = (config != null && config.getTimeLimitSeconds() > 0)
                ? "TIME: " + model.getTimeDisplay()
                : "ELAPSED: " + model.getTimeDisplay();
        boolean isLowTime = config != null && config.getTimeLimitSeconds() > 0 && model.getTimeRemainingSeconds() <= 15;
        Color timeBg = isLowTime ? new Color(220, 38, 38) : new Color(31, 41, 55);
        int timeX = (config != null && config.isGravityEnabled()) ? getWidth() - 365 : getWidth() - 140;
        drawBadge(g2d, timeStr, timeBg, timeX, 22);

        // Feature Badges
        int badgeX = 20;
        int badgeY = 40;
        if (config != null) {
            if (config.isRotationEnabled()) {
                badgeX = drawBadge(g2d, "ROTATE: Q / E", new Color(79, 70, 229), badgeX, badgeY);
            }
            if (config.isCrackedWallsEnabled()) {
                int remaining = model.getRemainingCrackedWallsCount();
                badgeX = drawBadge(g2d, "CRACKED WALLS: " + remaining + " REMAINING", new Color(217, 119, 6), badgeX, badgeY);
            }
            if (model.isShowingPath()) {
                badgeX = drawBadge(g2d, "RADAR PATH ACTIVE", new Color(16, 185, 129), badgeX, badgeY);
            }
        }

        // Visual Gravity Direction Widget (Compass & Drop hint)
        if (config != null && config.isGravityEnabled()) {
            drawGravityWidget(g2d, getWidth() - 250, 10);
        } else if (currentMomentum > 0) {
            String momText = currentMomentum >= 2 ? "MOMENTUM: " + currentMomentum + "x (SMASH!)" : "MOMENTUM: 1x";
            Color momColor = currentMomentum >= 2 ? new Color(239, 68, 68) : new Color(59, 130, 246);
            drawBadge(g2d, momText, momColor, getWidth() - 220, 22);
        }

        // Bottom HUD Footer
        int footerY = getHeight() - BOTTOM_HUD_HEIGHT;
        g2d.setColor(new Color(26, 32, 44));
        g2d.fillRect(0, footerY, getWidth(), BOTTOM_HUD_HEIGHT);
        g2d.setColor(new Color(45, 55, 72));
        g2d.drawLine(0, footerY, getWidth(), footerY);

        g2d.setColor(new Color(160, 174, 192));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String instruct = (config != null) ? config.getInstructions() : "";
        g2d.drawString(instruct, 20, footerY + 26);

        String controls = "[R] Reset | [HOLD S] Radar Path";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(controls, getWidth() - fm.stringWidth(controls) - 20, footerY + 26);
    }

    private void drawGravityWidget(Graphics2D g2d, int x, int y) {
        int w = 230;
        int h = 48;

        // Background box
        g2d.setColor(new Color(17, 24, 39));
        g2d.fillRoundRect(x, y, w, h, 10, 10);
        g2d.setColor(new Color(59, 130, 246));
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect(x, y, w, h, 10, 10);

        // Downward Gravity Icon & Label
        g2d.setColor(new Color(96, 165, 250));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("GRAVITY: \u2193 DOWN", x + 12, y + 22);

        // Action trigger label
        g2d.setColor(new Color(243, 244, 246));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.drawString("Press [SPACE] or [G] to Drop", x + 12, y + 38);

        // Visual Downward Arrow Indicator
        int arrowX = x + w - 32;
        int arrowY = y + 14;
        g2d.setColor(new Color(59, 130, 246));
        g2d.fillOval(arrowX - 2, arrowY - 2, 24, 24);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.drawString("\u2193", arrowX + 5, arrowY + 16);
    }

    private int drawBadge(Graphics2D g2d, String text, Color bg, int x, int y) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text) + 12;
        int height = 18;

        g2d.setColor(bg);
        g2d.fillRoundRect(x, y, width, height, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x + 6, y + 13);

        return x + width + 8;
    }

    private void drawWinOverlay(Graphics2D g2d) {
        // Dark translucent overlay
        g2d.setColor(new Color(0, 0, 0, 185));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Modal Box
        int boxW = 440;
        int boxH = 220;
        int boxX = (getWidth() - boxW) / 2;
        int boxY = (getHeight() - boxH) / 2;

        g2d.setColor(new Color(26, 32, 44));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2d.setColor(new Color(72, 187, 120));
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // Win Title
        g2d.setColor(new Color(72, 187, 120));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 26));
        String winTitle = "STAGE COMPLETE!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(winTitle, boxX + (boxW - fm.stringWidth(winTitle)) / 2, boxY + 60);

        // Subtitle
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
        String sub = "Target exit reached successfully.";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, boxX + (boxW - fm.stringWidth(sub)) / 2, boxY + 105);

        // Prompt
        g2d.setColor(new Color(246, 224, 94));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        String prompt = "Press [ENTER] or [SPACE] for Next Level";
        fm = g2d.getFontMetrics();
        g2d.drawString(prompt, boxX + (boxW - fm.stringWidth(prompt)) / 2, boxY + 160);
    }

    private void drawGameOverOverlay(Graphics2D g2d) {
        // Dark red-tinted translucent overlay
        g2d.setColor(new Color(20, 5, 5, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Modal Box
        int boxW = 440;
        int boxH = 220;
        int boxX = (getWidth() - boxW) / 2;
        int boxY = (getHeight() - boxH) / 2;

        g2d.setColor(new Color(30, 16, 18));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2d.setColor(new Color(239, 68, 68));
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // Title
        g2d.setColor(new Color(239, 68, 68));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 26));
        String title = "TIME'S UP!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 60);

        // Subtitle
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
        String sub = "The clock ran out before you reached the exit.";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, boxX + (boxW - fm.stringWidth(sub)) / 2, boxY + 105);

        // Prompt
        g2d.setColor(new Color(246, 224, 94));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        String prompt = "Press [R] to Retry Level";
        fm = g2d.getFontMetrics();
        g2d.drawString(prompt, boxX + (boxW - fm.stringWidth(prompt)) / 2, boxY + 160);
    }

    /**
     * Particle effect for smashed cracked walls.
     */
    private static class DebrisParticle {
        double x, y, vx, vy;
        final Color color;
        int life;
        final int maxLife;

        DebrisParticle(double x, double y, double vx, double vy, Color color, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.maxLife = maxLife;
            this.life = maxLife;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.25; // gravity fall
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Graphics2D g2d) {
            float alpha = (float) life / (float) maxLife;
            g2d.setColor(new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha));
            g2d.fillRect((int) x, (int) y, 4, 4);
        }
    }
}