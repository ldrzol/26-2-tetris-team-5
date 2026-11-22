package kr.ac.jbnu.se.tetris.shared;

import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedClientFrame extends AbstractSharedFrame {
    private final PeerConnection connection;
    private final int fee;
    private final Runnable onExit;

    public SharedClientFrame(PeerConnection connection, int fee, Runnable onExit) {
        super("땅따먹기 - 상대 (내 블록: 검은 테두리)");
        this.connection = connection;
        this.fee = fee;
        this.onExit = onExit;

        setupKeys();
        connection.setListener(this::onMessage);

        releaseConnectionOnClose(connection, onExit);
        pack();
    }

    private void onMessage(Object message) {
        if (!(message instanceof SharedStateMessage)) {
            return;
        }
        SharedStateMessage state = (SharedStateMessage) message;
        SwingUtilities.invokeLater(() -> {
            view.setState(state);
            scoreLabel.setText("나(P2): " + state.getScore(1) + "    상대(P1): " + state.getScore(0));
            if (state.isGameOver()) {
                showResultOnce(connection, state.getWinner(), 1, fee, onExit);
            }
        });
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "left", InputMessage.Action.LEFT);
        bind(KeyEvent.VK_RIGHT, "right", InputMessage.Action.RIGHT);
        bind(KeyEvent.VK_UP, "rotate", InputMessage.Action.ROTATE);
        bind(KeyEvent.VK_DOWN, "down", InputMessage.Action.DOWN);
        bind(KeyEvent.VK_SPACE, "drop", InputMessage.Action.DROP);
    }

    private void bind(int keyCode, String name, InputMessage.Action action) {
        bind(keyCode, name, () -> connection.send(new InputMessage(action)));
    }
}