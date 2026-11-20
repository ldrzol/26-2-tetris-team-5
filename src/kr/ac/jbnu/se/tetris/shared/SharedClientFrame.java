package kr.ac.jbnu.se.tetris.shared;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedClientFrame extends SharedFrame {

    public SharedClientFrame(PeerConnection connection, int fee, Runnable onExit) {
        super(connection, fee, onExit, "땅따먹기 - 상대 (내 블록: 검은 테두리)");
        pack();
    }

    @Override
    protected void onMessage(Object message) {
        if (!(message instanceof SharedStateMessage)) {
            return;
        }
        SharedStateMessage state = (SharedStateMessage) message;
        SwingUtilities.invokeLater(() -> {
            view.setState(state);
            scoreLabel.setText("나(P2): " + state.getScore(1) + "    상대(P1): " + state.getScore(0));
            if (state.isGameOver() && !settled) {
                settled = true;
                ResultPanel.showShared(this, connection, state.getWinner(), 1, fee, onExit);
            }
        });
    }

    @Override
    protected void onKey(InputMessage.Action action) {
        connection.send(new InputMessage(action));
    }
}