package kr.ac.jbnu.se.tetris.shared;

import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedHostFrame extends AbstractSharedFrame {

    /** 한 판 제한 시간. */
    private static final long TIME_LIMIT_MS = 5 * 60 * 1000L;

    /** 블록이 한 칸 내려오는 간격. */
    private static final int FALL_MS = 400;
    private final SharedGame game = new SharedGame();
    private final PeerConnection connection;
    private final long endTime = System.currentTimeMillis() + TIME_LIMIT_MS;
    private final int fee;
    private final Runnable onExit;

    public SharedHostFrame(PeerConnection connection, int fee, Runnable onExit) {
        super("땅따먹기 - 방장 (내 블록: 흰 테두리)");
        this.connection = connection;
        this.fee = fee;
        this.onExit = onExit;

        setupKeys();
        connection.setListener(this::onMessage);
        refresh();

        releaseConnectionOnClose(connection, onExit);
        pack();

        new Timer(FALL_MS, e -> {
            if (System.currentTimeMillis() >= endTime) {
                game.end();
            }
            game.tick();
            refresh();
        }).start();
    }

    private void refresh() {
        view.setState(game);
        if (game.isGameOver()) {
            showResultOnce(connection, game.getWinner(), 0, fee, onExit);
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

    @Override
    protected void afterKey() {
        refresh();
    }
}