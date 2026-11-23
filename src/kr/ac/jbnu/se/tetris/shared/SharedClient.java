package kr.ac.jbnu.se.tetris.shared;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

/** 땅따먹기 참가자. 판은 방장이 돌리고, 여기서는 받은 상태를 보여 주고 키만 보낸다. */
class SharedClient extends SharedSession {

    SharedClient(PeerConnection connection, int fee, View view) {
        super(connection, fee, view);
        view.showStatus("방장이 시작하기를 기다리는 중...");
        connection.send(READY);   // [버그 14] 내 화면이 준비됐다고 방장에게 알린다
    }

    @Override
    protected void onMessage(Object message) {
        if (message instanceof String && ((String) message).startsWith(COUNT)) {
            // [버그 14] 방장이 보낸 카운트다운을 그대로 띄운다
            String text = ((String) message).substring(COUNT.length()) + "초 뒤에 시작합니다";
            SwingUtilities.invokeLater(() -> view.showStatus(text));
            return;
        }
        if (!(message instanceof SharedStateMessage)) {
            return;
        }
        SharedStateMessage state = (SharedStateMessage) message;
        SwingUtilities.invokeLater(() -> {
            view.showState(state);
            finish(state, 1);
        });
    }

    @Override
    public void onKey(InputMessage.Action action) {
        connection.send(new InputMessage(action));
    }
}