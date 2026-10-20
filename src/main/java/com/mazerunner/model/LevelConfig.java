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
    private final boolean portalsEnabled;
    private final int gemsCount;
    private final int timeLimitSeconds;
    private final boolean chaserEnabled;

    public LevelConfig(int levelNumber, String title, String instructions,
                       int rows, int cols,
                       boolean rotationEnabled, boolean gravityEnabled,
                       boolean crackedWallsEnabled, int crackedWallsCount,
                       boolean portalsEnabled, int gemsCount,
                       int timeLimitSeconds, boolean chaserEnabled) {
        this.levelNumber = levelNumber;
        this.title = title;
        this.instructions = instructions;
        this.rows = rows;
        this.cols = cols;
        this.rotationEnabled = rotationEnabled;
        this.gravityEnabled = gravityEnabled;
        this.crackedWallsEnabled = crackedWallsEnabled;
        this.crackedWallsCount = crackedWallsCount;
        this.portalsEnabled = portalsEnabled;
        this.gemsCount = gemsCount;
        this.timeLimitSeconds = timeLimitSeconds;
        this.chaserEnabled = chaserEnabled;
    }

    public LevelConfig(int levelNumber, String title, String instructions,
                       int rows, int cols,
                       boolean rotationEnabled, boolean gravityEnabled,
                       boolean crackedWallsEnabled, int crackedWallsCount,
                       boolean portalsEnabled, int gemsCount,
                       int timeLimitSeconds) {
        this(levelNumber, title, instructions, rows, cols,
             rotationEnabled, gravityEnabled,
             crackedWallsEnabled, crackedWallsCount,
             portalsEnabled, gemsCount,
             timeLimitSeconds, false);
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
    public boolean isPortalsEnabled() { return portalsEnabled; }
    public int getGemsCount() { return gemsCount; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public boolean isChaserEnabled() { return chaserEnabled; }
}
