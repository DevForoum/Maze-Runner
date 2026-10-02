package com.mazerunner.model;

/**
 * Configuration blueprint for an individual level stage.
 */
public class LevelConfig {
    private final int levelNumber;
    private final String title;
    private final String instructions;
    private final int rows;
    private final int cols;
    private final boolean rotationEnabled;
    private final boolean gravityEnabled;
    private final boolean crackedWallsEnabled;
    private final int crackedWallsCount;
    private final int timeLimitSeconds;

    public LevelConfig(int levelNumber, String title, String instructions,
                       int rows, int cols,
                       boolean rotationEnabled, boolean gravityEnabled,
                       boolean crackedWallsEnabled, int crackedWallsCount,
                       int timeLimitSeconds) {
        this.levelNumber = levelNumber;
        this.title = title;
        this.instructions = instructions;
        this.rows = rows;
        this.cols = cols;
        this.rotationEnabled = rotationEnabled;
        this.gravityEnabled = gravityEnabled;
        this.crackedWallsEnabled = crackedWallsEnabled;
        this.crackedWallsCount = crackedWallsCount;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public int getLevelNumber() { return levelNumber; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public boolean isRotationEnabled() { return rotationEnabled; }
    public boolean isGravityEnabled() { return gravityEnabled; }
    public boolean isCrackedWallsEnabled() { return crackedWallsEnabled; }
    public int getCrackedWallsCount() { return crackedWallsCount; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
}
