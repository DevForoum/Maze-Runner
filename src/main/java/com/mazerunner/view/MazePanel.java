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
 * momentum visual trail, screen shake, floating game HUD,
 * and the menacing Chaser adversary.
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

    /**
     * MVC-clean debris spawner: computes all pixel geometry internally from cell coords.
     */
    public void spawnDebrisAt(int cellRow, int cellCol, Direction wallDir) {
        int mazeAreaWidth = getWidth();
        int mazeAreaHeight = getHeight() - TOP_HUD_HEIGHT - BOTTOM_HUD_HEIGHT;
        int maxGridDimension = Math.max(model.getRows(), model.getCols());
        int cellSize = Math.min((mazeAreaWidth - 60) / maxGridDimension, (mazeAreaHeight - 60) / maxGridDimension);
        cellSize = Math.max(cellSize, 16);
        int gridPixelWidth = model.getCols() * cellSize;
        int gridPixelHeight = model.getRows() * cellSize;
        int originX = (mazeAreaWidth - gridPixelWidth) / 2;
        int originY = TOP_HUD_HEIGHT + (mazeAreaHeight - gridPixelHeight) / 2;

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

        // Render Goal, Portals, Gems, Trail, Player and Chaser
        drawGoal(g2d, originX, originY, cellSize);
        drawGemsAndPortals(g2d, originX, originY, cellSize);
        drawMomentumTrail(g2d, originX, originY, cellSize);
        drawPlayer(g2d, originX, originY, cellSize);
        drawChaser(g2d, originX, originY, cellSize);

        // Restore transform for particles
        g2d.setTransform(mazeTransform);

        // Render Debris Particles
        for (DebrisParticle p : particles) {
            p.draw(g2d);
        }

        // Restore base transform for fixed upright HUD
        g2d.setTransform(originalTransform);
        drawHUD(g2d);

        // Render Win / Caught Overlay
        if (model.isGoalReached()) {
            drawWinOverlay(g2d);
        } else if (model.isCaughtByChaser()) {
            drawCaughtOverlay(g2d);
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
                // Fractured / Structural weak wall styling
                g2d.setColor(new Color(245, 158, 11)); // Amber alert
                g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{3, 3}, 0));
            } else {
                // Reinforced solid structural boundary
                g2d.setColor(new Color(110, 130, 160));
                g2d.setStroke(new BasicStroke(2.5f));
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
        if (momentumTrail.isEmpty()) return;

        int alphaStep = Math.max(15, 120 / momentumTrail.size());
        for (int i = 0; i < momentumTrail.size(); i++) {
            Point p = momentumTrail.get(i);
            int tx = originX + p.x * cellSize + 4;
            int ty = originY + p.y * cellSize + 4;
            int tSize = cellSize - 8;

            int alpha = Math.min(180, (i + 1) * alphaStep);
            g2d.setColor(new Color(255, 140, 0, alpha));
            g2d.fillRoundRect(tx, ty, tSize, tSize, 6, 6);
        }
    }

    private void drawPlayer(Graphics2D g2d, int originX, int originY, int cellSize) {
        int px = originX + model.getPlayerCol() * cellSize + 4;
        int py = originY + model.getPlayerRow() * cellSize + 4;
        int pSize = cellSize - 8;

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

    private void drawChaser(Graphics2D g2d, int originX, int originY, int cellSize) {
        if (!model.hasChaser()) return;
        com.mazerunner.model.ChaserEntity chaser = model.getChaser();
        int cx = originX + chaser.getCol() * cellSize + cellSize / 2;
        int cy = originY + chaser.getRow() * cellSize + cellSize / 2;
        int r = Math.max(5, cellSize / 2 - 3);

        long time = System.currentTimeMillis();

        if (chaser.isStunned()) {
            // Stunned: Dizzy, gray/amber pulsating core
            g2d.setColor(new Color(245, 158, 11, 140));
            g2d.fillOval(cx - r - 2, cy - r - 2, (r + 2) * 2, (r + 2) * 2);
            g2d.setColor(new Color(156, 163, 175));
            g2d.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Orbiting dizzy stars
            g2d.setColor(new Color(253, 224, 71));
            int numStars = 3;
            double angleOffset = (time / 180.0);
            for (int i = 0; i < numStars; i++) {
                double a = angleOffset + (i * (2 * Math.PI / numStars));
                int sx = cx + (int) (Math.cos(a) * (r + 5));
                int sy = cy + (int) (Math.sin(a) * (r + 4));
                g2d.fillOval(sx - 2, sy - 2, 4, 4);
            }
        } else {
            // Active hunting: Sinister pulsing crimson aura
            double pulse = 0.75 + 0.25 * Math.sin(time / 200.0);
            int auraSize = (int) ((r + 4) * pulse);
            g2d.setColor(new Color(239, 68, 68, 120));
            g2d.fillOval(cx - auraSize, cy - auraSize, auraSize * 2, auraSize * 2);

            // Chaser body
            g2d.setColor(new Color(185, 28, 28));
            g2d.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Menacing glowing eyes
            int eyeOffset = Math.max(2, r / 3);
            int eyeSize = Math.max(2, r / 3);
            g2d.setColor(new Color(254, 240, 138));
            g2d.fillOval(cx - eyeOffset - eyeSize / 2, cy - eyeSize / 2, eyeSize, eyeSize);
            g2d.fillOval(cx + eyeOffset - eyeSize / 2, cy - eyeSize / 2, eyeSize, eyeSize);
        }
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

    private void drawGemsAndPortals(Graphics2D g2d, int originX, int originY, int cellSize) {
        long tick = System.currentTimeMillis();
        Cell[][] grid = model.getGrid();

        for (int r = 0; r < model.getRows(); r++) {
            for (int c = 0; c < model.getCols(); c++) {
                Cell cell = grid[r][c];
                int x = originX + c * cellSize;
                int y = originY + r * cellSize;
                int cx = x + cellSize / 2;
                int cy = y + cellSize / 2;
                int halfSize = Math.max(3, cellSize / 3);

                if (cell.hasGem()) {
                    double pulse = 0.75 + 0.25 * Math.sin((tick / 350.0) + r + c);
                    int glowSize = (int) (halfSize * 1.5 * pulse);

                    // Diamond glow halo
                    g2d.setColor(new Color(6, 182, 212, 100));
                    int[] gx = {cx, cx + glowSize, cx, cx - glowSize};
                    int[] gy = {cy - glowSize, cy, cy + glowSize, cy};
                    g2d.fillPolygon(gx, gy, 4);

                    // Diamond core
                    g2d.setColor(new Color(34, 211, 238));
                    int[] dx = {cx, cx + halfSize, cx, cx - halfSize};
                    int[] dy = {cy - halfSize, cy, cy + halfSize, cy};
                    g2d.fillPolygon(dx, dy, 4);

                    // Inner bright highlight
                    g2d.setColor(Color.WHITE);
                    int s = Math.max(2, halfSize / 2);
                    int[] hx = {cx, cx + s, cx, cx - s};
                    int[] hy = {cy - s, cy, cy + s, cy};
                    g2d.fillPolygon(hx, hy, 4);
                }

                if (cell.isPortal()) {
                    int pId = cell.getPortalId();
                    Color portalColor = pId == 1 ? new Color(6, 182, 212) : new Color(168, 85, 247);
                    Color portalGlow  = pId == 1 ? new Color(6, 182, 212, 60) : new Color(168, 85, 247, 60);

                    double spin = (tick / 400.0) * (pId == 1 ? 1 : -1);
                    int pr = Math.max(4, cellSize / 2 - 2);

                    double pulseP = 0.85 + 0.15 * Math.sin(tick / 280.0);
                    int glowR = (int) (pr * pulseP) + 4;
                    g2d.setColor(portalGlow);
                    g2d.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

                    g2d.setColor(portalColor);
                    g2d.setStroke(new BasicStroke(2.8f));
                    g2d.drawOval(cx - pr, cy - pr, pr * 2, pr * 2);

                    g2d.setStroke(new BasicStroke(2.2f));
                    g2d.drawArc(cx - pr + 3, cy - pr + 3, (pr - 3) * 2, (pr - 3) * 2,
                            (int) Math.toDegrees(spin), 120);

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, Math.max(7, cellSize / 4)));
                    String label = "P" + pId;
                    FontMetrics pfm = g2d.getFontMetrics();
                    g2d.drawString(label, cx - pfm.stringWidth(label) / 2, cy + pfm.getAscent() / 2 - 1);
                }
            }
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(20, 24, 34));
        g2d.fillRect(0, 0, getWidth(), TOP_HUD_HEIGHT);
        g2d.setColor(new Color(45, 55, 72));
        g2d.drawLine(0, TOP_HUD_HEIGHT, getWidth(), TOP_HUD_HEIGHT);

        LevelConfig config = model.getLevelConfig();

        // Level Title & Grid Dimensions
        g2d.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2d.setColor(Color.WHITE);
        String title = (config != null) ? "STAGE " + config.getLevelNumber() + ": " + config.getTitle() : "MAZE RUNNER";
        g2d.drawString(title, 20, 30);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(160, 174, 192));
        g2d.drawString("Grid: " + model.getCols() + "x" + model.getRows(), 20, 48);

        // Timer Display
        int timeLimit = (config != null) ? config.getTimeLimitSeconds() : 0;
        int elapsed = model.getElapsedTimeSeconds();
        int remaining = Math.max(0, timeLimit - elapsed);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 19));
        String timeStr = (timeLimit > 0)
                ? String.format("TIME: %02d:%02d", remaining / 60, remaining % 60)
                : "TIME: " + model.getTimeDisplay();

        Color timerColor = Color.WHITE;
        if (timeLimit > 0) {
            if (remaining <= 10) timerColor = new Color(248, 113, 113); // Urgent red
            else if (remaining <= 25) timerColor = new Color(251, 191, 36); // Warning yellow
            else timerColor = new Color(52, 211, 153); // Comfortable green
        }
        g2d.setColor(timerColor);
        g2d.drawString(timeStr, 220, 30);

        // Feature Badges
        int badgeX = 380;
        int badgeY = 15;

        if (config != null) {
            if (config.isCrackedWallsEnabled()) {
                int crackedRem = model.getRemainingCrackedWallsCount();
                Color cColor = crackedRem > 0 ? new Color(245, 158, 11) : new Color(75, 85, 99);
                badgeX = drawBadge(g2d, "CRACKED WALLS: " + crackedRem, cColor, badgeX, badgeY);
            }

            if (config.isPortalsEnabled()) {
                badgeX = drawBadge(g2d, "PORTALS ACTIVE", new Color(124, 58, 237), badgeX, badgeY);
            }

            int gemsCollected = model.getCurrentStats().getGemsCollected();
            int gemsTotal = config.getGemsCount();
            String gemStars = "\u2605".repeat(gemsCollected) + "\u2606".repeat(gemsTotal - gemsCollected);
            Color gemColor = gemsCollected == gemsTotal ? new Color(16, 185, 129) : new Color(180, 140, 0);
            badgeX = drawBadge(g2d, "GEMS " + gemStars, gemColor, badgeX, badgeY);

            if (model.isShowingPath()) {
                badgeX = drawBadge(g2d, "RADAR PATH ACTIVE", new Color(16, 185, 129), badgeX, badgeY);
            }

            if (model.hasChaser()) {
                com.mazerunner.model.ChaserEntity chaser = model.getChaser();
                if (chaser.isStunned()) {
                    badgeX = drawBadge(g2d, "💫 STUNNED (" + chaser.getStunTicksRemaining() + ")", new Color(180, 83, 9), badgeX, badgeY);
                } else {
                    badgeX = drawBadge(g2d, "⚠ CHASER HUNTING", new Color(185, 28, 28), badgeX, badgeY);
                }
            }
        }

        // Visual Gravity Direction Widget
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
        // Truncate or ellipsis if instruction string is too wide for the panel
        FontMetrics fmInstr = g2d.getFontMetrics();
        int maxInstrWidth = getWidth() - 40;
        if (fmInstr.stringWidth(instruct) > maxInstrWidth) {
            while (instruct.length() > 0 && fmInstr.stringWidth(instruct + "…") > maxInstrWidth) {
                instruct = instruct.substring(0, instruct.length() - 1);
            }
            instruct = instruct + "…";
        }
        g2d.drawString(instruct, 20, footerY + 26);

        // Mute badge
        boolean muted = com.mazerunner.model.SoundManager.getInstance().isMuted();
        String muteLabel = muted ? "🔇 MUTED [M]" : "🔊 [M] Mute";
        Color muteBg = muted ? new Color(120, 30, 30) : new Color(30, 50, 80);
        drawBadge(g2d, muteLabel, muteBg, getWidth() - 130, footerY + 12);

        String controls = "[R] Reset | [HOLD S] Radar";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(new Color(160, 174, 192));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.drawString(controls, getWidth() - fm.stringWidth(controls) - 145, footerY + 26);
    }

    private void drawGravityWidget(Graphics2D g2d, int x, int y) {
        int w = 230;
        int h = 48;

        g2d.setColor(new Color(17, 24, 39));
        g2d.fillRoundRect(x, y, w, h, 10, 10);
        g2d.setColor(new Color(59, 130, 246));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(x, y, w, h, 10, 10);

        int cx = x + 25;
        int cy = y + h / 2;
        int r = 16;
        g2d.setColor(new Color(30, 41, 59));
        g2d.fillOval(cx - r, cy - r, r * 2, r * 2);

        Direction gravDir = model.getGravityDirection();
        g2d.setColor(new Color(239, 68, 68));
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int tipX = cx + (int) (gravDir.getDc() * 11);
        int tipY = cy + (int) (gravDir.getDr() * 11);
        g2d.drawLine(cx, cy, tipX, tipY);
        g2d.fillOval(tipX - 3, tipY - 3, 6, 6);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(new Color(243, 244, 246));
        g2d.drawString("GRAVITY: " + gravDir.name(), x + 52, y + 20);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2d.setColor(new Color(156, 163, 175));
        g2d.drawString("[SPACE/G] Trigger Slide", x + 52, y + 36);
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
        com.mazerunner.model.LevelStats stats = model.getCurrentStats();
        String grade = stats.calculateGrade();
        int stars = stats.getStarsEarned();
        int totalGems = (model.getLevelConfig() != null) ? model.getLevelConfig().getGemsCount() : 3;
        int score = stats.calculateTotalScore();

        g2d.setColor(new Color(0, 0, 0, 195));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int boxW = 500;
        int boxH = 320;
        int boxX = (getWidth() - boxW) / 2;
        int boxY = (getHeight() - boxH) / 2;

        Color gradeColor = switch (grade) {
            case "S" -> new Color(255, 215, 0);   // Gold
            case "A" -> new Color(72, 187, 120);   // Green
            case "B" -> new Color(59, 130, 246);   // Blue
            default  -> new Color(160, 174, 192);  // Grey
        };

        g2d.setColor(new Color(18, 24, 36));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 18, 18);
        g2d.setColor(gradeColor);
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 18, 18);

        g2d.setColor(gradeColor);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        String banner = "STAGE COMPLETE!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(banner, boxX + (boxW - fm.stringWidth(banner)) / 2, boxY + 38);

        int gradeBoxSize = 72;
        int gradeBoxX = boxX + 28;
        int gradeBoxY = boxY + 56;
        g2d.setColor(gradeColor.darker());
        g2d.fillRoundRect(gradeBoxX, gradeBoxY, gradeBoxSize, gradeBoxSize, 14, 14);
        g2d.setColor(gradeColor);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(gradeBoxX, gradeBoxY, gradeBoxSize, gradeBoxSize, 14, 14);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 38));
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.WHITE);
        g2d.drawString(grade, gradeBoxX + (gradeBoxSize - fm.stringWidth(grade)) / 2,
                gradeBoxY + gradeBoxSize / 2 + fm.getAscent() / 2 - 4);

        int starY = gradeBoxY + gradeBoxSize + 10;
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        String starStr = "★".repeat(stars) + "☆".repeat(totalGems - stars);
        fm = g2d.getFontMetrics();
        g2d.setColor(stars == totalGems ? new Color(255, 215, 0) : new Color(120, 120, 120));
        g2d.drawString(starStr, gradeBoxX + (gradeBoxSize - fm.stringWidth(starStr)) / 2, starY + 22);

        int statsX = boxX + 128;
        int statsY = boxY + 62;
        int rowH = 26;

        String[][] rows = {
                {"FINAL SCORE", String.format("%,d pts", score)},
                {"GEMS COLLECTED", stats.getGemsCollected() + " / " + totalGems},
                {"WALLS SMASHED", String.valueOf(stats.getWallsSmashed())},
                {"MOVES / ROTATIONS", stats.getMoveCount() + " / " + stats.getRotationCount()},
                {"TIME TAKEN", formatTime(stats.getTimeTakenSeconds())}
        };

        for (int i = 0; i < rows.length; i++) {
            int ry = statsY + i * rowH;
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(new Color(160, 174, 192));
            g2d.drawString(rows[i][0], statsX, ry);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2d.setColor(Color.WHITE);
            String val = rows[i][1];
            fm = g2d.getFontMetrics();
            g2d.drawString(val, boxX + boxW - 40 - fm.stringWidth(val), ry);
        }

        g2d.setColor(new Color(55, 65, 80));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawLine(boxX + 20, boxY + boxH - 48, boxX + boxW - 20, boxY + boxH - 48);

        g2d.setColor(new Color(246, 224, 94));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        String prompt = "[ ENTER ] or [ SPACE ]  →  Next Level        [ R ]  →  Retry";
        fm = g2d.getFontMetrics();
        g2d.drawString(prompt, boxX + (boxW - fm.stringWidth(prompt)) / 2, boxY + boxH - 20);
    }

    private void drawCaughtOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 205));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int boxW = 480;
        int boxH = 230;
        int boxX = (getWidth() - boxW) / 2;
        int boxY = (getHeight() - boxH) / 2;

        Color dangerColor = new Color(239, 68, 68);

        g2d.setColor(new Color(24, 18, 24));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2d.setColor(dangerColor);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);

        // Banner
        g2d.setColor(dangerColor);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        String title = "CAUGHT BY THE CHASER!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 45);

        // Message
        g2d.setColor(new Color(226, 232, 240));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String msg1 = "The shadow adversary intercepted you in the labyrinth.";
        fm = g2d.getFontMetrics();
        g2d.drawString(msg1, boxX + (boxW - fm.stringWidth(msg1)) / 2, boxY + 80);

        // Hint
        g2d.setColor(new Color(251, 191, 36));
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 13));
        String tip = "Tactical Tip: Rotate the maze and press SPACE/G to slide the chaser!";
        fm = g2d.getFontMetrics();
        g2d.drawString(tip, boxX + (boxW - fm.stringWidth(tip)) / 2, boxY + 115);

        String tip2 = "High-momentum wall impacts will STUN the chaser for ~3.5 seconds.";
        fm = g2d.getFontMetrics();
        g2d.drawString(tip2, boxX + (boxW - fm.stringWidth(tip2)) / 2, boxY + 138);

        // Retry Action
        g2d.setColor(new Color(160, 174, 192));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        String retry = "[R] Restart Level";
        fm = g2d.getFontMetrics();
        g2d.drawString(retry, boxX + (boxW - fm.stringWidth(retry)) / 2, boxY + 190);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
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
