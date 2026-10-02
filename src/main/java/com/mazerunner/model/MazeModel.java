package com.mazerunner.model;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

/**
 * Core game state manager handling generation (DFS Backtracking),
 * dynamic shortest path (BFS), maze rotation, gravity sliding physics,
 * and structural wall destruction.
 */
public class MazeModel {
    private LevelConfig levelConfig;
    private int rows;
    private int cols;
    private Cell[][] grid;

    private int playerRow = 0;
    private int playerCol = 0;
    private int rotationAngle = 0; // 0, 90, 180, 270 degrees clockwise
    private boolean showingPath = false;
    private final Random random = new Random();

    // Timer management
    private int timeRemainingSeconds = 0;
    private int elapsedTimeSeconds = 0;
    private boolean timeExpired = false;

    public MazeModel(LevelConfig levelConfig) {
        initLevel(levelConfig);
    }

    public void initLevel(LevelConfig levelConfig) {
        this.levelConfig = levelConfig;
        this.rows = levelConfig.getRows();
        this.cols = levelConfig.getCols();
        this.grid = new Cell[rows][cols];
        this.rotationAngle = 0;
        this.showingPath = false;
        this.timeRemainingSeconds = levelConfig.getTimeLimitSeconds();
        this.elapsedTimeSeconds = 0;
        this.timeExpired = false;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
        generateMaze();
    }

    public void tickTimer() {
        if (timeExpired || isGoalReached()) return;

        if (levelConfig != null && levelConfig.getTimeLimitSeconds() > 0) {
            timeRemainingSeconds--;
            if (timeRemainingSeconds <= 0) {
                timeRemainingSeconds = 0;
                timeExpired = true;
            }
        } else {
            elapsedTimeSeconds++;
        }
    }

    public boolean isTimeExpired() {
        return timeExpired;
    }

    public int getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public int getElapsedTimeSeconds() {
        return elapsedTimeSeconds;
    }

