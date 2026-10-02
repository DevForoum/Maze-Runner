package com.mazerunner.controller;

import com.mazerunner.model.Direction;
import com.mazerunner.model.LevelConfig;
import com.mazerunner.model.LevelManager;
import com.mazerunner.model.MazeModel;
import com.mazerunner.model.SlideResult;
import com.mazerunner.view.MazePanel;

import javax.swing.Timer;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Controller orchestrating user input, rotation transformations,
 * gravity slide animations, wall impact resolutions, and level transitions.
 */
public class GameController extends KeyAdapter {
    private final MazeModel model;
    private final MazePanel panel;
    private final LevelManager levelManager;

    private boolean isSliding = false;
    private Timer slideAnimationTimer = null;
    private final Timer levelClockTimer;

    public GameController(MazeModel model, MazePanel panel, LevelManager levelManager) {
        this.model = model;
        this.panel = panel;
        this.levelManager = levelManager;

        // 1-second interval level clock timer
        this.levelClockTimer = new Timer(1000, e -> {
            if (!model.isGoalReached()) {
                model.tickTimer();
                panel.repaint();
            }
        });
        this.levelClockTimer.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Level Won / Advance state
        if (model.isGoalReached()) {
            if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
                advanceToNextLevel();
            } else if (keyCode == KeyEvent.VK_R) {
                restartLevel();
            }
            return;
        }

        // Lock inputs while in gravity slide animation
        if (isSliding) {
            return;
        }

        LevelConfig config = model.getLevelConfig();

        switch (keyCode) {
            // Screen-relative directional movement
            case KeyEvent.VK_UP -> handleMovement(Direction.UP);
            case KeyEvent.VK_DOWN -> handleMovement(Direction.DOWN);
            case KeyEvent.VK_LEFT -> handleMovement(Direction.LEFT);
            case KeyEvent.VK_RIGHT -> handleMovement(Direction.RIGHT);

            // Smooth 90° Maze Rotations (decoupled from gravity drop)
            case KeyEvent.VK_Q -> {
                if (config != null && config.isRotationEnabled() && !panel.isRotating()) {
                    model.rotateCounterClockwise();
                    panel.rotateDegrees(-90);
                }
            }
            case KeyEvent.VK_E -> {
                if (config != null && config.isRotationEnabled() && !panel.isRotating()) {
                    model.rotateClockwise();
                    panel.rotateDegrees(90);
                }
            }

            // Dedicated Gravity Drop / Slide Trigger (Separate from rotation)
            case KeyEvent.VK_SPACE, KeyEvent.VK_G -> {
                if (config != null && config.isGravityEnabled() && !panel.isRotating()) {
                    triggerGravitySlide();
                }
            }

            // Hold-to-reveal radar path (press starts showing)
            case KeyEvent.VK_S -> {
                if (!model.isShowingPath()) {
                    model.solveBFS();
                    panel.repaint();
                }
            }

            // Level reset
            case KeyEvent.VK_R -> restartLevel();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // As long as player holds S, path is shown; releasing S hides it
        if (e.getKeyCode() == KeyEvent.VK_S) {
            model.clearPath();
            panel.repaint();
        }
    }

    private void handleMovement(Direction screenDir) {
        if (panel.isRotating()) return;
        // Translate screen perspective to grid coordinate space based on rotation
        Direction gridDir = model.mapScreenToGrid(screenDir);
        boolean moved = model.movePlayer(gridDir);
        if (moved) {
            panel.repaint();
        }
    }

    /**
     * Executes kinetic gravity slide along the environmental gravity vector.
     * Animates momentum buildup and processes wall destruction upon impact.
     */
    private void triggerGravitySlide() {
        SlideResult result = model.simulateSlide();
        List<Point> steps = result.getPathCoordinates();

        // No sliding possible if blocked immediately
        if (steps.size() <= 1) {
            return;
        }

        isSliding = true;

        if (slideAnimationTimer != null && slideAnimationTimer.isRunning()) {
            slideAnimationTimer.stop();
        }

        final int totalSteps = steps.size();
        final int[] currentStepIndex = {0};

        // Slide animation timer (~40ms per cell traversed)
        slideAnimationTimer = new Timer(40, null);
        slideAnimationTimer.addActionListener(e -> {
            currentStepIndex[0]++;

            if (currentStepIndex[0] < totalSteps) {
                Point stepCoord = steps.get(currentStepIndex[0]);
                model.setPlayerPosition(stepCoord.y, stepCoord.x);

                // Pass progressive momentum and trail for visual buildup
                List<Point> subTrail = steps.subList(0, currentStepIndex[0] + 1);
                panel.setMomentum(currentStepIndex[0], subTrail);
            } else {
                // Completed slide: process impact
                slideAnimationTimer.stop();
                isSliding = false;

                // Wall destruction and impact effects
                if (result.isWallBroken()) {
                    panel.triggerScreenShake(18.0);
                    Point impact = result.getImpactCell();
                    if (impact != null) {
                        panel.spawnDebrisAt(impact.y, impact.x, result.getImpactWallDirection());
                    }
                } else if (result.hadHighMomentum()) {
                    // Solid wall slam with high momentum
                    panel.triggerScreenShake(11.0);
                }

                // Reset momentum indicators after a brief stabilization delay
                Timer coolDownTimer = new Timer(180, ev -> {
                    panel.setMomentum(0, null);
                    ((Timer) ev.getSource()).stop();
                });
                coolDownTimer.setRepeats(false);
                coolDownTimer.start();
            }
        });

        slideAnimationTimer.start();
    }

    private void advanceToNextLevel() {
        levelManager.advanceLevel();
        LevelConfig nextConfig = levelManager.getCurrentLevel();
        model.initLevel(nextConfig);
        panel.resetAngles();
        panel.setMomentum(0, null);
        panel.repaint();
    }

    private void restartLevel() {
        if (slideAnimationTimer != null && slideAnimationTimer.isRunning()) {
            slideAnimationTimer.stop();
            isSliding = false;
        }
        model.initLevel(levelManager.getCurrentLevel());
        panel.resetAngles();
        panel.setMomentum(0, null);
        panel.repaint();
    }
}
