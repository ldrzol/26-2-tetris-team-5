package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 게임 종료 안내. Figma "Desktop - 10" 의 덮개 부분.
 *
 * 게임 화면 위에 반투명하게 덮인다. ←→(↑↓) 로 고르고 Enter 로 선택한다.
 * 글자와 버튼은 디자인의 고정 x 좌표 대신 카드 한가운데를 기준으로 맞춘다.
 * 디자인과 폰트가 달라 글자 폭이 달라지므로, 좌표를 베끼면 가운데가 틀어진다.
 *
 * 대전 결과는 가운데 큰 카드가 두 보드를 가리지 않도록, 보드마다 결과 띠를 얹고
 * 버튼은 두 보드 사이 가운데 줄에 세로로 놓는다.
 */
class GameOverOverlay extends DesignPanel {

	private static final Rectangle CARD = new Rectangle(485, 452, 1029, 519);

	private static final int TITLE_TOP = 541, SCORE_TOP = 689;
	private static final int COIN_GAP = 56;   // 점수 아래 코인 줄
	private static final float TITLE_SIZE = 96f, TEXT_SIZE = 36f;
	private static final double TRACKING = 0.1;
	private static final String SCORE_FORMAT = "Score  %06d";

	private static final int BUTTON_W = 280, BUTTON_H = 95, BUTTON_GAP = 61;
	private static final int BUTTON_TOP = 818;   // 디자인은 788. 조금 내렸다
	private static final int BUTTON_BORDER = 7;

	private static final int ARROW_W = 21, ARROW_H = 39;
	private static final int ARROW_GAP = 24;     // 화살표와 글자 사이

	// 대전 결과: 보드 자리는 VersusPanel 의 BOARD_1 / BOARD_2 와 같다
	private static final Rectangle[] SIDE_BOARDS = {
			new Rectangle(268, 239, 550, 1100), new Rectangle(1183, 239, 550, 1100) };
	private static final int BAND_TOP = 560;
	private static final int BAND_H = 300;
	private static final int SIDE_TITLE_TOP = 590;
	private static final int SIDE_SCORE_TOP = 720;
	private static final Rectangle SIDE_CARD = new Rectangle(852, 905, 296, 290);
	private static final int SIDE_BUTTON_W = 250;
	private static final int SIDE_BUTTON_TOP = 935;
	private static final int SIDE_BUTTON_STEP = 130;

	private final String scoreText, coinText, title;
	private final Runnable onRetry, onMenu;

	/** 대전 결과일 때만 있다. [0] = 왼쪽 보드, [1] = 오른쪽 보드. */
	private final String[] sideTitles;
	private final String[] sideScores;
	private int selected = 0;

	GameOverOverlay(int score, int coins, Runnable onRetry, Runnable onMenu) {
		this(Wallet.cleared(score) ? "CLEAR!" : "GAME OVER", score, coins, onRetry, onMenu);
	}

	GameOverOverlay(String title, int score, int coins, Runnable onRetry, Runnable onMenu) {
		this(title, String.format(SCORE_FORMAT, score), coins, null, null, onRetry, onMenu);
	}

	/** 대전 결과. titles 예) "1P WIN" / "2P LOSE". coins 는 왼쪽(나) 정산 코인. */
	GameOverOverlay(String[] titles, int[] scores, int coins, Runnable onRetry, Runnable onMenu) {
		this(null, null, coins, titles, new String[] {
				String.format(SCORE_FORMAT, scores[0]), String.format(SCORE_FORMAT, scores[1]) },
				onRetry, onMenu);
	}

	private GameOverOverlay(String title, String scoreText, int coins, String[] sideTitles,
			String[] sideScores, Runnable onRetry, Runnable onMenu) {
		this.title = title;
		this.scoreText = scoreText;
		this.coinText = coins > 0 ? "코인  +" + coins : null;
		this.sideTitles = sideTitles;
		this.sideScores = sideScores;
		this.onRetry = onRetry;
		this.onMenu = onMenu;
		setOpaque(false);   // 뒤 게임 화면이 비쳐야 한다

		Ui.bind(this, KeyEvent.VK_LEFT, "overLeft", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_RIGHT, "overRight", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_UP, "overUp", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_DOWN, "overDown", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "overEnter", this::choose);
	}