    public String getTimeDisplay() {
        int seconds = (levelConfig != null && levelConfig.getTimeLimitSeconds() > 0)
                ? timeRemainingSeconds
                : elapsedTimeSeconds;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    /**
     * Generates maze using Recursive Backtracking (DFS),
     * resets player to (0,0), and injects cracked walls if configured.
     */
    public void generateMaze() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].reset();
            }
        }

        Stack<Cell> stack = new Stack<>();
        Cell current = grid[0][0];
        current.setVisited(true);
        stack.push(current);

        while (!stack.isEmpty()) {
            current = stack.peek();
            List<Cell> unvisitedNeighbors = getUnvisitedNeighbors(current);

            if (!unvisitedNeighbors.isEmpty()) {
                Collections.shuffle(unvisitedNeighbors);
                Cell next = unvisitedNeighbors.get(0);
                removeWallsBetween(current, next);
                next.setVisited(true);
                stack.push(next);
            } else {
                stack.pop();
            }
        }

        // Reset visited flags after maze generation
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setVisited(false);
            }
        }

        // Inject cracked walls for kinetic puzzle challenges
        if (levelConfig != null && levelConfig.isCrackedWallsEnabled()) {
            injectCrackedWalls(levelConfig.getCrackedWallsCount());
        }

        playerRow = 0;
        playerCol = 0;

        if (showingPath) {
            solveBFS();
        }
    }

    /**
     * Injects cracked structural walls, guaranteeing that each cracked wall
     * has an unobstructed straight runway of at least 2 cells leading directly into it
     * so players can accumulate momentum >= 2.
     */
    private void injectCrackedWalls(int targetCount) {
        List<WallCandidate> candidates = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Check Right wall
                if (c < cols - 1 && grid[r][c].hasWall(Direction.RIGHT)) {
                    if (c >= 2 || c <= cols - 4) {
                        candidates.add(new WallCandidate(r, c, Direction.RIGHT));
                    }
                }
                // Check Down wall
                if (r < rows - 1 && grid[r][c].hasWall(Direction.DOWN)) {
                    if (r >= 2 || r <= rows - 4) {
                        candidates.add(new WallCandidate(r, c, Direction.DOWN));
                    }
                }
            }
        }

        Collections.shuffle(candidates, random);
        int placed = 0;
        for (WallCandidate cand : candidates) {
            if (placed >= targetCount) break;

            int r = cand.r;
            int c = cand.c;
            Direction wallDir = cand.dir;
            boolean runwayCarved = false;

            if (wallDir == Direction.DOWN) {
                // Horizontal wall between (r, c) and (r+1, c)
                if (r >= 2) {
                    // Approach from above (moving DOWN: r-2 -> r-1 -> r)
                    openPassage(r - 2, c, r - 1, c);
                    openPassage(r - 1, c, r, c);
                    runwayCarved = true;
                } else if (r <= rows - 4) {
                    // Approach from below (moving UP: r+3 -> r+2 -> r+1)
                    openPassage(r + 3, c, r + 2, c);
                    openPassage(r + 2, c, r + 1, c);
                    runwayCarved = true;
                }
            } else if (wallDir == Direction.RIGHT) {
                // Vertical wall between (r, c) and (r, c+1)
                if (c >= 2) {
                    // Approach from left (moving RIGHT: c-2 -> c-1 -> c)
                    openPassage(r, c - 2, r, c - 1);
                    openPassage(r, c - 1, r, c);
                    runwayCarved = true;
                } else if (c <= cols - 4) {
                    // Approach from right (moving LEFT: c+3 -> c+2 -> c+1)
                    openPassage(r, c + 3, r, c + 2);
                    openPassage(r, c + 2, r, c + 1);
                    runwayCarved = true;
                }
            }

            if (runwayCarved) {
                grid[r][c].setCracked(wallDir, true);
                int nr = r + wallDir.getDr();
                int nc = c + wallDir.getDc();
                if (isValidCoord(nr, nc)) {
                    grid[nr][nc].setCracked(wallDir.opposite(), true);
                }
                placed++;
            }
        }
    }

    private void openPassage(int r1, int c1, int r2, int c2) {
        if (!isValidCoord(r1, c1) || !isValidCoord(r2, c2)) return;
        int dr = r2 - r1;
        int dc = c2 - c1;
        for (Direction d : Direction.values()) {
            if (d.getDr() == dr && d.getDc() == dc) {
                grid[r1][c1].removeWall(d);
                grid[r2][c2].removeWall(d.opposite());
                break;
            }
        }
    }

    public int getRemainingCrackedWallsCount() {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isCracked(Direction.RIGHT)) count++;
                if (grid[r][c].isCracked(Direction.DOWN)) count++;
            }
        }
        return count;
    }

    private record WallCandidate(int r, int c, Direction dir) {}

    /**
     * Solves shortest path from current player position to goal using BFS.
     */
    public void solveBFS() {
        // Reset path states
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setPath(false);
            }
        }

        Queue<Cell> queue = new ArrayDeque<>();
        boolean[][] visited = new boolean[rows][cols];
        Map<Cell, Cell> parentMap = new HashMap<>();

        Cell start = grid[playerRow][playerCol];
        Cell target = grid[rows - 1][cols - 1];

        queue.add(start);
        visited[start.getRow()][start.getCol()] = true;

        while (!queue.isEmpty()) {
            Cell curr = queue.poll();
            if (curr == target) break;

            int r = curr.getRow();
            int c = curr.getCol();

            for (Direction dir : Direction.values()) {
                if (!curr.hasWall(dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (isValidCoord(nr, nc) && !visited[nr][nc]) {
                        Cell neighbor = grid[nr][nc];
                        visited[nr][nc] = true;
                        parentMap.put(neighbor, curr);
                        queue.add(neighbor);
                    }
                }
            }
        }

        // Trace back shortest path
        Cell step = target;
        while (step != null && parentMap.containsKey(step)) {
            step.setPath(true);
            step = parentMap.get(step);
        }
        showingPath = true;
    }

    public void clearPath() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setPath(false);
            }
        }
        showingPath = false;
    }

    public void togglePath() {
        if (showingPath) {
            clearPath();
        } else {
            solveBFS();
        }
    }

    /**
     * Maps screen-relative arrow key direction to grid coordinates based on maze rotation.
     */
    public Direction mapScreenToGrid(Direction screenDir) {
        int stepsCCW = (rotationAngle / 90) % 4;
        Direction current = screenDir;
        for (int i = 0; i < stepsCCW; i++) {
            current = current.rotateCounterClockwise();
        }
        return current;
    }

    /**
     * Returns the grid-relative direction along which gravity pulls.
     * When unrotated (0°), visual down is grid DOWN.
     * With 90° clockwise canvas rotation, visual down aligns with grid RIGHT.
     */
    public Direction getGravityDirection() {
        return switch (rotationAngle) {
            case 0 -> Direction.DOWN;
            case 90 -> Direction.RIGHT;
            case 180 -> Direction.UP;
            case 270 -> Direction.LEFT;
            default -> Direction.DOWN;
        };
    }

    /**
     * Rotates maze by 90° clockwise.
     */
    public void rotateClockwise() {
        rotationAngle = (rotationAngle + 90) % 360;
    }

    /**
     * Rotates maze by 90° counter-clockwise.
     */
    public void rotateCounterClockwise() {
        rotationAngle = (rotationAngle + 270) % 360;
    }

    /**
     * Simulates gravity slide along the current gravity direction.
     * Evaluates traversal distance, momentum, and checks for cracked wall destruction.
     */
    public SlideResult simulateSlide() {
        Direction dir = getGravityDirection();
        SlideResult result = new SlideResult(dir);

        int r = playerRow;
        int c = playerCol;
        result.addStep(r, c);

        while (true) {
            Cell currentCell = grid[r][c];
            // Stop if there is a wall in the sliding direction
            if (currentCell.hasWall(dir)) {
                boolean isCracked = currentCell.isCracked(dir);
                int dist = result.getDistanceTraversed();
                int nr = r + dir.getDr();
                int nc = c + dir.getDc();

                // Momentum threshold: distance >= 2 to break cracked walls
                if (dist >= 2 && isCracked && isValidCoord(nr, nc)) {
                    // Shatter wall!
                    breakWall(r, c, dir);
                    result.setImpact(r, c, dir, true);
                    // Move one step forward into the newly opened cell
                    r = nr;
                    c = nc;
                    result.addStep(r, c);
                } else {
                    // Impact solid or non-breakable wall
                    result.setImpact(r, c, dir, false);
                }
                break;
            }

            int nr = r + dir.getDr();
            int nc = c + dir.getDc();
            if (!isValidCoord(nr, nc)) {
                result.setImpact(r, c, dir, false);
                break;
            }

            r = nr;
            c = nc;
            result.addStep(r, c);
        }

        return result;
    }

    /**
     * Shatters and removes a wall between (r, c) and its neighbor in direction dir.
     */
    public void breakWall(int r, int c, Direction dir) {
        if (!isValidCoord(r, c)) return;
        grid[r][c].breakWall(dir);

        int nr = r + dir.getDr();
        int nc = c + dir.getDc();
        if (isValidCoord(nr, nc)) {
            grid[nr][nc].breakWall(dir.opposite());
        }

        // Recalculate path if active
        if (showingPath) {
            solveBFS();
        }
    }

    /**
     * Standard discrete player movement in direction dir.
     */
    public boolean movePlayer(Direction dir) {
        if (grid[playerRow][playerCol].hasWall(dir)) {
            return false;
        }

        int targetRow = playerRow + dir.getDr();
        int targetCol = playerCol + dir.getDc();

        if (!isValidCoord(targetRow, targetCol)) {
            return false;
        }

        playerRow = targetRow;
        playerCol = targetCol;

        if (showingPath) {
            solveBFS();
        }
        return true;
    }

    public void setPlayerPosition(int row, int col) {
        if (isValidCoord(row, col)) {
            this.playerRow = row;
            this.playerCol = col;
            if (showingPath) {
                solveBFS();
            }
        }
    }

    public boolean isGoalReached() {
        return playerRow == rows - 1 && playerCol == cols - 1;
    }

    public boolean isValidCoord(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    private List<Cell> getUnvisitedNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();
        int r = cell.getRow();
        int c = cell.getCol();

        for (Direction dir : Direction.values()) {
            int nr = r + dir.getDr();
            int nc = c + dir.getDc();
            if (isValidCoord(nr, nc) && !grid[nr][nc].isVisited()) {
                neighbors.add(grid[nr][nc]);
            }
        }
        return neighbors;
    }

    private void removeWallsBetween(Cell current, Cell next) {
        int rDiff = next.getRow() - current.getRow();
        int cDiff = next.getCol() - current.getCol();

        for (Direction dir : Direction.values()) {
            if (dir.getDr() == rDiff && dir.getDc() == cDiff) {
                current.removeWall(dir);
                next.removeWall(dir.opposite());
                break;
            }
        }
    }

    // Getters
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Cell[][] getGrid() { return grid; }
    public int getPlayerRow() { return playerRow; }
    public int getPlayerCol() { return playerCol; }
    public int getRotationAngle() { return rotationAngle; }
    public LevelConfig getLevelConfig() { return levelConfig; }
    public boolean isShowingPath() { return showingPath; }
}