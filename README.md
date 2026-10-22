# Maze Runner: Kinetic Gravity Engine

A feature-rich maze game built in **Java (Swing)** as a college project.
Navigate procedurally generated mazes across 4 escalating levels, each introducing new mechanics.

---

## How to Run

### Terminal (Windows)
```bash
# 1. Compile
powershell -ExecutionPolicy Bypass -File build.ps1

# 2. Launch
java -cp target/classes com.mazerunner.Main
```

### IDE (IntelliJ / VSCode / Eclipse)
Open the project folder, set source root to `src/main/java`, and run `com.mazerunner.Main`.

---

## Controls

| Key | Action |
|-----|--------|
| `Arrow Keys` | Move the player |
| `Q` / `E` | Rotate maze 90° counter-clockwise / clockwise |
| `Space` / `G` | Trigger gravity slide |
| `Hold S` | Show BFS radar path to the goal |
| `R` | Restart current level |
| `M` | Mute / unmute sounds |

---

## Levels

| # | Name | Size | New Mechanic |
|---|------|------|---|
| 1 | The Training Grounds | 8×8 | Basic navigation + gems |
| 2 | Gravitational Shift | 12×12 | Rotation, gravity drop, portals |
| 3 | Breaker of Walls | 14×14 | Momentum physics + cracked walls |
| 4 | Shadow Labyrinth | 18×18 | Chaser AI adversary |

After Level 4 the game generates **infinite procedural levels** that grow larger each time.

---

## Features

- **Procedural Maze Generation** — DFS Backtracking; every run is unique
- **BFS Shortest Path** — hold `S` to reveal the optimal route in real time
- **Maze Rotation** — smooth 90° animated rotations that remap gravity
- **Gravity Physics** — player slides until hitting a wall; momentum builds over distance
- **Cracked Walls** — walls shatter when hit with momentum ≥ 2
- **Portals** — teleport between two linked cells; BFS path is portal-aware
- **Energy Gems** — collectibles that affect your end-of-level score
- **Chaser AI** — BFS-hunting adversary that also reacts to gravity slides
- **Scorecard / Grade** — time, gems, and moves determine S/A/B/C grade
- **Animated Splash Screen** — cinematic intro with custom artwork
- **Level Selector** — choose any level directly from the menu
- **Screen Shake + Debris Particles** — visual feedback on wall impacts
- **Sound Effects** — move, portal whoosh, wall smash, win

---

## Project Structure

```
src/
├── main/java/com/mazerunner/
│   ├── Main.java                  ← Entry point, screen flow
│   ├── controller/
│   │   └── GameController.java   ← Key input, game loop, level transitions
│   ├── model/
│   │   ├── MazeModel.java        ← Core game state (generation, BFS, physics)
│   │   ├── Cell.java             ← Single maze cell (walls, gems, portals)
│   │   ├── Direction.java        ← UP/DOWN/LEFT/RIGHT enum with vector helpers
│   │   ├── ChaserEntity.java     ← Chaser AI state (position, stun, BFS step)
│   │   ├── LevelConfig.java      ← Per-level settings (size, features, timer)
│   │   ├── LevelManager.java     ← Level progression + procedural generation
│   │   ├── LevelStats.java       ← Moves, gems, time → grade calculation
│   │   ├── SlideResult.java      ← Result of a gravity slide simulation
│   │   └── SoundManager.java     ← Sound playback with mute toggle
│   └── view/
│       ├── MazePanel.java        ← Game renderer (cells, HUD, particles, chaser)
│       ├── MenuPanel.java        ← Animated main menu with splash screen
│       └── LevelSelectPanel.java ← Level chooser screen
└── test/java/com/mazerunner/
    └── EngineTest.java           ← 8 unit tests (no JUnit dependency)
```

---

## Architecture — MVC Pattern

```
┌─────────────┐   key events   ┌──────────────────┐   state queries   ┌────────────┐
│  MazePanel  │ ─────────────► │  GameController  │ ◄──────────────── │ MazeModel  │
│   (View)    │ ◄─────────────  │  (Controller)    │ ──────────────►   │  (Model)   │
└─────────────┘    repaint()   └──────────────────┘   movePlayer() …  └────────────┘
```

---

## Running the Tests

```bash
javac -cp target/classes -d target/classes src/test/java/com/mazerunner/EngineTest.java
java  -cp target/classes com.mazerunner.EngineTest
# Expected: === ALL ENGINE TESTS PASSED SUCCESSFULLY! ===
```
