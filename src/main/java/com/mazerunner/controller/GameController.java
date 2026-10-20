package com.mazerunner.controller;

import com.mazerunner.model.Direction;
import com.mazerunner.model.LevelConfig;
import com.mazerunner.model.LevelManager;
import com.mazerunner.model.MazeModel;
import com.mazerunner.model.SlideResult;
import com.mazerunner.model.SoundManager;
import com.mazerunner.view.MazePanel;

import javax.swing.Timer;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

/**
 * Controller orchestrating user input, rotation transformations,
 * gravity slide animations, wall impact resolutions, chaser AI steps, and level transitions.
 */
public class GameController extends KeyAdapter {
    private final MazeModel model;
    private final MazePanel panel;
    private final LevelManager levelManager;

    private boolean isSliding = false;
    private Timer slideAnimationTimer = null;
    private final Timer levelClockTimer;
    private final Timer chaserTimer;

    public GameController(MazeModel model, MazePanel panel, LevelManager levelManager) {
        this.model = model;
        this.panel = panel;
        this.levelManager = levelManager;

        // 1-second interval level clock timer
        this.levelClockTimer = new Timer(1000, e -> {
            if (!model.isGoalReached() && !model.isCaughtByChaser()) {
                model.tickTimer();
                panel.repaint();
            }
        });
        this.levelClockTimer.start();

        // 650ms interval for autonomous Chaser adversary step
        this.chaserTimer = new Timer(650, e -> {
            if (!isSliding && !panel.isRotating() && !model.isGoalReached() && !model.isCaughtByChaser()) {
                if (model.hasChaser()) {
                    model.stepChaser();
                    if (model.isCaughtByChaser()) {
                        SoundManager.getInstance().play(SoundManager.WALL_SMASH);
                        panel.triggerScreenShake(14.0);
                    }
                    panel.repaint();
                }
            }
        });
        this.chaserTimer.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Caught by Chaser state: only allow restart
        if (model.isCaughtByChaser()) {
            if (keyCode == KeyEvent.VK_R) {
                restartLevel();
            }
            return;
        }

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
                    SoundManager.getInstance().play(SoundManager.PORTAL_WHOOSH);
                }
            }
            case KeyEvent.VK_E -> {
                if (config != null && config.isRotationEnabled() && !panel.isRotating()) {
                    model.rotateClockwise();
                    panel.rotateDegrees(90);
                    SoundManager.getInstance().play(SoundManager.PORTAL_WHOOSH);
                }
            }

            // Dedicated Gravity Drop / Slide Trigger (Environmental: affects both player & chaser)
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

            // Mute / unmute all sounds
            case KeyEvent.VK_M -> {
                SoundManager.getInstance().toggleMute();
                panel.repaint();
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
            SoundManager.getInstance().play(SoundManager.MOVE);
            if (model.isCaughtByChaser()) {
                SoundManager.getInstance().play(SoundManager.WALL_SMASH);
                panel.triggerScreenShake(14.0);
            }
            panel.repaint();
        }
    }

    /**
     * Executes kinetic gravity slide along the environmental gravity vector.
     * Environmental gravity pulls BOTH player and the Chaser adversary down simultaneously!
     * Chasers impacting walls with high momentum (>= 2 cells) get stunned.
     */
    private void triggerGravitySlide() {
        SlideResult playerResult = model.simulateSlide();
        SlideResult chaserResult = model.simulateChaserSlide();

        List<Point> playerSteps = playerResult.getPathCoordinates();
        List<Point> chaserSteps = (chaserResult != null) ? chaserResult.getPathCoordinates() : Collections.emptyList();

        boolean playerCanSlide = playerSteps.size() > 1;
        boolean chaserCanSlide = chaserSteps.size() > 1;

        if (!playerCanSlide && !chaserCanSlide) {
            return;
        }

        isSliding = true;

        if (slideAnimationTimer != null && slideAnimationTimer.isRunning()) {
            slideAnimationTimer.stop();
        }

        final int maxSteps = Math.max(playerSteps.size(), chaserSteps.size());
        final int[] currentStepIndex = {0};

        // Slide animation timer (~40ms per cell traversed)
        slideAnimationTimer = new Timer(40, null);
        slideAnimationTimer.addActionListener(e -> {
            currentStepIndex[0]++;
            int step = currentStepIndex[0];

            if (step < maxSteps) {
                if (step < playerSteps.size()) {
                    Point pStep = playerSteps.get(step);
                    model.setPlayerPosition(pStep.y, pStep.x);
                    List<Point> subTrail = playerSteps.subList(0, step + 1);
                    panel.setMomentum(step, subTrail);
                }
                if (step < chaserSteps.size() && model.hasChaser()) {
                    Point cStep = chaserSteps.get(step);
                    model.getChaser().setPosition(cStep.y, cStep.x);
                }
                panel.repaint();
            } else {
                slideAnimationTimer.stop();
                isSliding = false;

                // Wall destruction and impact effects for player
                if (playerResult.isWallBroken()) {
                    SoundManager.getInstance().play(SoundManager.WALL_SMASH);
                    panel.triggerScreenShake(18.0);
                    Point impact = playerResult.getImpactCell();
                    if (impact != null) {
                        panel.spawnDebrisAt(impact.y, impact.x, playerResult.getImpactWallDirection());
                    }
                } else if (playerResult.hadHighMomentum()) {
                    SoundManager.getInstance().play(SoundManager.WALL_SMASH);
                    panel.triggerScreenShake(11.0);
                }

                // Chaser impact stun if high momentum was accumulated
                if (chaserResult != null && chaserResult.hadHighMomentum() && model.hasChaser()) {
                    SoundManager.getInstance().play(SoundManager.WALL_SMASH);
                    panel.triggerScreenShake(9.0);
                }

                // Check capture at end of slide
                if (model.hasChaser() && !model.getChaser().isStunned() &&
                        model.getPlayerRow() == model.getChaser().getRow() &&
                        model.getPlayerCol() == model.getChaser().getCol()) {
                    model.setCaughtByChaser(true);
                    SoundManager.getInstance().play(SoundManager.WALL_SMASH);
                    panel.triggerScreenShake(14.0);
                }

                Timer coolDownTimer = new Timer(180, ev -> {
                    panel.setMomentum(0, null);
                    ((Timer) ev.getSource()).stop();
                });
                coolDownTimer.setRepeats(false);
                coolDownTimer.start();
                panel.repaint();
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
