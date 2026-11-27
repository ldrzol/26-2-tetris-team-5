package kr.ac.jbnu.se.tetris.network;

import java.util.function.Supplier;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.save.Wallet;

/**
 * 네트워크 클래식 대전의 통신과 정산.
 *
 * [리팩토링: 기능 편애 → Extract Class] 예전에는 게임 화면(VersusPanel)이 판 보내기, 메시지 받기,
 * 결과 보내기, 연결 닫기, 참가비 정산을 직접 했다. 이제 화면은 그리기와 조작만 하고,
 * 네트워크와 코인에 관한 일은 이 클래스가 맡는다.
 */
public class NetworkMatch {

    /** 상대에게서 온 일을 화면에 알린다. 모두 화면 스레드에서 불린다. */
    public interface Listener {
        /** 상대 판·점수가 왔다. */
        void onRivalState(BoardStateMessage state);

        /** 상대가 아이템을 썼다. */
        void onRivalItem(ItemMessage item);

        /** [버그 17] 방장이 정한 결과가 왔다. 이미 내 쪽에서 본 결과로 뒤집어서 넘긴다. */
        void onResult(MatchResult result);

        /** [버그 21] 상대가 나갔거나 연결이 끊겼다. */
        void onRivalLeft();
    }

    /** 내 판을 상대에게 보내는 간격(ms). */
    private static final int SNAPSHOT_MS = 100;

    private final PeerConnection connection;
    private final boolean host;
    private final int fee;
    private final Supplier<BoardStateMessage> mySnapshot;
    private final Timer snapshots;

    /** [버그 20] 대전이 끝나는 시각(ms). */
    private long endTime;

    /**
     * @param host       내가 방장인지. 승패는 방장만 정한다.
     * @param fee        참가비. 시작할 때 이미 냈다.
     * @param mySnapshot 지금 내 판을 메시지로 만들어 주는 곳 (화면이 만든다)
     */
    public NetworkMatch(PeerConnection connection, boolean host, int fee, Supplier<BoardStateMessage> mySnapshot) {
        this.connection = connection;
        this.host = host;
        this.fee = fee;
        this.mySnapshot = mySnapshot;
        this.snapshots = new Timer(SNAPSHOT_MS, e -> connection.send(mySnapshot.get()));
    }

    /** 대전을 시작한다. 받기·끊김 알림을 연결하고, 판 보내기와 시계를 시작한다. */
    public void start(int seconds, Listener listener) {
        connection.setListener(message -> SwingUtilities.invokeLater(() -> dispatch(message, listener)));
        connection.setOnDisconnected(() -> SwingUtilities.invokeLater(listener::onRivalLeft));   // [버그 21]
        snapshots.start();                                                                       // [버그 22]
        endTime = System.currentTimeMillis() + seconds * 1000L;                                  // [버그 20]
    }

    /** 받은 메시지를 종류별로 화면에 넘긴다. */
    private void dispatch(Object message, Listener listener) {
        if (message instanceof BoardStateMessage)
            listener.onRivalState((BoardStateMessage) message);
        else if (message instanceof ItemMessage)
            listener.onRivalItem((ItemMessage) message);
        else if (message instanceof MatchResult)
            listener.onResult(((MatchResult) message).opposite());   // [버그 17] 방장 쪽에서 본 결과라 뒤집는다
    }

    public boolean isHost() {
        return host;
    }

    /** [버그 20] 남은 초. 끝나는 시각에서 지금 시각을 뺀다 (올림). */
    public int secondsLeft() {
        return (int) Math.max(0, (endTime - System.currentTimeMillis() + 999) / 1000);
    }

    /**
     * 아이템을 상대에게 보낸다. 블록 교환은 내 블록을 실어 보내고, 상대 블록은 답장으로 받는다.
     * (받아 둔 상대 화면에서 꺼내 쓰면 최대 SNAPSHOT_MS 만큼 옛날 것이라 교환이 어긋난다)
     */
    public void sendItem(Attack attack, Tetrominoes myPiece) {
        if (attack == Attack.SWAP)
            connection.send(new ItemMessage(Attack.SWAP, myPiece, false));
        else
            connection.send(new ItemMessage(attack));
    }

    /** 블록 교환 답장. 내가 못 받았으면 빈 모양을 돌려줘서 상대도 그대로 두게 한다. */
    public void replySwap(Tetrominoes mine) {
        connection.send(new ItemMessage(Attack.SWAP, mine, true));
    }

    /** [버그 22] 판 보내기를 멈춘다. 마지막 판(점수·게임 오버)은 한 번 더 보낸다. */
    public void stopSending() {
        if (snapshots.isRunning()) {
            connection.send(mySnapshot.get());
            snapshots.stop();
        }
    }

    /**
     * [버그 17] 끝났을 때. 방장이면 정한 결과를 참가자에게 보내고, 연결을 닫고,
     * 참가비를 정산해서 받은 코인을 돌려준다.
     */
    public int finish(MatchResult result) {
        stopSending();
        if (host)
            connection.send(result);
        connection.close();
        return settle(result);
    }

    /** [버그 21] 상대가 끊겼을 때. 판 보내기만 멈춘다. (끊겼을 때 정산 규칙은 팀과 정하기) */
    public void rivalLeft() {
        snapshots.stop();
    }

    /** [버그 21] 내가 나갈 때. 연결을 닫으면 상대 쪽에서 끊겼다는 걸 안다. */
    public void leave() {
        snapshots.stop();
        connection.close();
    }

    /** [버그 18] 참가비 정산. 이기면 2배, 비기면 돌려받고, 지면 이미 낸 참가비를 잃는다. */
    private int settle(MatchResult result) {
        if (result == MatchResult.WIN) {
            Wallet.addCoins(fee * 2);
            return fee * 2;
        }
        if (result == MatchResult.DRAW) {
            Wallet.addCoins(fee);
            return fee;
        }
        return 0;
    }
}