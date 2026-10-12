package kr.ac.jbnu.se.tetris;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

public class SharedLocalFrame extends JFrame {
    private final SharedGame game = new SharedGame();
    private final SharedBoardView view = new SharedBoardView();
    private final JLabel scoreLabel = new JLabel(" ", JLabel.CENTER);

    public SharedLocalFrame() {
        view.setPreferredSize(new Dimension(280, 440));
        add(scoreLabel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);

        setupKeys();
        refresh();

        setTitle("땅따먹기 (한 PC 테스트)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();

        new Timer(400, e -> {
            game.tick();
            refresh();
        }).start();
    }

    private void refresh() {
        view.setState(game);
        scoreLabel.setText("P1: " + game.getScore(0) + "    P2: " + game.getScore(1));
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "p1left", () -> game.moveLeft(0));
        bind(KeyEvent.VK_RIGHT, "p1right", () -> game.moveRight(0));
        bind(KeyEvent.VK_UP, "p1rotate", () -> game.rotateRight(0));
        bind(KeyEvent.VK_DOWN, "p1down", () -> game.moveDown(0));
        bind(KeyEvent.VK_SPACE, "p1drop", () -> game.hardDrop(0));

        bind(KeyEvent.VK_A, "p2left", () -> game.moveLeft(1));
        bind(KeyEvent.VK_D, "p2right", () -> game.moveRight(1));
        bind(KeyEvent.VK_W, "p2rotate", () -> game.rotateRight(1));
        bind(KeyEvent.VK_S, "p2down", () -> game.moveDown(1));
        bind(KeyEvent.VK_Q, "p2drop", () -> game.hardDrop(1));
    }

    private void bind(int keyCode, String name, Runnable action) {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.run();
                refresh();
            }
        });
    }

    public static void main(String[] args) {
        SharedLocalFrame frame = new SharedLocalFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}