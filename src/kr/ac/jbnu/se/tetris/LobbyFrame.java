package kr.ac.jbnu.se.tetris;

import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JOptionPane;
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
		String ip = JOptionPane.showInputDialog(this, "방장의 IP를 입력하세요");
		if (ip == null || ip.trim().isEmpty())
			return;

		busy = true;
		status = ip.trim() + " 에 접속하는 중...";
		repaint();

		new Thread(() -> {
			try {
				connection = PeerConnection.connect(ip.trim(), PORT, this::onMessage);
				SwingUtilities.invokeLater(() -> onConnected("방에 들어갔습니다"));
			} catch (IOException e) {
				SwingUtilities.invokeLater(() -> onFailed("접속 실패: " + e.getMessage()));
			}
		}).start();
	}

	private void onConnected(String text) {
		status = text + " - 연결 완료!";
		connection.send("안녕! 연결 확인");
		repaint();
		frame.showScreen(MainFrame.VERSUS);
	}

	private void onFailed(String text) {
		busy = false;
		status = text;
		repaint();
	}

	private void onMessage(Object message) {
		SwingUtilities.invokeLater(() -> {
			status = "받은 메시지: " + message;
			repaint();
		});
	}
}
