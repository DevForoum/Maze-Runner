package com.mazerunner.model;

import java.awt.Point;
import java.util.*;

/**
 * ChaserEntity — Autonomous AI adversary that hunts the player using BFS pathfinding.
 * Responds to environmental gravity: when the maze gravity drops, the chaser also
 * falls/slides in the gravity direction, and is stunned upon high-velocity impact with walls.
 */
public class ChaserEntity {
    private int row;
    private int col;
    private int stunTicksRemaining = 0;
    private boolean active = true;

    public ChaserEntity(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public boolean isStunned() {
        return stunTicksRemaining > 0;
    }

    public int getStunTicksRemaining() {
        return stunTicksRemaining;
    }

    public void stun(int ticks) {
        this.stunTicksRemaining = Math.max(this.stunTicksRemaining, ticks);
    }

    public void decrementStun() {
        if (stunTicksRemaining > 0) {
            stunTicksRemaining--;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Computes the next single step towards (targetRow, targetCol) using BFS
     * on the current maze grid, respecting all walls.
     * Returns true if a step was taken, false if path is blocked or chaser is stunned.
     */
    public boolean stepTowards(int targetRow, int targetCol, Cell[][] grid, int rows, int cols) {
        if (!active || isStunned()) {
            return false;
        }

        if (row == targetRow && col == targetCol) {
            return false;
        }

        // BFS to find the shortest path from (row, col) to (targetRow, targetCol)
        Point nextCell = findBfsNextStep(row, col, targetRow, targetCol, grid, rows, cols);
        if (nextCell != null) {
            this.row = nextCell.y;
            this.col = nextCell.x;
            return true;
        }
        return false;
    }

    /**
     * Performs a BFS from start to target and returns the immediate next cell to step into.
     */
    private Point findBfsNextStep(int startR, int startC, int targetR, int targetC,
                                  Cell[][] grid, int rows, int cols) {
        Queue<Point> queue = new ArrayDeque<>();
        Point[][] parent = new Point[rows][cols];
        boolean[][] visited = new boolean[rows][cols];

        Point start = new Point(startC, startR); // Point.x = col, Point.y = row
        queue.add(start);
        visited[startR][startC] = true;

        boolean found = false;

        while (!queue.isEmpty()) {
            Point curr = queue.poll();
            if (curr.y == targetR && curr.x == targetC) {
                found = true;
                break;
            }

            Cell currentCell = grid[curr.y][curr.x];
            for (int i = 0; i < 4; i++) {
                if (!currentCell.hasWall(Direction.fromIndex(i))) {
                    Direction dir = Direction.fromIndex(i);
                    int nr = curr.y + dir.getDr();
                    int nc = curr.x + dir.getDc();

                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]) {
                        visited[nr][nc] = true;
                        parent[nr][nc] = curr;
                        queue.add(new Point(nc, nr));
                    }
                }
            }
        }

        if (!found) {
            return null;
        }

        // Backtrack from target to find the first step after start
        Point step = new Point(targetC, targetR);
        Point prev = parent[step.y][step.x];

        while (prev != null && !(prev.x == startC && prev.y == startR)) {
            step = prev;
            prev = parent[step.y][step.x];
        }

        return step;
    }

    /**
     * Simulates gravity sliding for the chaser along the specified gravity direction.
     * Returns a SlideResult containing the trajectory steps and whether the chaser was stunned.
     */
    public SlideResult simulateSlide(Direction dir, Cell[][] grid, int rows, int cols) {
        SlideResult result = new SlideResult(dir);
        int r = this.row;
        int c = this.col;
        result.addStep(r, c);

        while (true) {
            Cell currentCell = grid[r][c];
            if (currentCell.hasWall(dir)) {
                // Terminating impact with wall
                result.setImpact(r, c, dir, false);
                break;
            }

            int nr = r + dir.getDr();
            int nc = c + dir.getDc();
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                result.setImpact(r, c, dir, false);
                break;
            }

            r = nr;
            c = nc;
            result.addStep(r, c);
        }

        // If chaser fell with high momentum (>= 2 cells), it is stunned by the impact!
        if (result.hadHighMomentum()) {
            stun(5); // Stunned for 5 ticks (~3.5 seconds)
        }

        return result;
    }
}
