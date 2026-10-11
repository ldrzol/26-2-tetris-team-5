package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 게임 종료 안내. Figma "Desktop - 10" 의 덮개 부분.
 *
 * 게임 화면 위에 반투명하게 덮인다. ←→ 로 고르고 Enter 로 선택한다.
 * 글자와 버튼은 디자인의 고정 x 좌표 대신 카드 한가운데를 기준으로 맞춘다.
 * 디자인과 폰트가 달라 글자 폭이 달라지므로, 좌표를 베끼면 가운데가 틀어진다.
 */
class GameOverOverlay extends DesignPanel {

	private static final Rectangle CARD = new Rectangle(485, 452, 1029, 519);

	private static final int TITLE_TOP = 541, SCORE_TOP = 689;
	private static final float TITLE_SIZE = 96f, TEXT_SIZE = 36f;
	private static final double TRACKING = 0.1;

	private static final int BUTTON_W = 280, BUTTON_H = 95, BUTTON_GAP = 61;
	private static final int BUTTON_TOP = 818;   // 디자인은 788. 조금 내렸다
	private static final int BUTTON_BORDER = 7;

	private static final int ARROW_W = 21, ARROW_H = 39;
	private static final int ARROW_GAP = 24;     // 화살표와 글자 사이

	private final String scoreText;
	private final Runnable onRetry, onMenu;
	private int selected = 0;

	GameOverOverlay(int score, Runnable onRetry, Runnable onMenu) {
		this.scoreText = String.format("Score  %06d", score);
		this.onRetry = onRetry;
		this.onMenu = onMenu;
		setOpaque(false);   // 뒤 게임 화면이 비쳐야 한다

		Ui.bind(this, KeyEvent.VK_LEFT, "overLeft", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_RIGHT, "overRight", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "overEnter",
				() -> (selected == 0 ? onRetry : onMenu).run());
	}

	private void move(int d) {
		selected = (selected + d + 2) % 2;
		repaint();
	}

	/** 두 버튼을 한 덩어리로 보고 카드 가운데에 놓는다. */
	private Rectangle button(int index) {
		int total = BUTTON_W * 2 + BUTTON_GAP;
		int x = cardCenterX() - total / 2 + index * (BUTTON_W + BUTTON_GAP);
		return new Rectangle(x, BUTTON_TOP, BUTTON_W, BUTTON_H);
	}

	private int cardCenterX() {
		return CARD.x + CARD.width / 2;
	}

	@Override
	void paintDesign(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		fill(g, CARD, Ui.CREAM);
		centeredOnCard(g, "GAME OVER", TITLE_TOP, TITLE_SIZE, Color.BLACK);
		centeredOnCard(g, scoreText, SCORE_TOP, TEXT_SIZE, Color.BLACK);

		drawButton(g, button(0), "다시 하기", selected == 0);
		drawButton(g, button(1), "메인으로", selected == 1);
	}

	private void centeredOnCard(Graphics2D g, String s, int designTop, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size, TRACKING));
		int x = px(cardCenterX()) - g.getFontMetrics().stringWidth(s) / 2;
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
