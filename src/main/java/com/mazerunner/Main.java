package com.mazerunner;

import com.mazerunner.controller.GameController;
import com.mazerunner.model.LevelManager;
import com.mazerunner.model.MazeModel;
import com.mazerunner.model.SoundManager;
import com.mazerunner.view.LevelSelectPanel;
import com.mazerunner.view.MazePanel;
import com.mazerunner.view.MenuPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main application entry point.
 *
 * Flow:
 *   Main Menu  →  Level Selector  →  Game
 *
 * Each screen is swapped into the same JFrame so the window never closes/reopens.
 */
public class Main {

    private static JFrame frame;

    public static void main(String[] args) {
        // Pre-warm the sound engine on a background thread to avoid first-play latency
        Thread warmup = new Thread(() -> SoundManager.getInstance());
        warmup.setDaemon(true);
        warmup.start();

        SwingUtilities.invokeLater(Main::showMenu);
    }

    // ── Screen 1: Main Menu ────────────────────────────────────────────────
    private static void showMenu() {
        if (frame == null) {
            frame = new JFrame("Maze Runner: Kinetic Gravity Engine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
        }

        MenuPanel menuPanel = new MenuPanel(new MenuPanel.MenuCallback() {
            @Override public void onPlay() { SwingUtilities.invokeLater(Main::showLevelSelect); }
            @Override public void onQuit() { System.exit(0); }
        });

        frame.getContentPane().removeAll();
        frame.add(menuPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();
        frame.setState(JFrame.NORMAL);
        frame.requestFocus();
        menuPanel.requestFocusInWindow();
    }

    // ── Screen 2: Level Selector ───────────────────────────────────────────
    private static void showLevelSelect() {
        LevelSelectPanel selectPanel = new LevelSelectPanel(new LevelSelectPanel.SelectCallback() {
            @Override
            public void onLevelSelected(int startIndex) {
                // startIndex is 0-based (Level 1 = index 0)
                SwingUtilities.invokeLater(() -> startGame(startIndex));
            }
            @Override
            public void onBack() {
                SwingUtilities.invokeLater(Main::showMenu);
            }
        });

        frame.getContentPane().removeAll();
        frame.add(selectPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        selectPanel.requestFocusInWindow();
        frame.revalidate();
        frame.repaint();
    }

    // ── Screen 3: Game ─────────────────────────────────────────────────────
    private static void startGame(int levelIndex) {
        LevelManager levelManager = new LevelManager();
        levelManager.setLevelIndex(levelIndex);   // jump to chosen level

        MazeModel model = new MazeModel(levelManager.getCurrentLevel());
        MazePanel panel = new MazePanel(model);

        GameController controller = new GameController(model, panel, levelManager);
        panel.addKeyListener(controller);

        frame.getContentPane().removeAll();
        frame.add(panel);
        // Maximise so the dynamic cell-size calculation fills the whole screen
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        panel.requestFocusInWindow();
        frame.revalidate();
        frame.repaint();
    }
}

