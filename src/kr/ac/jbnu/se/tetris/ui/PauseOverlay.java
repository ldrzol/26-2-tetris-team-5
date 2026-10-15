package kr.ac.jbnu.se.tetris.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 게임 중 톱니를 눌렀을 때 덮이는 화면. Figma "리플레이 화면".
 *
 * 게임 위에 반투명하게 덮인다. 마우스로 누르거나, ↑↓ 로 고르고 Enter 로 선택한다.
 *
 * Esc 는 게임 화면 쪽에만 걸어 둔다. 여기에도 걸면 한 번 누를 때 둘 다 반응해서
 * 어느 쪽이 먼저 도느냐에 따라 결과가 달라진다.
 */
class PauseOverlay extends DesignPanel {

	// Figma "리플레이 화면" 수치 (디자인 기준 픽셀)
	private static final Rectangle BOX_OUTER = new Rectangle(689, 333, 622, 757);
	private static final Rectangle BOX_INNER = new Rectangle(697, 342, 606, 740);
	private static final int BOX_BORDER = 8;

	private static final int ITEM_X = 962;
	private static final int ITEM_TOP = 511;
	private static final int ITEM_GAP = 110;
	private static final int ITEM_HEIGHT = 70;
	private static final float ITEM_SIZE = 64f;

	private static final Rectangle ARROW = new Rectangle(836, 511, 29, 54);

	private static final String[] ITEMS = { "계속하기", "다시하기", "나가기", "환경설정" };

	private final Runnable[] actions;
	private int selected = 0;

	PauseOverlay(Runnable onResume, Runnable onRetry, Runnable onExit, Runnable onSettings) {
		this.actions = new Runnable[] { onResume, onRetry, onExit, onSettings };
		setOpaque(false);   // 뒤 게임 화면이 비쳐야 한다

		Ui.bind(this, KeyEvent.VK_UP, "pauseUp", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_DOWN, "pauseDown", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "pauseEnter", () -> actions[selected].run());

		for (int i = 0; i < ITEMS.length; i++)
			onClick(row(i), actions[i]);
	}

	private void move(int d) {
		selected = (selected + d + ITEMS.length) % ITEMS.length;
		repaint();
	}

	/** 눌러서 고를 수 있는 한 줄. 글자보다 넉넉하게 상자 폭을 쓴다. */
	private Rectangle row(int index) {
		return new Rectangle(BOX_INNER.x, ITEM_TOP + index * ITEM_GAP, BOX_INNER.width, ITEM_HEIGHT);
	}

	@Override
	void paintDesign(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		// 바깥 흐린 테두리 + 안쪽 크림 테두리 (두 겹)
		outline(g, BOX_OUTER, DIM_CREAM, BOX_BORDER);
		outline(g, BOX_INNER, Ui.CREAM, BOX_BORDER);

		for (int i = 0; i < ITEMS.length; i++)
			text(g, ITEMS[i], ITEM_X, ITEM_TOP + i * ITEM_GAP, ITEM_SIZE, Color.WHITE);

		drawImage(g, ARROW_ICON,
				new Rectangle(ARROW.x, ARROW.y + selected * ITEM_GAP, ARROW.width, ARROW.height));
	}
}
