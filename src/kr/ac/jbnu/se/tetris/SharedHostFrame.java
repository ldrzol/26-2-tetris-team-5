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
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedHostFrame extends JFrame {
    private final SharedGame game = new SharedGame();
    private final SharedBoardView view = new SharedBoardView();
    private final JLabel scoreLabel = new JLabel(" ", JLabel.CENTER);
    private final PeerConnection connection;
    private final long endTime = System.currentTimeMillis() + 5 * 60 * 1000L;
    private final int fee;
    private boolean settled = false;

    public SharedHostFrame(PeerConnection connection, int fee) {
        this.connection = connection;
        this.fee = fee;
        view.setPreferredSize(new Dimension(280, 440));
        add(scoreLabel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);

        setupKeys();
        connection.setListener(message -> onMessage(message));
        refresh();

        setTitle("땅따먹기 - 방장 (내 블록: 흰 테두리)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();

        new Timer(400, e -> {
            if (System.currentTimeMillis() >= endTime) {
                game.end();
            }
            game.tick();
            refresh();
        }).start();
    }

    private void refresh() {
        view.setState(game);
        if (game.isGameOver() && !settled) {
            settled = true;
            setTitle(Coins.settle(game.getWinner(), 0, fee));
        }
        long left = Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        scoreLabel.setText("남은 시간 " + left / 60 + ":" + String.format("%02d", left % 60)
                + "  |  나(P1): " + game.getScore(0) + "  상대(P2): " + game.getScore(1));
        connection.send(new SharedStateMessage(game));
    }

    private void onMessage(Object message) {
        if (!(message instanceof InputMessage)) {
            return;
        }
        InputMessage input = (InputMessage) message;
        SwingUtilities.invokeLater(() -> {
            applyInput(1, input.action);
            refresh();
        });
    }

    private void applyInput(int p, InputMessage.Action action) {
        switch (action) {
            case LEFT:
                game.moveLeft(p);
                break;
            case RIGHT:
                game.moveRight(p);
                break;
            case ROTATE:
                game.rotateRight(p);
                break;
            case DOWN:
                game.moveDown(p);
                break;
            case DROP:
                game.hardDrop(p);
                break;
        }
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "left", () -> game.moveLeft(0));
        bind(KeyEvent.VK_RIGHT, "right", () -> game.moveRight(0));
        bind(KeyEvent.VK_UP, "rotate", () -> game.rotateRight(0));
        bind(KeyEvent.VK_DOWN, "down", () -> game.moveDown(0));
        bind(KeyEvent.VK_SPACE, "drop", () -> game.hardDrop(0));
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
}