package com.mazerunner.model;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
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
    private int elapsedTimeSeconds = 0;

    // Performance & Scorecard stats
    private LevelStats currentStats = new LevelStats();

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
        this.elapsedTimeSeconds = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
        generateMaze();
    }

    public void tickTimer() {
        if (!isGoalReached()) {
            elapsedTimeSeconds++;
        }
    }

    public int getElapsedTimeSeconds() {
        return elapsedTimeSeconds;
    }

    public String getTimeDisplay() {
        int m = elapsedTimeSeconds / 60;
        int s = elapsedTimeSeconds % 60;
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

        // Inject energy gems
        if (levelConfig != null && levelConfig.getGemsCount() > 0) {
            injectGems(levelConfig.getGemsCount());
        }

        // Inject linked portal pair
        if (levelConfig != null && levelConfig.isPortalsEnabled()) {
            injectPortals();
        }

        this.currentStats = new LevelStats();

        playerRow = 0;
        playerCol = 0;

        if (showingPath) {
            solveBFS();
        }
    }

    private void injectGems(int targetCount) {
        List<Point> candidates = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 && c == 0) continue;
                if (r == rows - 1 && c == cols - 1) continue;

                int wallCount = 0;
                for (Direction d : Direction.values()) {
                    if (grid[r][c].hasWall(d)) wallCount++;
                }
                if (wallCount >= 2) {
                    candidates.add(new Point(c, r));
                }
            }
        }
        Collections.shuffle(candidates, random);
        int placed = 0;
        for (Point p : candidates) {
            if (placed >= targetCount) break;
            grid[p.y][p.x].setGem(true);
            placed++;
        }
    }

    private void injectPortals() {
        List<Point> openCells = new ArrayList<>();
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (r == 0 && c == 0) continue;
                if (r == rows - 1 && c == cols - 1) continue;
                if (grid[r][c].hasGem()) continue;
                openCells.add(new Point(c, r));
            }
        }
        Collections.shuffle(openCells, random);
        Point portalA = null;
        Point portalB = null;
        for (int i = 0; i < openCells.size(); i++) {
            Point p1 = openCells.get(i);
            for (int j = i + 1; j < openCells.size(); j++) {
                Point p2 = openCells.get(j);
                if (p1.distance(p2) >= 4.0) {
                    portalA = p1;
                    portalB = p2;
                    break;
                }
            }
            if (portalA != null) break;
        }

        if (portalA != null && portalB != null) {
            grid[portalA.y][portalA.x].setPortal(1, portalB);
            grid[portalB.y][portalB.x].setPortal(2, portalA);
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
     * Solves shortest path from current player position to goal using BFS,
     * treating linked portals as zero-cost transitions between their two cells.
     */
    public void solveBFS() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setPath(false);
            }
        }

        Queue<Cell> queue = new ArrayDeque<>();
        Set<Cell> visited = new HashSet<>();
        Map<Cell, Cell> parentMap = new HashMap<>();
        Map<Cell, Cell> portalEntryMap = new HashMap<>();

        Cell start = grid[playerRow][playerCol];
        Cell target = grid[rows - 1][cols - 1];

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Cell curr = queue.poll();
            if (curr == target) break;

            int r = curr.getRow();
            int c = curr.getCol();

            for (Direction dir : Direction.values()) {
                if (!curr.hasWall(dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (isValidCoord(nr, nc)) {
                        Cell neighbor = grid[nr][nc];
                        Cell destination = neighbor;
                        Cell portalEntry = null;
                        if (neighbor.isPortal()) {
                            Point portalTarget = neighbor.getPortalTarget();
                            if (portalTarget != null && isValidCoord(portalTarget.y, portalTarget.x)) {
                                destination = grid[portalTarget.y][portalTarget.x];
                                portalEntry = neighbor;
                            }
                        }
                        if (visited.add(destination)) {
                            parentMap.put(destination, curr);
                            if (portalEntry != null) {
                                portalEntryMap.put(destination, portalEntry);
                            }
                            queue.add(destination);
                        }
                    }
                }
            }
        }

        Cell step = target;
        while (step != null && parentMap.containsKey(step)) {
            step.setPath(true);
            Cell portalEntry = portalEntryMap.get(step);
            if (portalEntry != null) {
                portalEntry.setPath(true);
            }
            step = parentMap.get(step);
        }
        if (target == start) {
            target.setPath(true);
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
        currentStats.recordRotation();
        rotationAngle = (rotationAngle + 90) % 360;
    }

    public void rotateCounterClockwise() {
        currentStats.recordRotation();
        rotationAngle = (rotationAngle + 270) % 360;
    }

    /**
     * Simulates gravity slide along the current gravity direction.
     * Evaluates traversal distance, momentum, gem collections, and portal transits.
     */
    public SlideResult simulateSlide() {
        Direction dir = getGravityDirection();
        SlideResult result = new SlideResult(dir);
        java.util.Set<Point> usedPortals = new java.util.HashSet<>();

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

                    if (grid[r][c].hasGem()) {
                        grid[r][c].setGem(false);
                        currentStats.recordGemCollected();
                    }
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

            // Collect gem during slide
            if (grid[r][c].hasGem()) {
                grid[r][c].setGem(false);
                currentStats.recordGemCollected();
            }

            // Portal transit: preserve momentum and exit at linked portal
            if (grid[r][c].isPortal()) {
                Point portalPoint = new Point(c, r);
                if (!usedPortals.contains(portalPoint)) {
                    usedPortals.add(portalPoint);
                    Point target = grid[r][c].getPortalTarget();
                    if (target != null && isValidCoord(target.y, target.x)) {
                        usedPortals.add(target);
                        r = target.y;
                        c = target.x;
                        result.addStep(r, c);

                        if (grid[r][c].hasGem()) {
                            grid[r][c].setGem(false);
                            currentStats.recordGemCollected();
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Shatters and removes a wall between (r, c) and its neighbor in direction dir.
     */
    public void breakWall(int r, int c, Direction dir) {
        if (!isValidCoord(r, c)) return;
        currentStats.recordWallSmashed();
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

        currentStats.recordMove();
        playerRow = targetRow;
        playerCol = targetCol;

        // Collect gem
        if (grid[playerRow][playerCol].hasGem()) {
            grid[playerRow][playerCol].setGem(false);
            currentStats.recordGemCollected();
        }

        // Teleport if portal
        if (grid[playerRow][playerCol].isPortal()) {
            Point target = grid[playerRow][playerCol].getPortalTarget();
            if (target != null && isValidCoord(target.y, target.x)) {
                playerRow = target.y;
                playerCol = target.x;
                if (grid[playerRow][playerCol].hasGem()) {
                    grid[playerRow][playerCol].setGem(false);
                    currentStats.recordGemCollected();
                }
            }
        }

        if (showingPath) {
            solveBFS();
        }
        return true;
    }

    public void setPlayerPosition(int row, int col) {
        if (isValidCoord(row, col)) {
            this.playerRow = row;
            this.playerCol = col;
            if (grid[playerRow][playerCol].hasGem()) {
                grid[playerRow][playerCol].setGem(false);
                currentStats.recordGemCollected();
            }
            if (showingPath) {
                solveBFS();
            }
        }
    }

    public boolean isGoalReached() {
        boolean reached = playerRow == rows - 1 && playerCol == cols - 1;
        if (reached) {
            int scoreTargetTimeRemaining = Math.max(0, levelConfig.getTimeLimitSeconds() - elapsedTimeSeconds);
            currentStats.finalizeStats(elapsedTimeSeconds, scoreTargetTimeRemaining,
                    (levelConfig != null) ? levelConfig.getTimeLimitSeconds() : 0,
                    (levelConfig != null) ? levelConfig.getGemsCount() : 3);
        }
        return reached;
    }

    public LevelStats getCurrentStats() {
        return currentStats;
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