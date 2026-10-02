package com.mazerunner.model;

/**
 * Standard 4-way direction enum mapped to grid row/column deltas
 * and cell wall array indices (0: UP, 1: RIGHT, 2: DOWN, 3: LEFT).
 */
public enum Direction {
    UP(-1, 0, 0),
    RIGHT(0, 1, 1),
    DOWN(1, 0, 2),
    LEFT(0, -1, 3);

    private final int dr;
    private final int dc;
    private final int wallIndex;

    Direction(int dr, int dc, int wallIndex) {
        this.dr = dr;
        this.dc = dc;
        this.wallIndex = wallIndex;
    }

    public int getDr() {
        return dr;
    }

    public int getDc() {
        return dc;
    }

    public int getWallIndex() {
        return wallIndex;
    }

    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case RIGHT -> LEFT;
            case DOWN -> UP;
            case LEFT -> RIGHT;
        };
    }

    public Direction rotateClockwise() {
        return switch (this) {
            case UP -> RIGHT;
            case RIGHT -> DOWN;
            case DOWN -> LEFT;
            case LEFT -> UP;
        };
    }

    public Direction rotateCounterClockwise() {
        return switch (this) {
            case UP -> LEFT;
            case LEFT -> DOWN;
            case DOWN -> RIGHT;
            case RIGHT -> UP;
        };
    }

    public static Direction fromIndex(int index) {
        return switch (index) {
            case 0 -> UP;
            case 1 -> RIGHT;
            case 2 -> DOWN;
            case 3 -> LEFT;
            default -> throw new IllegalArgumentException("Invalid wall index: " + index);
        };
    }
}
