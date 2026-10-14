package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 레벨 클리어 안내. Figma "레벨 클리어 화면".
 *
 * GAME OVER 덮개와 같은 카드 규격이고, 버튼만 '다음 레벨 / 메인으로' 다.
 * ←→ 로 고르고 Enter 로 선택한다.
 */
class LevelClearOverlay extends DesignPanel {

	private static final Rectangle CARD = new Rectangle(485, 452, 1029, 519);

	private static final int TITLE_TOP = 530, SCORE_TOP = 689, COIN_GAP = 56;
	private static final float TITLE_SIZE = 96f, TEXT_SIZE = 36f;
	private static final double TRACKING = 0.1;

	private static final int BUTTON_W = 280, BUTTON_H = 95, BUTTON_GAP = 61;
	private static final int BUTTON_TOP = 818, BUTTON_BORDER = 7;

	private static final int ARROW_W = 21, ARROW_H = 39, ARROW_GAP = 24;

	private final String title, scoreText, coinText;
	private final Runnable onNext, onMenu;
	private int selected = 0;

	LevelClearOverlay(int level, int score, int coins, Runnable onNext, Runnable onMenu) {
		this.title = "LEVEL " + level + " CLEAR!";
		this.scoreText = String.format("Score  %06d", score);
		this.coinText = coins > 0 ? "코인  +" + coins : null;
		this.onNext = onNext;
		this.onMenu = onMenu;
		setOpaque(false);   // 뒤 게임 화면이 비쳐야 한다

		Ui.bind(this, KeyEvent.VK_LEFT, "clearLeft", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_RIGHT, "clearRight", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "clearEnter", this::choose);
		onClick(button(0), () -> { selected = 0; choose(); });
		onClick(button(1), () -> { selected = 1; choose(); });
	}

	private void move(int d) {
		selected = (selected + d + 2) % 2;
		Sfx.play(Sfx.Sound.MOVE);
		repaint();
	}

	private void choose() {
		Sfx.play(Sfx.Sound.ROTATE);
		(selected == 0 ? onNext : onMenu).run();
	}

	/** 두 버튼을 한 덩어리로 보고 카드 가운데에 놓는다. */
	private Rectangle button(int index) {
		int total = BUTTON_W * 2 + BUTTON_GAP;
		int x = CARD.x + CARD.width / 2 - total / 2 + index * (BUTTON_W + BUTTON_GAP);
		return new Rectangle(x, BUTTON_TOP, BUTTON_W, BUTTON_H);
	}

	@Override
	void paintDesign(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		fill(g, CARD, Ui.CREAM);
		centeredOnCard(g, title, TITLE_TOP, TITLE_SIZE);
		centeredOnCard(g, scoreText, SCORE_TOP, TEXT_SIZE);
		if (coinText != null)
			centeredOnCard(g, coinText, SCORE_TOP + COIN_GAP, TEXT_SIZE);

		drawButton(g, button(0), "다음 레벨", selected == 0);
		drawButton(g, button(1), "메인으로", selected == 1);
	}

	private void centeredOnCard(Graphics2D g, String s, int designTop, float size) {
		g.setColor(Color.BLACK);
		g.setFont(font(size, TRACKING));
		int x = px(CARD.x + CARD.width / 2) - g.getFontMetrics().stringWidth(s) / 2;
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

		if (!on || ARROW_ICON == null)
			return;

		int arrowW = sz(ARROW_W), arrowH = sz(ARROW_H);
		int arrowY = py(r.y) + (sz(r.height) - arrowH) / 2;
		g.drawImage(ARROW_ICON, textX - sz(ARROW_GAP) - arrowW, arrowY, arrowW, arrowH, this);
	}
}
