package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.net.InetAddress;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.save.Wallet;

/**
 * 2인 대전 로비의 연결, 신호 주고받기, 참가비 정산. 화면(LobbyFrame)은 결과만 받아서 보여 준다.
 *
 * 받은 신호는 화면 스레드에서 처리한다. host, shared, fee 를 한 스레드에서만 바꾸기 위해서다.
 */
public class LobbyService {

    /** 화면에 알려 줄 일. 모두 화면 스레드에서 불린다. */
    public interface Listener {
        /** 진행 상황이 바뀜 (계속 기다리는 중). */
        void onStatus(String text);

        /** 연결 실패나 취소. 다시 고를 수 있게 한다. */
        void onFailed(String text);

        /** 둘 다 참가비를 내서 게임을 시작한다. */
        void onStart(PeerConnection connection, boolean host, boolean shared, int fee);
    }

    public static final int PORT = 5000;

    /** 참가비 하한. 이보다 적게는 걸 수 없다. */
    public static final int MIN_FEE = 100;

    // 연결한 뒤 주고받는 신호. 둘 다 같은 문자열을 봐야 한다.
    private static final String READY = "READY";
    private static final String ACCEPT = "ACCEPT";
    private static final String DECLINE = "DECLINE";
    private static final String MODE = "MODE:";

    private final Listener listener;
    private PeerConnection connection;

    /** 내가 방을 연 쪽인지. 방장만 모드와 참가비를 정한다. */
    private boolean host = false;

    /** 땅따먹기면 true, 클래식 대전이면 false. */
    private boolean shared = false;

    private int fee;

    public LobbyService(Listener listener) {
        this.listener = listener;
    }

    /** 걸 수 있는 참가비인지. */
    public boolean isValidFee(int fee) {
        return fee >= MIN_FEE && fee <= Wallet.coins();
    }

    /** 내 IP. 못 찾으면 null. */
    public String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return null;
        }
    }

    // 방 열기 / 들어가기

    public void openRoom(boolean shared, int fee) {
        this.host = true;
        this.shared = shared;
        this.fee = fee;

        new Thread(() -> {
            try {
                connection = PeerConnection.host(PORT, this::onMessage);
                SwingUtilities.invokeLater(() -> onConnected("상대가 들어왔습니다"));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> listener.onFailed("방 만들기 실패: " + e.getMessage()));
            }
        }).start();
    }

    public void joinRoom(String ip) {
        host = false;

        new Thread(() -> {
            try {
                connection = PeerConnection.connect(ip, PORT, this::onMessage);
                SwingUtilities.invokeLater(() -> onConnected("방에 들어갔습니다"));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> listener.onFailed("접속 실패: " + e.getMessage()));
            }
        }).start();
    }

    private void onConnected(String text) {
        if (host) {
            listener.onStatus(text + " - 상대 준비를 기다리는 중...");
        } else {
            listener.onStatus(text + " - 방장이 시작하기를 기다리는 중...");
            connection.send(READY);
        }
    }

    // 신호 주고받기

    private void onMessage(Object message) {
        if (message instanceof String)
            SwingUtilities.invokeLater(() -> handleText((String) message));
    }

    private void handleText(String text) {
        if (host)
            handleAsHost(text);
        else if (text.startsWith(MODE))
            handleOffer(text);
    }

    private void handleAsHost(String text) {
        if (text.equals(READY)) {
            connection.send(MODE + (shared ? "SHARED" : "CLASSIC") + ":" + fee);
            listener.onStatus("참가비 " + fee + " 코인 - 상대의 확인을 기다리는 중...");
        } else if (text.equals(ACCEPT)) {
            Wallet.addCoins(-fee);
            listener.onStart(connection, host, shared, fee);
        } else if (text.equals(DECLINE)) {
            cancel("상대의 코인이 부족해서 취소되었습니다");
        }
    }

    /** 방장이 보낸 "MODE:<모드>:<참가비>" 를 받아 수락하거나 거절한다. */
    private void handleOffer(String text) {
        String[] parts = text.split(":");
        shared = parts[1].equals("SHARED");
        fee = Integer.parseInt(parts[2]);

        if (Wallet.coins() < fee) {
            connection.send(DECLINE);
            cancel("코인이 부족합니다 (참가비 " + fee + ")");
            return;
        }
        Wallet.addCoins(-fee);
        connection.send(ACCEPT);
        listener.onStart(connection, host, shared, fee);
    }

    private void cancel(String message) {
        connection.close();
        listener.onFailed(message);
    }
}