package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.save.Wallet;
import kr.ac.jbnu.se.tetris.shared.SharedBoardView;
import kr.ac.jbnu.se.tetris.shared.SharedGame;
import kr.ac.jbnu.se.tetris.shared.SharedSession;
import kr.ac.jbnu.se.tetris.shared.SharedState;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 땅따먹기 화면. Figma "리팩 - 2인 모드(한 화면)". 넓은 판 하나를 둘이 같이 쓴다.
 *
 * 규칙과 통신은 SharedSession(방장/참가자)이 하고, 이 화면은 그리기와 키 입력만 맡는다.
 * 1P(방장) 블록은 흰 테두리, 2P(참가자) 블록은 검은 테두리다. Esc 로 나간다.
 */
class SharedScreen extends DesignPanel implements SharedSession.View {

	// Figma "리팩 - 2인 모드(한 화면)" 수치. 보드 상자는 14x22 판에 맞춰 높이만 줄였다.
	private static final int BOARD_X = 700, BOARD_W = 600, BOARD_CENTER_Y = 711;
	private static final Rectangle[] NAME_OVAL = {
			new Rectangle(313, 21, 220, 103), new Rectangle(1468, 22, 220, 103) };
	private static final int[] COLUMN_X = { 243, 1398 };
	private static final int NEXT_TOP = 164, NEXT_H = 263, HOLD_TOP = 494, HOLD_H = 297;
	private static final int SCORE_TOP = 859, SCORE_H = 452, PANEL_W = 359;

	private static final int STATUS_TOP = 140, STATUS_H = 80;

	private static final String LEAVE = "나가기";
	private static final String STAY = "계속하기";

	private final MainFrame frame;
	private final SharedBoardView view = new SharedBoardView();
	private final SharedSession session;
	private final int me;
	private final int fee;

	private SharedState state;
	private String status = "";

	SharedScreen(MainFrame frame, PeerConnection connection, boolean host, int fee) {
		this.frame = frame;
		this.me = host ? 0 : 1;
		this.fee = fee;
		view.setOpaque(false);   // 보드 상자의 하늘색이 비쳐야 한다
		add(view);

		// 예전 땅따먹기 창과 같은 키
		key(KeyEvent.VK_LEFT, "left", InputMessage.Action.LEFT);
		key(KeyEvent.VK_RIGHT, "right", InputMessage.Action.RIGHT);
		key(KeyEvent.VK_UP, "rotate", InputMessage.Action.ROTATE);
		key(KeyEvent.VK_Z, "rotateLeft", InputMessage.Action.ROTATE_LEFT);
		key(KeyEvent.VK_C, "hold", InputMessage.Action.HOLD);
		key(KeyEvent.VK_DOWN, "down", InputMessage.Action.DOWN);
		key(KeyEvent.VK_SPACE, "drop", InputMessage.Action.DROP);
		Ui.bind(this, KeyEvent.VK_ESCAPE, "leave", this::askLeave);
		onBack(this::askLeave);

		session = SharedSession.start(connection, host, fee, this);
	}

	private void key(int code, String name, InputMessage.Action action) {
		Ui.bind(this, code, "shared:" + name, () -> {
			if (isEnabled())     // 덮개가 떠 있으면 판을 건드리지 않는다
				session.onKey(action);
		});
	}

	/** 나갈지 묻는다. 나가면 연결을 닫는다. (예전에는 창을 닫으면 나갔다) */
	private void askLeave() {
		if (!isEnabled())
			return;
		frame.showChoice("게임을 그만두고 나갈까요?", LEAVE, STAY, answer -> {
			if (LEAVE.equals(answer)) {
				session.close();
				frame.showScreen(MainFrame.MENU);
			}
		});
	}

	// SharedSession.View

	@Override
	public void showState(SharedState s) {
		state = s;
		view.setState(s);
		repaint();
	}

	@Override
	public void showStatus(String text) {
		status = text;
		repaint();
	}

	@Override
	public void showResult(int winner, int me) {
		String note = Wallet.settle(winner, me, fee);
		String[] results = winner == -1 ? new String[] { "DRAW", "DRAW" }
				: winner == 0 ? new String[] { "WIN", "LOSE" } : new String[] { "LOSE", "WIN" };
		frame.showSharedResult(results, new int[] { score(0), score(1) }, note, session::close);
	}

	// 그리기

	/** 14 x 22 판이 들어가는 보드 상자. 가로는 디자인 그대로, 세로는 판 비율에 맞춘다. */
	private static Rectangle boardBox() {
		double cell = (BOARD_W - 30) / (double) SharedGame.WIDTH;
		int h = (int) Math.round(cell * SharedGame.HEIGHT) + 30;
		return new Rectangle(BOARD_X, BOARD_CENTER_Y - h / 2, BOARD_W, h);
	}

	@Override
	void layoutDesign() {
		Rectangle b = boardBox();
		b.grow(-15, -15);
		view.setBounds(fit(b, SharedGame.WIDTH, SharedGame.HEIGHT));   // 칸 크기의 배수로 꽉 맞춘다
	}

	private int score(int p) {
		return state == null ? 0 : state.getScore(p);
	}

	@Override
	void paintDesign(Graphics2D g) {
		background(g, SKY);
		boardFrame(g, view.getBounds(), Ui.BROWN);

		for (int p = 0; p < 2; p++) {
			int x = COLUMN_X[p];
			smallOval(g, NAME_OVAL[p], (p + 1) + "P", 64, false);
			if (p == me)
				textAt(g, "나", NAME_OVAL[p].x + NAME_OVAL[p].width + 24, NAME_OVAL[p].y + 36, 36, Ui.RED);

			Rectangle next = new Rectangle(x, NEXT_TOP, PANEL_W, NEXT_H);
			Rectangle hold = new Rectangle(x, HOLD_TOP, PANEL_W, HOLD_H);
			Rectangle score = new Rectangle(x, SCORE_TOP, PANEL_W, SCORE_H);
			awningPanel(g, next, "NEXT");
			awningPanel(g, hold, "HOLD");
			awningPanel(g, score, "SCORE");
			if (state != null) {
				preview(g, below(next, 16), state.nextType(p), p);
				preview(g, below(hold, 16), state.holdType(p), p);
			}
			textAt(g, String.format("%,d", score(p)), x + PANEL_W / 2.0, SCORE_TOP + 149, 52, Color.BLACK);
			text(g, "블록 테두리", x + 34, SCORE_TOP + 284, 36, Color.BLACK);
			textRight(g, p == 0 ? "흰색" : "검정", x + PANEL_W - 34, SCORE_TOP + 284, 36, Color.BLACK);
		}

		if (!status.isEmpty()) {
			int w = textWidth(g, status, 34) + 70;
			Rectangle r = new Rectangle(DESIGN_W / 2 - w / 2, STATUS_TOP, w, STATUS_H);
			box(g, r, 26, Ui.DARK_CARD, Ui.DARK_CARD_BORDER, 8, 7, Ui.SHADOW);
			textInBox(g, status, r, 34, Color.WHITE);
		}
		back(g);
	}
}
