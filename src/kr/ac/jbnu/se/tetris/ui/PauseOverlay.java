package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 게임 중 톱니를 눌렀을 때 덮이는 화면. Figma "리팩 - 일시 정지".
 *
 * 게임 위에 반투명하게 덮인다. 마우스로 누르거나, ↑↓ 로 고르고 Enter 로 선택한다.
 *
 * Esc 는 게임 화면 쪽에만 걸어 둔다. 여기에도 걸면 한 번 누를 때 둘 다 반응해서
 * 어느 쪽이 먼저 도느냐에 따라 결과가 달라진다.
 */
class PauseOverlay extends DesignPanel {

	// Figma "리팩 - 일시 정지" 수치 (디자인 기준 픽셀)
	private static final Rectangle CARD = new Rectangle(627, 255, 746, 912);
	private static final int TITLE_TOP = 347;
	private static final float TITLE_SIZE = 75f;

	private static final int ITEM_X = 970, ITEM_TOP = 653, ITEM_STEP = 85, ITEM_H = 70;
	private static final float ITEM_SIZE = 48f;
	private static final int ARROW_RIGHT = 903, ARROW_SIZE = 50;

	private static final String[] ITEMS = { "계속하기", "다시하기", "나가기", "설정으로" };

	/** 카드 아래 조작키 안내 (디자인에 없지만 있던 기능이라 남긴다) */
	private static final int KEYS_TOP = 1200, KEYS_STEP = 38, KEYS_PAD = 12;
	private static final float KEYS_SIZE = 24f;

	private final Runnable[] actions;
	private final String[] keyLines;
	private int selected = 0;

	PauseOverlay(Runnable onResume, Runnable onRetry, Runnable onExit, Runnable onSettings, String[] keyLines) {
		this.actions = new Runnable[] { onResume, onRetry, onExit, onSettings };
		this.keyLines = keyLines;
		setOpaque(false);   // 뒤 게임 화면이 비쳐야 한다

		Ui.bind(this, KeyEvent.VK_UP, "pauseUp", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_DOWN, "pauseDown", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "pauseEnter", () -> actions[selected].run());

		for (int i = 0; i < ITEMS.length; i++)
			onClick(row(i), actions[i]);
	}

	private void move(int d) {
		selected = (selected + d + ITEMS.length) % ITEMS.length;
		Sfx.play(Sfx.Sound.MOVE);
		repaint();
	}

	/** 눌러서 고를 수 있는 한 줄. 글자보다 넉넉하게 카드 폭을 쓴다. */
	private Rectangle row(int index) {
		return new Rectangle(CARD.x, ITEM_TOP + index * ITEM_STEP - 8, CARD.width, ITEM_H);
	}

	@Override
	void paintDesign(Graphics2D g) {
		scrim(g);
		darkCard(g, CARD);
		textCentered(g, "일시 정지", TITLE_TOP, TITLE_SIZE, Color.WHITE);

		for (int i = 0; i < ITEMS.length; i++)
			text(g, ITEMS[i], ITEM_X, ITEM_TOP + i * ITEM_STEP, ITEM_SIZE, Color.WHITE);
		arrow(g, ARROW_RIGHT, ITEM_TOP + selected * ITEM_STEP + 32, ARROW_SIZE);

		// 뒤 화면과 섞이지 않게 어두운 띠를 깔고 조작키를 적는다
		g.setColor(new Color(58, 57, 52, 220));
		Rectangle band = screen(new Rectangle(0, KEYS_TOP - KEYS_PAD, DESIGN_W,
				keyLines.length * KEYS_STEP + KEYS_PAD * 2));
		g.fillRect(0, band.y, getWidth(), band.height);
		for (int i = 0; i < keyLines.length; i++)
			textCentered(g, keyLines[i], KEYS_TOP + i * KEYS_STEP, KEYS_SIZE, Color.WHITE);
	}
}
