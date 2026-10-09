package com.mazerunner;

import com.mazerunner.model.Cell;
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

        model.rotateCounterClockwise();
        assert model.getRotationAngle() == 180;
        System.out.println("PASSED");
    }

    private static void testElapsedTimerBeyondScoreTarget() {
        System.out.print("Testing Elapsed Timer Beyond Score Target... ");
        LevelConfig config = new LevelConfig(99, "Timer Test", "", 5, 5,
                false, false, false, 0, false, 0, 1);
        MazeModel model = new MazeModel(config);

        assert model.getTimeDisplay().equals("00:00") : "Timer should start at 00:00";
        model.tickTimer();
        assert model.getElapsedTimeSeconds() == 1 : "Elapsed time should increment";
        assert model.getTimeDisplay().equals("00:01") : "Timer should count upward";

        model.tickTimer();
        assert model.getElapsedTimeSeconds() == 2 : "Timer must keep running past the score target";
        assert model.getTimeDisplay().equals("00:02") : "Display should show elapsed time past the target";

        model.setPlayerPosition(4, 4);
        assert model.isGoalReached() : "The test level should be complete";
        model.tickTimer();
        assert model.getElapsedTimeSeconds() == 2 : "Timer should stop when the level is complete";
        assert model.getCurrentStats().getTimeTakenSeconds() == 2 : "Completion stats should record elapsed time";
        System.out.println("PASSED");
    }

    private static void testPathfindingEveryLevel() {
        System.out.print("Testing BFS Pathfinding Across All Levels... ");
        LevelManager lm = new LevelManager();
        for (int i = 0; i < 4; i++) {
            MazeModel model = new MazeModel(lm.getCurrentLevel());
            model.solveBFS();
            assert model.isShowingPath() : "BFS path flag not set";

            // Verify target cell is marked as path
            Cell target = model.getGrid()[model.getRows() - 1][model.getCols() - 1];
            assert target.isPath() : "Target cell not flagged in BFS solution path for level " + (i + 1);

            lm.advanceLevel();
        }
        System.out.println("PASSED");
    }

    private static void testPortalPathfindingAndReachability() {
        System.out.print("Testing Portal-Aware Paths and Level 2 Reachability... ");

        LevelConfig config = new LevelConfig(99, "Portal Test", "", 5, 5,
                false, false, false, 0, true, 0, 0);
        MazeModel trappedModel = new MazeModel(config);
        Cell[][] trappedGrid = trappedModel.getGrid();
        resetGridToWalls(trappedGrid);
        openPassage(trappedGrid, 0, 0, Direction.RIGHT);
        openPassage(trappedGrid, 0, 1, Direction.RIGHT);
        trappedGrid[0][1].setPortal(1, new Point(4, 3));
        trappedGrid[4][3].setPortal(2, new Point(0, 1));
        trappedModel.solveBFS();

        assert !canReachExit(trappedModel) : "Entering the portal should strand the player";
        assert !trappedGrid[4][4].isPath() : "BFS must not show a route through a portal that cannot be exited";

        MazeModel validModel = new MazeModel(config);
        Cell[][] validGrid = validModel.getGrid();
        resetGridToWalls(validGrid);
        openPassage(validGrid, 0, 0, Direction.DOWN);
        openPassage(validGrid, 3, 3, Direction.DOWN);
        openPassage(validGrid, 4, 3, Direction.RIGHT);
        validGrid[1][0].setPortal(1, new Point(3, 3));
        validGrid[3][3].setPortal(2, new Point(1, 0));
        validModel.solveBFS();

        assert canReachExit(validModel) : "A linked portal route to the exit should be traversable";
        assert validGrid[4][4].isPath() : "BFS should flag a reachable goal";
        assert validGrid[1][0].isPath() && validGrid[3][3].isPath()
                : "BFS should show both portal entry and exit cells";

        LevelManager levelManager = new LevelManager();
        levelManager.advanceLevel();
        LevelConfig level2 = levelManager.getCurrentLevel();
        for (int i = 0; i < 250; i++) {
            MazeModel generated = new MazeModel(level2);
            assert canReachExit(generated) : "Generated level 2 maze " + i + " has no portal-aware exit path";
            generated.solveBFS();
            assert generated.getGrid()[generated.getRows() - 1][generated.getCols() - 1].isPath()
                    : "BFS missed the exit in generated level 2 maze " + i;
        }
        System.out.println("PASSED (250 generated level 2 mazes)");
    }

    private static void resetGridToWalls(Cell[][] grid) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                cell.reset();
            }
        }
    }

    private static void openPassage(Cell[][] grid, int row, int col, Direction direction) {
        int nextRow = row + direction.getDr();
        int nextCol = col + direction.getDc();
        grid[row][col].removeWall(direction);
        grid[nextRow][nextCol].removeWall(direction.opposite());
    }

    private static boolean canReachExit(MazeModel model) {
        Cell[][] grid = model.getGrid();
        boolean[][] visited = new boolean[model.getRows()][model.getCols()];
        Queue<Cell> queue = new ArrayDeque<>();
        Cell start = grid[model.getPlayerRow()][model.getPlayerCol()];
        queue.add(start);
        visited[start.getRow()][start.getCol()] = true;

        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            if (current.getRow() == model.getRows() - 1 && current.getCol() == model.getCols() - 1) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                if (current.hasWall(direction)) {
                    continue;
                }
                int row = current.getRow() + direction.getDr();
                int col = current.getCol() + direction.getDc();
                if (!model.isValidCoord(row, col)) {
                    continue;
                }
                Cell destination = grid[row][col];
                if (destination.isPortal()) {
                    Point target = destination.getPortalTarget();
                    if (target != null && model.isValidCoord(target.y, target.x)) {
                        destination = grid[target.y][target.x];
                    }
                }
                if (!visited[destination.getRow()][destination.getCol()]) {
                    visited[destination.getRow()][destination.getCol()] = true;
                    queue.add(destination);
                }
            }
        }
        return false;
    }

    private static void testSlidePhysicsAndCrackedWallDestruction() {
        System.out.print("Testing Gravity Slide and Cracked Wall Destruction... ");
        LevelConfig customConfig = new LevelConfig(99, "Test", "", 5, 5, true, true, true, 0, false, 0, 0);
        MazeModel model = new MazeModel(customConfig);

        // Manually create an open corridor from (0,0) down to (2,0)
        // Cell (0,0) -> remove DOWN wall
        model.getGrid()[0][0].removeWall(Direction.DOWN);
        model.getGrid()[1][0].removeWall(Direction.UP);
        // Cell (1,0) -> remove DOWN wall
        model.getGrid()[1][0].removeWall(Direction.DOWN);
        model.getGrid()[2][0].removeWall(Direction.UP);

        // Set cracked wall between (2,0) and (3,0)
        model.getGrid()[2][0].setCracked(Direction.DOWN, true);
        model.getGrid()[3][0].setCracked(Direction.UP, true);

        // Position player at (0,0)
        model.setPlayerPosition(0, 0);
        // Unrotated -> gravity pulls DOWN
        assert model.getGravityDirection() == Direction.DOWN;

        SlideResult result = model.simulateSlide();

        // Traversal from row 0 to row 2 is 2 steps -> distance = 2 (meets >= 2 threshold)
        assert result.getDistanceTraversed() >= 2 : "Distance should be >= 2, was " + result.getDistanceTraversed();
        assert result.hadHighMomentum() : "Expected high momentum";
        assert result.isWallBroken() : "Cracked wall should have been broken by momentum >= 2";

        // Verify wall was physically removed on both sides
        assert !model.getGrid()[2][0].hasWall(Direction.DOWN) : "Cell (2,0) DOWN wall should be broken";
        assert !model.getGrid()[3][0].hasWall(Direction.UP) : "Cell (3,0) UP wall should be broken";

        // Test: Manual 1-step move into a cracked wall must NOT break it (momentum < 2)
        model.getGrid()[3][0].setCracked(Direction.DOWN, true);
        model.getGrid()[4][0].setCracked(Direction.UP, true);
        model.setPlayerPosition(3, 0);

        boolean moved = model.movePlayer(Direction.DOWN);
        assert !moved : "Player should be blocked by unbroken cracked wall";
        assert model.getGrid()[3][0].hasWall(Direction.DOWN) : "Cracked wall must not break without momentum >= 2";

        // Test: Dynamic BFS path updates through the broken passage
        model.solveBFS();
        assert model.isShowingPath() : "BFS path should be computed";

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

        // Search for at least one cracked wall and verify an open straight runway of length >= 2 exists
        boolean foundRunway = false;
        Cell[][] grid = model.getGrid();
        for (int r = 0; r < model.getRows(); r++) {
            for (int c = 0; c < model.getCols(); c++) {
                if (grid[r][c].isCracked(Direction.DOWN)) {
                    // Check approach from above (r-1, r-2)
                    if (r >= 2 && !grid[r][c].hasWall(Direction.UP) && !grid[r-1][c].hasWall(Direction.UP)) {
                        foundRunway = true;
                    }
                    // Or approach from below (r+1, r+2)
                    if (r <= model.getRows() - 4 && !grid[r+1][c].hasWall(Direction.DOWN) && !grid[r+2][c].hasWall(Direction.DOWN)) {
                        foundRunway = true;
                    }
                }
                if (grid[r][c].isCracked(Direction.RIGHT)) {
                    // Check approach from left (c-1, c-2)
                    if (c >= 2 && !grid[r][c].hasWall(Direction.LEFT) && !grid[r][c-1].hasWall(Direction.LEFT)) {
                        foundRunway = true;
                    }
                    // Or approach from right (c+1, c+2)
                    if (c <= model.getCols() - 4 && !grid[r][c+1].hasWall(Direction.RIGHT) && !grid[r][c+2].hasWall(Direction.RIGHT)) {
                        foundRunway = true;
                    }
                }
            }
        }

        assert foundRunway : "Must find at least one guaranteed runway of length >= 2 leading to a cracked wall";
        System.out.println("PASSED (Found " + remainingCracked + " cracked walls with guaranteed runways)");
    }
}
