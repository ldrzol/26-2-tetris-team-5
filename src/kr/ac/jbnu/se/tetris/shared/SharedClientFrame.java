package kr.ac.jbnu.se.tetris.shared;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedClientFrame extends SharedFrame {

    public SharedClientFrame(PeerConnection connection, int fee, Runnable onExit) {
        super(connection, fee, onExit, "땅따먹기 - 상대 (내 블록: 검은 테두리)");
        scoreLabel.setText("방장이 시작하기를 기다리는 중...");
        pack();
        connection.send(READY);   // [버그 14] 내 창이 준비됐다고 방장에게 알린다
    }

    @Override
    protected void onMessage(Object message) {
        if (message instanceof String && ((String) message).startsWith(COUNT)) {
            // [버그 14] 방장이 보낸 카운트다운을 그대로 띄운다
            String text = ((String) message).substring(COUNT.length()) + "초 뒤에 시작합니다";
            SwingUtilities.invokeLater(() -> scoreLabel.setText(text));
            return;
        }
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