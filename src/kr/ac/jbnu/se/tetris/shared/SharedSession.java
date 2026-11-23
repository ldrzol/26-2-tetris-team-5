package kr.ac.jbnu.se.tetris.shared;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

/**
 * 땅따먹기 한 판의 공통 부분. 방장/참가자가 다른 부분만 하위 클래스가 채운다.
 *
 * 화면은 갖고 있지 않다. 보여 줄 것은 View 로 넘기고, 키 입력은 화면이 onKey 로 넘겨 준다.
 * (예전에는 따로 창을 띄웠지만 이제 메인 창 안의 화면이 그린다)
 */
public abstract class SharedSession {

    /** 판을 그리는 쪽. 모두 화면 스레드에서 불린다. */
    public interface View {
        /** 판 상태가 바뀌었다. */
        void showState(SharedState state);

        /** 판 위에 띄울 한 줄 (기다리는 중, 카운트다운, 남은 시간, 연결 끊김). */
        void showStatus(String text);

        /** 판이 끝났다. winner: 0=P1, 1=P2, -1=무승부. me: 나는 P1(0) 또는 P2(1). */
        void showResult(int winner, int me);
    }

    protected final transient PeerConnection connection;
    protected final int fee;
    protected final View view;
    protected boolean settled = false;
    /** [버그 14] 참가자가 화면을 열고 준비됐다는 신호. */
    protected static final String READY = "SHARED_READY";

    /** [버그 14] 카운트다운 신호. 뒤에 남은 초가 붙는다. 예: "COUNT:3" */
    protected static final String COUNT = "COUNT:";

    protected SharedSession(PeerConnection connection, int fee, View view) {
        this.connection = connection;
        this.fee = fee;
        this.view = view;

        connection.setListener(this::onMessage);
        // [버그 11] 상대가 사라지면(창 닫기, 랜선 빠짐, 먹통) 안내한다. 받는 스레드에서 불리므로 화면 스레드로 넘긴다.
        connection.setOnDisconnected(() -> SwingUtilities.invokeLater(this::onDisconnected));
    }

    /** 방장이면 판을 돌리는 쪽이다. */
    public static SharedSession start(PeerConnection connection, boolean host, int fee, View view) {
        return host ? new SharedHost(connection, fee, view) : new SharedClient(connection, fee, view);
    }

    /** 상대에게서 메시지를 받았을 때 (방장/참가자가 다름). */
    protected abstract void onMessage(Object message);

    /** 내가 키를 눌렀을 때 (방장은 직접 움직이고, 참가자는 방장에게 보냄). */
    public abstract void onKey(InputMessage.Action action);

    /** [버그 9] 화면을 떠나거나 게임을 더 진행할 수 없을 때 하위 클래스가 정리할 것. */
    protected void onClosed() {
        // 기본은 정리할 것이 없다. 방장은 블록 떨어뜨리는 타이머를 멈춘다.
    }

    /** 화면을 떠날 때. 타이머를 멈추고 연결을 닫는다. */
    public void close() {
        settled = true;
        onClosed();
        connection.close();
    }

    /** 끝났으면 한 번만 결과를 알린다. */
    protected void finish(SharedState state, int me) {
        if (state.isGameOver() && !settled) {
            settled = true;
            view.showResult(state.getWinner(), me);
        }
    }

    /** [버그 11] 상대와 연결이 끊겼을 때. 아직 결과가 안 났으면 멈추고 안내만 띄운다. */
    private void onDisconnected() {
        if (settled) {
            return;   // 이미 결과가 나왔으면 할 것이 없다
        }
        settled = true;   // 끊긴 뒤에는 결과·정산을 하지 않는다 (끊겼을 때 정산 규칙은 팀과 정하기)
        onClosed();
        view.showStatus("상대와 연결이 끊어졌습니다 - Esc 로 나가세요");
    }
}
