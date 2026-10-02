# Maze Runner Rules

## Goal

Reach the exit in the bottom-right corner of each maze. Collect the three gems
along the way to earn a three-star result. Gems improve your score and rating,
but are not required to reach the exit.

The timer counts up and records how long you take. Each level's former time
limit is only a score target: going over it does not end the game or prevent
you from finishing.

## Controls

| Key | Action |
| --- | --- |
| Arrow keys | Move through the maze. Directions follow the screen, including after rotation. |
| Hold `S` | Show the shortest-path radar while the key is held. |
| `R` | Restart the current level. |
| `Q` / `E` | Rotate the maze counterclockwise / clockwise, when rotation is enabled. |
| `Space` / `G` | Slide along gravity, when gravity is enabled. |
| `Enter` / `Space` | Continue to the next level after reaching the exit. |

## Level features

| Level | Maze | Features |
| --- | --- | --- |
| 1 — The Training Grounds | 8 × 8 | Arrow-key movement, radar path, and three gems. |
| 2 — Gravitational Shift | 12 × 12 | Adds maze rotation, gravity slides, and a linked portal pair. Time target: 60 seconds. |
| 3 — Breaker of Walls | 14 × 14 | Adds cracked walls. Build at least two cells of slide momentum to smash one. Portals remain active. Time target: 80 seconds. |
| 4 — Master Labyrinth | 18 × 18 | Combines rotation, gravity slides, cracked walls, portals, and gems. Time target: 100 seconds. |
| 5 onward — Infinite Labyrinth | 20 × 20, increasing up to 26 × 26 | Procedurally generated mazes continue with rotation, gravity, cracked walls, portals, and gems. The time target decreases to a minimum of 45 seconds. |

Portals transport you between their two locations when entered. The gravity
direction changes with maze rotation. Cracked walls can only be broken by a
gravity slide with enough momentum; ordinary movement cannot break them.

## Starting the game

On Windows, run `.\mvnw.cmd exec:java` from the project folder. The Maven
Wrapper downloads and uses Maven if it is not already installed.
