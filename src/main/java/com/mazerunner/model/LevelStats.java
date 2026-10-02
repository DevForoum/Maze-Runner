package com.mazerunner.model;

/**
 * Encapsulates performance metrics, scorecard grading, and star rating for a completed stage.
 */
public class LevelStats {
    private int movesCount = 0;
    private int rotationsCount = 0;
    private int wallsSmashedCount = 0;
    private int gemsCollected = 0;
    private int totalGems = 3;
    private int timeTakenSeconds = 0;
    private int timeRemainingSeconds = 0;
    private int timeLimitSeconds = 0;

    public void recordMove() { movesCount++; }
    public void recordRotation() { rotationsCount++; }
    public void recordWallSmashed() { wallsSmashedCount++; }
    public void recordGemCollected() { gemsCollected++; }

    public void finalizeStats(int timeTakenSeconds, int timeRemainingSeconds, int timeLimitSeconds, int totalGems) {
        this.timeTakenSeconds = timeTakenSeconds;
        this.timeRemainingSeconds = timeRemainingSeconds;
        this.timeLimitSeconds = timeLimitSeconds;
        this.totalGems = totalGems;
    }

    public int getMovesCount() { return movesCount; }
    public int getRotationsCount() { return rotationsCount; }
    public int getWallsSmashedCount() { return wallsSmashedCount; }
    public int getGemsCollected() { return gemsCollected; }
    public int getTotalGems() { return totalGems; }
    public int getTimeTakenSeconds() { return timeTakenSeconds; }

    public int getStarsEarned() {
        return Math.min(3, Math.max(0, gemsCollected));
    }

    public int calculateTotalScore() {
        int base = 1000;
        int gemPoints = gemsCollected * 500;
        int smashPoints = wallsSmashedCount * 300;
        int timePoints = timeRemainingSeconds * 25;
        int movePenalty = Math.min(600, movesCount * 4);
        return Math.max(100, base + gemPoints + smashPoints + timePoints - movePenalty);
    }

    public String calculateGrade() {
        if (gemsCollected == totalGems && (timeLimitSeconds == 0 || timeRemainingSeconds >= timeLimitSeconds * 0.35)) {
            return "S";
        } else if (gemsCollected >= 2) {
            return "A";
        } else if (gemsCollected == 1) {
            return "B";
        } else {
            return "C";
        }
    }
}
