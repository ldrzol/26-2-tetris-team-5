package kr.ac.jbnu.se.tetris;

import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.PeerConnection;

/**
 * 2인 대전 로비. Figma "Desktop - 12".
 *
 * 메뉴 모양은 다른 메뉴 화면과 같고, 두 항목이 방 만들기 / 방 참가다.
 * 연결 진행 상황은 디자인에 없지만 없으면 쓸 수 없으므로 상자 아래에 한 줄로 띄운다.
 */
class LobbyFrame extends MenuScreen {

	private static final int PORT = 5000;

	private final MainFrame frame;
	private PeerConnection connection;
	private String status = null;
	private boolean busy = false;
	private String mode;
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

	private void createRoom() {
		String[] options = { "클래식", "땅따먹기" };
		int choice = JOptionPane.showOptionDialog(this, "게임 모드를 고르세요", "모드 선택",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (choice < 0)
			return;
		mode = (choice == 0) ? "CLASSIC" : "SHARED";

		String input = JOptionPane.showInputDialog(this,
				"참가비를 입력하세요 (100 이상, 내 코인 " + Coins.get() + ")", "참가비", JOptionPane.QUESTION_MESSAGE);
		if (input == null)
			return;
		try {
			fee = Integer.parseInt(input.trim());
		} catch (NumberFormatException e) {
			status = "참가비는 숫자로 입력하세요";
			repaint();
			return;
		}
		if (fee < 100 || fee > Coins.get()) {
			status = "참가비는 100 이상, 내 코인 이하여야 합니다";
			repaint();
			return;
		}


		busy = true;
		try {
			status = "내 IP: " + InetAddress.getLocalHost().getHostAddress()
					+ " (포트 " + PORT + ") - 상대를 기다리는 중...";
		} catch (IOException e) {
			status = "내 IP를 알 수 없음. 상대를 기다리는 중...";
		}
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

	private void joinRoom() {
        mode = null;
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
		if (mode == null) {
			status = text + " - 방장이 시작하기를 기다리는 중...";
			connection.send("READY");
		} else {
			status = text + " - 상대 준비를 기다리는 중...";
		}
		repaint();
		frame.showScreen(MainFrame.VERSUS);
	}



	private void onFailed(String text) {
		busy = false;
		status = text;
		repaint();
	}

	private void onMessage(Object message) {
		if (!(message instanceof String))
			return;
		String text = (String) message;
		SwingUtilities.invokeLater(() -> handleText(text));
	}

	private void handleText(String text) {
		if (mode != null) {
			if (text.equals("READY")) {
				connection.send("MODE:" + mode + ":" + fee);
				status = "참가비 " + fee + " 코인 - 상대의 확인을 기다리는 중...";
				repaint();
			} else if (text.equals("ACCEPT")) {
				Coins.add(-fee);
				openGame(mode, true);
			} else if (text.equals("DECLINE")) {
				connection.close();
				busy = false;
				status = "상대의 코인이 부족해서 취소되었습니다";
				repaint();
			}
		} else if (text.startsWith("MODE:")) {
			String[] parts = text.split(":");
			String selectedMode = parts[1];
			fee = Integer.parseInt(parts[2]);
			if (Coins.get() >= fee) {
				Coins.add(-fee);
				connection.send("ACCEPT");
				openGame(selectedMode, false);
			} else {
				connection.send("DECLINE");
				connection.close();
				busy = false;
				status = "코인이 부족합니다 (참가비 " + fee + ")";
				repaint();
			}
		}
	}

	private void openGame(String selectedMode, boolean isHost) {
		JFrame game;
		if (selectedMode.equals("SHARED")) {
			game = isHost ? new SharedHostFrame(connection, fee) : new SharedClientFrame(connection, fee);
		} else {
			game = new DuelFrame(connection, fee);
		}
		game.setLocationRelativeTo(null);
		game.setVisible(true);
		frame.dispose();
	}
}
