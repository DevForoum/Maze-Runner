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
                "Arrow Keys: Navigate | Collect 3 Gems | [HOLD S] Radar Path",
                8, 8,
                false, false,
                false, 0,
                false, 3,
                0,
                false
        ));

        levels.add(new LevelConfig(
                2,
                "Gravitational Shift",
                "Q / E: Rotate | SPACE/G: Drop | Step into linked Portals to Teleport!",
                12, 12,
                true, true,
                false, 0,
                true, 3,
                60,
                false
        ));

        levels.add(new LevelConfig(
                3,
                "Breaker of Walls",
                "Q / E: Rotate | SPACE/G: Momentum Smash! Portals conserve slide velocity!",
                14, 14,
                true, true,
                true, 8,
                true, 3,
                80,
                false
        ));

        levels.add(new LevelConfig(
                4,
                "Shadow Labyrinth",
                "Q / E: Rotate | SPACE/G: Drop | Beware the Chaser! Gravity slides stun it on impact!",
                18, 18,
                true, true,
                true, 14,
                true, 3,
                100,
                true
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
                "Q / E: Rotate | SPACE / G: Gravity Drop | Portals, Gems & Chaser Active",
                size, size,
                true, true,
                true, 12 + (lvl - 4) * 2,
                true, 3,
                Math.max(45, 90 - (lvl - 4) * 5),
                true
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
