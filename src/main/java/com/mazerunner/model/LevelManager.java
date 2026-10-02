package com.mazerunner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages game progression through escalating challenge levels.
 */
public class LevelManager {
    private final List<LevelConfig> levels = new ArrayList<>();
    private int currentLevelIndex = 0;

    public LevelManager() {
        initDefaultLevels();
    }

    private void initDefaultLevels() {
        levels.add(new LevelConfig(
                1,
                "The Training Grounds",
                "Arrow Keys: Navigate | S: Shortest Path (BFS)",
                8, 8,
                false, false,
                false, 0,
                0
        ));

        levels.add(new LevelConfig(
                2,
                "Gravitational Shift",
                "Q / E: Rotate Board | SPACE / G: Gravity Drop/Slide! | S: Hold for Path",
                12, 12,
                true, true,
                false, 0,
                60
        ));

        levels.add(new LevelConfig(
                3,
                "Breaker of Walls",
                "Q / E: Rotate | SPACE / G: Slide >= 2 cells along runways to smash walls!",
                14, 14,
                true, true,
                true, 8,
                80
        ));

        levels.add(new LevelConfig(
                4,
                "Master Labyrinth",
                "Q / E: Rotate | SPACE / G: Gravity Drop | S: Hold for Dynamic Path",
                18, 18,
                true, true,
                true, 14,
                100
        ));
    }

    public LevelConfig getCurrentLevel() {
        if (currentLevelIndex < levels.size()) {
            return levels.get(currentLevelIndex);
        }
        // Procedurally generate subsequent levels beyond level 4
        int lvl = currentLevelIndex + 1;
        int size = Math.min(26, 18 + (lvl - 4) * 2);
        return new LevelConfig(
                lvl,
                "Infinite Labyrinth Tier " + (lvl - 3),
                "Q / E: Rotate | SPACE / G: Gravity Drop | S: Shortest Path",
                size, size,
                true, true,
                true, 12 + (lvl - 4) * 2,
                Math.max(45, 90 - (lvl - 4) * 5)
        );
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public int getTotalStaticLevels() {
        return levels.size();
    }

    public boolean advanceLevel() {
        currentLevelIndex++;
        return true;
    }

    public void restartCurrentLevel() {
        // level index stays the same
    }

    public void resetToFirstLevel() {
        currentLevelIndex = 0;
    }
}
