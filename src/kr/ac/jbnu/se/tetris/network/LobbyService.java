package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.net.InetAddress;

import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.save.Wallet;

/**
 * 2인 대전 로비의 연결, 신호 주고받기, 참가비 정산. 화면(LobbyFrame)은 결과만 받아서 보여 준다.
 *
 * 받은 신호는 화면 스레드에서 처리한다. host,gameMode, fee 를 한 스레드에서만 바꾸기 위해서다.
 */
public class LobbyService {

    /** 화면에 알려 줄 일. 모두 화면 스레드에서 불린다. */
    public interface Listener {
        /** 진행 상황이 바뀜 (계속 기다리는 중). */
        void onStatus(String text);

        /** 연결 실패나 취소. 다시 고를 수 있게 한다. */
        void onFailed(String text);

        /** 둘 다 참가비를 내서 게임을 시작한다. */
        void onStart(PeerConnection connection, boolean host, GameMode gameMode, int fee);
    }

    public static final int PORT = 5000;

    /** 참가비 하한. 이보다 적게는 걸 수 없다. */
    public static final int MIN_FEE = 100;

    // 연결한 뒤 주고받는 신호. 둘 다 같은 문자열을 봐야 한다.
    private static final String READY = "READY";
    private static final String ACCEPT = "ACCEPT";
    private static final String DECLINE = "DECLINE";
    private static final String MODE = "MODE:";
    private static final String START = "START";
    private static final String CANCEL = "CANCEL";

    private final Listener listener;
    private PeerConnection connection;

    /** 내가 방을 연 쪽인지. 방장만 모드와 참가비를 정한다. */
    private boolean host = false;

    /** 고른 모드. */
    private GameMode gameMode = GameMode.CLASSIC;

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

    public void openRoom(GameMode gameMode, int fee) {
        this.host = true;
        this.gameMode = gameMode;
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
        else
            handleAsGuest(text);
    }

    private void handleAsHost(String text) {
        if (text.equals(READY)) {
            connection.send(MODE + gameMode.name() + ":" + fee);
            listener.onStatus("참가비 " + fee + " 코인 - 상대의 확인을 기다리는 중...");
        } else if (text.equals(ACCEPT)) {
            // 기다리는 동안 내 코인이 줄었을 수 있으니 시작 직전에 다시 확인한다
            if (Wallet.coins() < fee) {
                connection.send(CANCEL);
                cancel("내 코인이 부족해서 취소되었습니다");
                return;
            }
            Wallet.addCoins(-fee);
            connection.send(START);
            listener.onStart(connection, host, gameMode, fee);
        } else if (text.equals(DECLINE)) {
            cancel("상대의 코인이 부족해서 취소되었습니다");
        }
    }

    private void handleAsGuest(String text) {
        if (text.startsWith(MODE)) {
            handleOffer(text);
        } else if (text.equals(START)) {
            Wallet.addCoins(-fee);
            listener.onStart(connection, host, gameMode, fee);
        } else if (text.equals(CANCEL)) {
            cancel("방장의 코인이 부족해서 취소되었습니다");
        }
    }

    /** 방장이 보낸 "MODE:<모드>:<참가비>" 를 받아 수락하거나 거절한다. 코인은 방장이 START 를 보낸 뒤에 낸다. */
    private void handleOffer(String text) {
        if (!readOffer(text)) {
            cancel("방장이 보낸 정보가 올바르지 않아 취소되었습니다");
            return;
        }
        if (Wallet.coins() < fee) {
            connection.send(DECLINE);
            cancel("코인이 부족합니다 (참가비 " + fee + ")");
            return;
        }
        connection.send(ACCEPT);
        listener.onStatus("참가비 " + fee + " 코인 - 방장이 시작하기를 기다리는 중...");
    }

    /** "MODE:<모드>:<참가비>" 를 읽어 gameMode, fee 에 넣는다. 형식이 틀리면 false. */
    private boolean readOffer(String text) {
        String[] parts = text.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            gameMode = GameMode.valueOf(parts[1]);
            fee = Integer.parseInt(parts[2]);
        } catch (IllegalArgumentException e) {   // 숫자가 아닐 때 나는 NumberFormatException 도 여기에 포함
            return false;
        }
        return fee >= MIN_FEE;
    }

    private void cancel(String message) {
        connection.close();
        listener.onFailed(message);
    }
}