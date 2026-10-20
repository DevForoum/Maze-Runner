package com.mazerunner;

import com.mazerunner.controller.GameController;
import com.mazerunner.model.LevelManager;
import com.mazerunner.model.MazeModel;
import com.mazerunner.model.SoundManager;
import com.mazerunner.view.MazePanel;
import com.mazerunner.view.MenuPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main application entry point — shows the animated main menu,
 * then transitions to the game on Play. EDT Thread-Safe.
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

    /** Show the animated main menu screen */
    private static void showMenu() {
        if (frame == null) {
            frame = new JFrame("Maze Runner: Kinetic Gravity Engine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
        }

        MenuPanel menuPanel = new MenuPanel(new MenuPanel.MenuCallback() {
            @Override
            public void onPlay() {
                SwingUtilities.invokeLater(Main::startGame);
            }

            @Override
            public void onQuit() {
                System.exit(0);
            }
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

    /** Transition from menu to the actual game */
    private static void startGame() {
        LevelManager levelManager = new LevelManager();
        MazeModel model = new MazeModel(levelManager.getCurrentLevel());
        MazePanel panel = new MazePanel(model);

        GameController controller = new GameController(model, panel, levelManager);
        panel.addKeyListener(controller);

        frame.getContentPane().removeAll();
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        panel.requestFocusInWindow();
        frame.revalidate();
        frame.repaint();
    }
}