	/** Enter: 고른 버튼을 누른다. */
	private void choose() {
		(selected == 0 ? onRetry : onMenu).run();
	}

	private boolean versus() {
		return sideTitles != null;
	}

	private void move(int d) {
		selected = (selected + d + 2) % 2;
		repaint();
	}

	/** 두 버튼을 한 덩어리로 보고 카드 가운데에 놓는다. 대전 결과는 가운데 줄에 세로로. */
	private Rectangle button(int index) {
		if (versus())
			return new Rectangle(DESIGN_W / 2 - SIDE_BUTTON_W / 2, SIDE_BUTTON_TOP + index * SIDE_BUTTON_STEP,
					SIDE_BUTTON_W, BUTTON_H);
		int total = BUTTON_W * 2 + BUTTON_GAP;
		int x = cardCenterX() - total / 2 + index * (BUTTON_W + BUTTON_GAP);
		return new Rectangle(x, BUTTON_TOP, BUTTON_W, BUTTON_H);
	}

	private int cardCenterX() {
		return CARD.x + CARD.width / 2;
	}

	@Override
	void paintDesign(Graphics2D g) {
		if (versus())
			paintVersus(g);
		else
			paintSingle(g);

		drawButton(g, button(0), "다시 하기", selected == 0);
		drawButton(g, button(1), "메인으로", selected == 1);
	}

	private void paintSingle(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		fill(g, CARD, Ui.CREAM);
		centered(g, title, cardCenterX(), TITLE_TOP, TITLE_SIZE, Color.BLACK);
		centered(g, scoreText, cardCenterX(), SCORE_TOP, TEXT_SIZE, Color.BLACK);
		if (coinText != null)
			centered(g, coinText, cardCenterX(), SCORE_TOP + COIN_GAP, TEXT_SIZE, Color.BLACK);
	}

	/** 두 보드는 그대로 보이게 두고, 보드 가운데에 결과 띠만 얹는다. */
	private void paintVersus(Graphics2D g) {
		for (int i = 0; i < SIDE_BOARDS.length; i++) {
			Rectangle board = SIDE_BOARDS[i];
			int cx = board.x + board.width / 2;
			fill(g, new Rectangle(board.x, BAND_TOP, board.width, BAND_H), SCRIM);
			Color color = sideTitles[i].endsWith("LOSE") ? DIM_CREAM : Ui.CREAM;
			centered(g, sideTitles[i], cx, SIDE_TITLE_TOP, TITLE_SIZE, color);
			centered(g, sideScores[i], cx, SIDE_SCORE_TOP, TEXT_SIZE, Ui.CREAM);
			if (i == 0 && coinText != null)
				centered(g, coinText, cx, SIDE_SCORE_TOP + COIN_GAP, TEXT_SIZE, Ui.CREAM);
		}
		fill(g, SIDE_CARD, Ui.CREAM);
	}

	private void centered(Graphics2D g, String s, int designCenterX, int designTop, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size, TRACKING));
		int x = px(designCenterX) - g.getFontMetrics().stringWidth(s) / 2;
		g.drawString(s, x, py(designTop) + g.getFontMetrics().getAscent());
	}

	/** 고른 버튼은 검게 채우고 글자를 크림색으로, 나머지는 테두리만 그린다. */
	private void drawButton(Graphics2D g, Rectangle r, String label, boolean on) {
		if (on)
			fill(g, r, Color.BLACK);
		else
			outline(g, r, Color.BLACK, BUTTON_BORDER);

		g.setColor(on ? Ui.CREAM : Color.BLACK);
		g.setFont(font(TEXT_SIZE, TRACKING));
		int textW = g.getFontMetrics().stringWidth(label);
		int textX = px(r.x) + (sz(r.width) - textW) / 2;
		g.drawString(label, textX, centeredBaseline(g, r.y, r.height));

		if (!on)
			return;

		// 화살표는 글자 왼쪽에, 버튼 세로 가운데에 놓는다
		int arrowW = sz(ARROW_W), arrowH = sz(ARROW_H);
		int arrowY = py(r.y) + (sz(r.height) - arrowH) / 2;
		if (ARROW_ICON != null)
			g.drawImage(ARROW_ICON, textX - sz(ARROW_GAP) - arrowW, arrowY, arrowW, arrowH, this);
	}
}
