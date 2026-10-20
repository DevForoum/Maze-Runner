package com.mazerunner;

import com.mazerunner.model.Cell;
import com.mazerunner.model.ChaserEntity;
import com.mazerunner.model.Direction;
import com.mazerunner.model.LevelConfig;
import com.mazerunner.model.LevelManager;
import com.mazerunner.model.MazeModel;
import com.mazerunner.model.SlideResult;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class EngineTest {
    public static void main(String[] args) {
        System.out.println("=== RUNNING MAZE RUNNER ENGINE VERIFICATION TESTS ===");
        testLevelManager();
        testRotationAndGravityMapping();
        testElapsedTimerBeyondScoreTarget();
        testPathfindingEveryLevel();
        testPortalPathfindingAndReachability();
        testSlidePhysicsAndCrackedWallDestruction();
        testGuaranteedRunwaysInLevel3();
        testChaserAIAndGravityReaction();
        System.out.println("=== ALL ENGINE TESTS PASSED SUCCESSFULLY! ===");
    }

    private static void testLevelManager() {
        System.out.print("Testing Level Progression... ");
        LevelManager lm = new LevelManager();
        LevelConfig l1 = lm.getCurrentLevel();
        assert l1.getLevelNumber() == 1 : "Level 1 number mismatch";
        assert !l1.isRotationEnabled() : "Level 1 should not have rotation";
        assert l1.getRows() == 8 && l1.getCols() == 8 : "Level 1 grid should be 8x8";

        lm.advanceLevel();
        LevelConfig l2 = lm.getCurrentLevel();
        assert l2.getLevelNumber() == 2 : "Level 2 number mismatch";
        assert l2.isRotationEnabled() && l2.isGravityEnabled() : "Level 2 must have rotation and gravity";

        lm.advanceLevel();
        LevelConfig l3 = lm.getCurrentLevel();
        assert l3.isCrackedWallsEnabled() : "Level 3 must have cracked walls enabled";

        lm.advanceLevel();
        LevelConfig l4 = lm.getCurrentLevel();
        assert l4.getLevelNumber() == 4 : "Level 4 mismatch";
        assert l4.isChaserEnabled() : "Level 4 must have Chaser enabled";
        System.out.println("PASSED");
    }

    private static void testRotationAndGravityMapping() {
        System.out.print("Testing Rotation and Gravity Vector Math... ");
        LevelManager lm = new LevelManager();
        MazeModel model = new MazeModel(lm.getCurrentLevel());

        assert model.getRotationAngle() == 0;
        assert model.getGravityDirection() == Direction.DOWN;

        model.rotateClockwise();
        assert model.getRotationAngle() == 90;
        assert model.getGravityDirection() == Direction.RIGHT;

        model.rotateClockwise();
        assert model.getRotationAngle() == 180;
        assert model.getGravityDirection() == Direction.UP;

        model.rotateClockwise();
        assert model.getRotationAngle() == 270;
        assert model.getGravityDirection() == Direction.LEFT;

        model.rotateClockwise();
        assert model.getRotationAngle() == 0;
        assert model.getGravityDirection() == Direction.DOWN;

        // Screen to grid mapping test
        assert model.mapScreenToGrid(Direction.DOWN) == Direction.DOWN;
        model.rotateClockwise(); // 90°
        assert model.mapScreenToGrid(Direction.DOWN) == Direction.RIGHT;
        assert model.mapScreenToGrid(Direction.UP) == Direction.LEFT;

        System.out.println("PASSED");
    }

    private static void testElapsedTimerBeyondScoreTarget() {
        System.out.print("Testing Elapsed Timer Beyond Score Target... ");
        LevelManager lm = new LevelManager();
        lm.advanceLevel(); // Level 2 has 60s target
        LevelConfig l2 = lm.getCurrentLevel();

        MazeModel model = new MazeModel(l2);
        for (int i = 0; i < 75; i++) {
            model.tickTimer();
        }
        assert model.getElapsedTimeSeconds() == 75 : "Timer should count up indefinitely";
        assert model.getTimeDisplay().equals("01:15") : "Display format mismatch";
        System.out.println("PASSED");
    }

    private static void testPathfindingEveryLevel() {
        System.out.print("Testing BFS Pathfinding Across All Levels... ");
        LevelManager lm = new LevelManager();
        for (int i = 0; i < lm.getTotalStaticLevels(); i++) {
            MazeModel model = new MazeModel(lm.getCurrentLevel());
            model.solveBFS();

            Cell[][] grid = model.getGrid();
            assert grid[0][0].isPath() : "Start must be marked on path in level " + (i + 1);
            assert grid[model.getRows() - 1][model.getCols() - 1].isPath() : "Goal must be on path in level " + (i + 1);
            lm.advanceLevel();
        }
        System.out.println("PASSED");
    }

    private static void testPortalPathfindingAndReachability() {
        System.out.print("Testing Portal-Aware Paths and Level 2 Reachability... ");
        LevelManager lm = new LevelManager();
        lm.advanceLevel(); // to Level 2
        LevelConfig l2 = lm.getCurrentLevel();

        for (int iter = 0; iter < 250; iter++) {
            MazeModel model = new MazeModel(l2);
            model.solveBFS();
            Cell[][] grid = model.getGrid();
            assert grid[l2.getRows() - 1][l2.getCols() - 1].isPath() : "Level 2 goal must always be solvable!";
        }
        System.out.println("PASSED (250 generated level 2 mazes)");
    }

    private static void testSlidePhysicsAndCrackedWallDestruction() {
        System.out.print("Testing Gravity Slide and Cracked Wall Destruction... ");
        LevelConfig config = new LevelConfig(99, "Test", "Test", 6, 6, true, true, true, 4, false, 0, 0);
        MazeModel model = new MazeModel(config);

        Cell[][] grid = model.getGrid();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                grid[r][c].reset();
            }
        }

        // Open vertical corridor from (0,0) -> (1,0) -> (2,0) with cracked wall below (2,0)
        grid[0][0].removeWall(Direction.DOWN);
        grid[1][0].removeWall(Direction.UP);
        grid[1][0].removeWall(Direction.DOWN);
        grid[2][0].removeWall(Direction.UP);

        // Cracked wall between (2,0) and (3,0)
        grid[2][0].setCracked(Direction.DOWN, true);
        grid[3][0].setCracked(Direction.UP, true);

        // Gravity is DOWN (0 degrees)
        SlideResult result = model.simulateSlide();
        assert result.isWallBroken() : "Wall should have shattered with momentum >= 2!";
        assert result.getDistanceTraversed() >= 2 : "Should traverse at least 2 cells before impact";
        assert !grid[2][0].hasWall(Direction.DOWN) : "Wall at (2,0) DOWN must be removed";
        assert !grid[3][0].hasWall(Direction.UP) : "Wall at (3,0) UP must be removed";

        System.out.println("PASSED");
    }

    private static void testGuaranteedRunwaysInLevel3() {
        System.out.print("Testing Guaranteed Runways in Level 3 Generation... ");
        LevelManager lm = new LevelManager();
        lm.advanceLevel(); // to 2
        lm.advanceLevel(); // to 3
        LevelConfig l3 = lm.getCurrentLevel();
        assert l3.getLevelNumber() == 3;

        MazeModel model = new MazeModel(l3);
        int remainingCracked = model.getRemainingCrackedWallsCount();
        assert remainingCracked > 0 : "Level 3 must have cracked walls, found " + remainingCracked;

        boolean foundRunway = false;
        Cell[][] grid = model.getGrid();
        for (int r = 0; r < model.getRows(); r++) {
            for (int c = 0; c < model.getCols(); c++) {
                if (grid[r][c].isCracked(Direction.DOWN)) {
                    if (r >= 2 && !grid[r][c].hasWall(Direction.UP) && !grid[r-1][c].hasWall(Direction.UP)) {
                        foundRunway = true;
                    }
                    if (r <= model.getRows() - 4 && !grid[r+1][c].hasWall(Direction.DOWN) && !grid[r+2][c].hasWall(Direction.DOWN)) {
                        foundRunway = true;
                    }
                }
                if (grid[r][c].isCracked(Direction.RIGHT)) {
                    if (c >= 2 && !grid[r][c].hasWall(Direction.LEFT) && !grid[r][c-1].hasWall(Direction.LEFT)) {
                        foundRunway = true;
                    }
                    if (c <= model.getCols() - 4 && !grid[r][c+1].hasWall(Direction.RIGHT) && !grid[r][c+2].hasWall(Direction.RIGHT)) {
                        foundRunway = true;
                    }
                }
            }
        }

        assert foundRunway : "Must find at least one guaranteed runway of length >= 2 leading to a cracked wall";
        System.out.println("PASSED (Found " + remainingCracked + " cracked walls with guaranteed runways)");
    }

    private static void testChaserAIAndGravityReaction() {
        System.out.print("Testing Chaser AI Tracking & Environmental Gravity Slide... ");

        // 1. Verify Level 4 has Chaser enabled
        LevelManager lm = new LevelManager();
        lm.advanceLevel(); // 2
        lm.advanceLevel(); // 3
        lm.advanceLevel(); // 4
        LevelConfig l4 = lm.getCurrentLevel();
        assert l4.isChaserEnabled() : "Level 4 must enable Chaser";

        MazeModel model = new MazeModel(l4);
        assert model.hasChaser() : "MazeModel must have active Chaser in Level 4";
        ChaserEntity chaser = model.getChaser();
        assert chaser != null;

        // 2. Test Chaser BFS hunting steps
        int initR = chaser.getRow();
        int initC = chaser.getCol();
        boolean chaserMoved = model.stepChaser();
        assert chaserMoved : "Chaser must find BFS path and step towards player";
        assert chaser.getRow() != initR || chaser.getCol() != initC : "Chaser coordinates must update after step";

        // 3. Test Chaser Environmental Gravity Slide & High-Momentum Stun
        LevelConfig testConfig = new LevelConfig(98, "ChaserGravityTest", "Test", 6, 6, true, true, false, 0, false, 0, 0, true);
        MazeModel customModel = new MazeModel(testConfig);
        Cell[][] grid = customModel.getGrid();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                grid[r][c].reset();
            }
        }
        // Build 4-cell vertical corridor at col 2: (0,2) to (3,2) with bottom wall at (3,2)
        grid[0][2].removeWall(Direction.DOWN);
        grid[1][2].removeWall(Direction.UP);
        grid[1][2].removeWall(Direction.DOWN);
        grid[2][2].removeWall(Direction.UP);
        grid[2][2].removeWall(Direction.DOWN);
        grid[3][2].removeWall(Direction.UP);

        ChaserEntity testChaser = customModel.getChaser();
        testChaser.setPosition(0, 2);

        // Slide chaser downward
        SlideResult slideResult = customModel.simulateChaserSlide();
        assert slideResult != null : "SlideResult must not be null";
        assert slideResult.getDistanceTraversed() == 3 : "Chaser should slide 3 cells down to row 3";
        assert slideResult.hadHighMomentum() : "Slide of 3 cells must accumulate high momentum";
        assert testChaser.isStunned() : "Chaser must be STUNNED after high momentum impact into wall!";

        // 4. Verify stunned chaser cannot take steps towards player
        boolean movedWhileStunned = testChaser.stepTowards(0, 0, grid, 6, 6);
        assert !movedWhileStunned : "Stunned chaser must NOT be able to move";

        System.out.println("PASSED");
    }
}
