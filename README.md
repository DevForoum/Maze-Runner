# 🧩 Maze Runner: Kinetic Gravity Engine v2.0

Maze Runner is an advanced 2D desktop game and algorithm visualization tool built in Java[cite: 1]. Moving beyond traditional static mazes, this project combines procedural maze generation[cite: 1], graph traversal algorithms[cite: 1], and real-time physics mechanics (such as environmental rotation and gravitational sliding) into a clean Model-View-Controller (MVC) architecture.

## 🚀 Key Features & Mechanics
- **Dynamic Maze Generation:** Procedurally builds fresh mazes every session using the **Recursive Backtracking (DFS)** algorithm[cite: 1].
- **Kinetic Maze Rotation:** Rotate the entire board dynamically using `Q` and `E` keys, with perspective-aware input mapping.
- **Environmental Gravity & Momentum:** Shift gravity vectors to slide the player across open corridors, building momentum to smash cracked structural walls[cite: 1].
- **Algorithm Visualization:** Integrated **Breadth-First Search (BFS)** computes and highlights the optimal shortest path on demand[cite: 1].
- **Polished Swing Rendering:** Double-buffered `Graphics2D` rendering featuring particle debris effects, screen shake feedback, and momentum visual trails.

## 🛠️ Tech Stack & Architecture
- **Language:** Java 17+[cite: 1]
- **GUI Framework:** Java Swing (`JFrame`, custom `JPanel`, `Graphics2D`)[cite: 1]
- **Build Automation:** Maven[cite: 1]
- **Architecture Pattern:** Strict Model-View-Controller (MVC) separation[cite: 1]
- **Testing:** Comprehensive automated engine verification test suite (`EngineTest`)
