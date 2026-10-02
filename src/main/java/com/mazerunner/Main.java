package com.mazerunner;

import com.mazerunner.controller.GameController;
import com.mazerunner.model.LevelManager;
import com.mazerunner.model.MazeModel;
import com.mazerunner.view.MazePanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main application entry point ensuring EDT Thread Safety for Swing initialization.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Maze Runner: Kinetic Gravity Engine v2.0");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            LevelManager levelManager = new LevelManager();
            MazeModel model = new MazeModel(levelManager.getCurrentLevel());
            MazePanel panel = new MazePanel(model);

            GameController controller = new GameController(model, panel, levelManager);
            panel.addKeyListener(controller);

            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);

            panel.requestFocusInWindow();
        });
    }
}