package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.network.LobbyService;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.save.Wallet;
import kr.ac.jbnu.se.tetris.network.GameMode;


/**
 * 2인 대전 로비. Figma "Desktop - 12".
 *
 * 메뉴 모양은 다른 메뉴 화면과 같고, 두 항목이 방 만들기 / 방 참가다.
 * 연결 진행 상황은 디자인에 없지만 없으면 쓸 수 없으므로 상자 아래에 한 줄로 띄운다.
 *
 * 연결, 신호, 참가비 정산은 LobbyService 가 하고, 이 클래스는 화면만 맡는다.
 */
class LobbyFrame extends MenuScreen implements LobbyService.Listener {


	private final MainFrame frame;
	private final transient LobbyService service = new LobbyService(this);

	private String status = null;
	private boolean busy = false;

	LobbyFrame(MainFrame frame) {
		super("네트워크 대전", new String[] { "방 생성하기", "방 참가하기" }, null,
				() -> frame.showScreen(MainFrame.MULTI), true);
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

	/** 모드 -> 참가비 -> 방 열기. 덮개는 모달이 아니라서 한 단계씩 이어 간다. */
	private void createRoom() {
		frame.showChoice("게임 모드를 고르세요", GameMode.CLASSIC.label(), GameMode.SHARED.label(), choice -> {
			if (choice == null)
				return;
			askFee(choice.equals(GameMode.SHARED.label()) ? GameMode.SHARED : GameMode.CLASSIC);
		});
	}

	/** 참가비를 묻고, 걸 수 있는 금액이면 방을 연다. */
	private void askFee(GameMode gameMode) {
		frame.showInput("참가비 (" + LobbyService.MIN_FEE + " 이상, 내 코인 " + Wallet.coins() + ")", "", input -> {
			if (input == null)
				return;
			int fee;
			try {
				fee = Integer.parseInt(input.trim());
			} catch (NumberFormatException e) {
				fail("참가비는 숫자로 입력하세요");
				return;
			}
			if (!service.isValidFee(fee)) {
				fail("참가비는 " + LobbyService.MIN_FEE + " 이상, 내 코인 이하여야 합니다");
				return;
			}
			openRoom(gameMode, fee);
		});
	}

	private void openRoom(GameMode gameMode, int fee) {
		busy = true;
		status = waitingMessage();
		repaint();
		service.openRoom(gameMode, fee);
	}

	private void fail(String message) {
		status = message;
		repaint();
	}

	/** 내 IP 를 알 수 있으면 같이 알려 준다. 못 찾아도 방은 열린다. */
	private String waitingMessage() {
		String ip = service.localAddress();
		if (ip == null)
			return "내 IP를 알 수 없음. 상대를 기다리는 중...";
		return "내 IP: " + ip + " (포트 " + LobbyService.PORT + ") - 상대를 기다리는 중...";
	}

	private void joinRoom() {
		frame.showInput("방장의 IP를 입력하세요", "", ip -> {
			if (ip != null && !ip.trim().isEmpty())
				connect(ip.trim());
		});
	}

	private void connect(String ip) {
		busy = true;
		status = ip + " 에 접속하는 중...";
		repaint();
		service.joinRoom(ip);
	}

	// LobbyService 가 알려 주는 일

	@Override
	public void onStatus(String text) {
		status = text;
		repaint();
	}

	@Override
	public void onFailed(String text) {
		busy = false;
		status = text;
		repaint();
	}

	@Override
	public void onStart(PeerConnection connection, boolean host, GameMode gameMode, int fee) {
		if (gameMode == GameMode.SHARED)
			frame.showShared(connection, host, fee);
		else
			frame.showVersus(connection, host, fee);
	}
}