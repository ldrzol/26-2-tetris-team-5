package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.save.Wallet;
import kr.ac.jbnu.se.tetris.shared.SharedClientFrame;
import kr.ac.jbnu.se.tetris.shared.SharedHostFrame;

import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.PeerConnection;

/**
 * 2인 대전 로비. Figma "Desktop - 12".
 *
 * 메뉴 모양은 다른 메뉴 화면과 같고, 두 항목이 방 만들기 / 방 참가다.
 * 연결 진행 상황은 디자인에 없지만 없으면 쓸 수 없으므로 상자 아래에 한 줄로 띄운다.
 *
 * 연결한 뒤에는 방장이 모드와 참가비를 알리고, 참가자가 코인을 보고 수락하거나 거절한다.
 */
class LobbyFrame extends MenuScreen {

	private static final int PORT = 5000;

	/** 참가비 하한. 이보다 적게는 걸 수 없다. */
	private static final int MIN_FEE = 100;

	// 연결한 뒤 주고받는 신호. 둘 다 같은 문자열을 봐야 한다.
	private static final String READY = "READY";
	private static final String ACCEPT = "ACCEPT";
	private static final String DECLINE = "DECLINE";
	private static final String MODE = "MODE:";

	private final MainFrame frame;
	private PeerConnection connection;

	private String status = null;
	private boolean busy = false;

	/** 내가 방을 연 쪽인지. 방장만 모드와 참가비를 정한다. */
	private boolean host = false;

	/** 땅따먹기면 true, 클래식 대전이면 false. */
	private boolean shared = false;

	private int fee;

	LobbyFrame(MainFrame frame) {
		super(new String[] { "멀티 모드", "- 2인 -" }, new float[] { 128f, 96f }, 252,
				new String[] { "방 만들기", "방 참가" }, null);
		this.frame = frame;
	}

	@Override
	void select(int index) {
		if (index == 0)
			createRoom();
		else
			joinRoom();
	}

	@Override
	boolean isSelectable(int index) {
		return !busy;
	}

	@Override
	String status() {
		return status;
	}

	// 방 열기 / 들어가기

	private void createRoom() {
		if (!askMode() || !askFee())
			return;

		host = true;
		busy = true;
		status = waitingMessage();
		repaint();

		new Thread(() -> {
			try {
				connection = PeerConnection.host(PORT, this::onMessage);
				SwingUtilities.invokeLater(() -> onConnected("상대가 들어왔습니다"));
			} catch (IOException e) {
				SwingUtilities.invokeLater(() -> onFailed("방 만들기 실패: " + e.getMessage()));
			}
		}).start();
	}

	/** 모드를 묻는다. 취소하면 false. */
	private boolean askMode() {
		String[] options = { "클래식", "땅따먹기" };
		int choice = JOptionPane.showOptionDialog(this, "게임 모드를 고르세요", "모드 선택",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (choice < 0)
			return false;
		shared = choice == 1;
		return true;
	}

	/** 참가비를 묻는다. 취소하거나 걸 수 없는 금액이면 false. */
	private boolean askFee() {
		String input = JOptionPane.showInputDialog(this,
				"참가비를 입력하세요 (" + MIN_FEE + " 이상, 내 코인 " + Wallet.coins() + ")",
				"참가비", JOptionPane.QUESTION_MESSAGE);
		if (input == null)
			return false;

		try {
			fee = Integer.parseInt(input.trim());
		} catch (NumberFormatException e) {
			return fail("참가비는 숫자로 입력하세요");
		}
		if (fee < MIN_FEE || fee > Wallet.coins())
			return fail("참가비는 " + MIN_FEE + " 이상, 내 코인 이하여야 합니다");
		return true;
	}

	private boolean fail(String message) {
		status = message;
		repaint();
		return false;
	}

	/** 내 IP 를 알 수 있으면 같이 알려 준다. 못 찾아도 방은 열린다. */
	private String waitingMessage() {
		try {
			return "내 IP: " + InetAddress.getLocalHost().getHostAddress()
					+ " (포트 " + PORT + ") - 상대를 기다리는 중...";
		} catch (IOException e) {
			return "내 IP를 알 수 없음. 상대를 기다리는 중...";
		}
	}

	private void joinRoom() {
		host = false;
		frame.showInput("방장의 IP를 입력하세요", "", ip -> {
			if (ip != null && !ip.trim().isEmpty())
				connect(ip.trim());
		});
	}

	private void connect(String ip) {
		busy = true;
		status = ip + " 에 접속하는 중...";
		repaint();

		new Thread(() -> {
			try {
				connection = PeerConnection.connect(ip, PORT, this::onMessage);
				SwingUtilities.invokeLater(() -> onConnected("방에 들어갔습니다"));
			} catch (IOException e) {
				SwingUtilities.invokeLater(() -> onFailed("접속 실패: " + e.getMessage()));
			}
		}).start();
	}

	private void onConnected(String text) {
		if (host) {
			status = text + " - 상대 준비를 기다리는 중...";
		} else {
			status = text + " - 방장이 시작하기를 기다리는 중...";
			connection.send(READY);
		}
		repaint();
	}

	private void onFailed(String text) {
		busy = false;
		status = text;
		repaint();
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
			status = "참가비 " + fee + " 코인 - 상대의 확인을 기다리는 중...";
			repaint();
		} else if (text.equals(ACCEPT)) {
			Wallet.addCoins(-fee);
			openGame();
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
		openGame();
	}

	private void cancel(String message) {
		connection.close();
		busy = false;
		status = message;
		repaint();
	}

	// 게임 시작

	private void openGame() {
		if (shared)
			openSharedWindow();
		else
			frame.showVersus(connection, fee);
	}

	/**
	 * 땅따먹기는 판 모양이 달라서 아직 디자인 화면에 들어가 있지 않다. 따로 창을 띄우고,
	 * 그동안 메인 창은 숨겨 뒀다가 끝나면 다시 보여 준다.
	 */
	private void openSharedWindow() {
		Runnable onExit = () -> {
			frame.setVisible(true);
			frame.showScreen(MainFrame.MENU);
		};
		JFrame game = host
				? new SharedHostFrame(connection, fee, onExit)
				: new SharedClientFrame(connection, fee, onExit);
		game.setLocationRelativeTo(null);
		game.setVisible(true);
		frame.setVisible(false);
	}
}
