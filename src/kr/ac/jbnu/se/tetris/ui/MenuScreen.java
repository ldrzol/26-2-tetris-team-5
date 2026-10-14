package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;

/**
 * 메뉴 화면 공통. Figma "Desktop - 5 / 13 / 12" 가 전부 같은 모양이고
 * 제목과 두 항목만 다르다.
 *
 * ↑↓ 로 고르고 Enter 로 선택한다.
 */
class MenuScreen extends DesignPanel {

	private static final Image BACKGROUND = image("images/menu_bg.png");

	// Figma 수치 (디자인 기준 픽셀)
	private static final Rectangle BOX_OUTER = new Rectangle(745, 657, 510, 320);
	private static final Rectangle BOX_INNER = new Rectangle(751, 665, 497, 305);
	private static final int BOX_BORDER = 8;

	private static final int ITEM_X = 932;
	private static final int ITEM_TOP = 727;
	private static final int ITEM_GAP = 111;   // 첫째 항목 -> 둘째 항목
	private static final float ITEM_SIZE = 64f;

	private static final Rectangle ARROW = new Rectangle(837, 735, 29, 54);

	private static final int TITLE_LINE_GAP = 161;
	private static final double TITLE_TRACKING = 0.15;

	/** 디자인에 없는 안내문 자리. 로비의 연결 상태처럼 꼭 필요한 경우에만 쓴다. */
	private static final int STATUS_TOP = 1030;
	private static final float STATUS_SIZE = 36f;

	private final String[] titleLines;
	private final float[] titleSizes;
	private final int titleTop;
	private final String[] items;
	private final IntConsumer onSelect;

	private int selected = 0;

	MenuScreen(String[] titleLines, float[] titleSizes, int titleTop, String[] items, IntConsumer onSelect) {
		this.titleLines = titleLines;
		this.titleSizes = titleSizes;
		this.titleTop = titleTop;
		this.items = items;
		this.onSelect = onSelect;

		Ui.bind(this, KeyEvent.VK_UP, "menuUp", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_DOWN, "menuDown", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "menuEnter", this::choose);
	}

	private void move(int d) {
		selected = (selected + d + items.length) % items.length;
		Sfx.play(Sfx.Sound.MOVE);
		repaint();
	}

	private void choose() {
		if (isSelectable(selected)) {
			Sfx.play(Sfx.Sound.ROTATE);
			select(selected);
		}
	}

	/** 항목을 골랐을 때. 기본은 생성자에 넘긴 동작, 자식 화면은 덮어써도 된다. */
	void select(int index) {
		if (onSelect != null)
			onSelect.accept(index);
	}

	/** 항목을 지금 고를 수 있는지. (로비에서 접속 중일 때 막는 용도) */
	boolean isSelectable(int index) {
		return true;
	}

	/** 상자 아래에 띄울 안내문. 없으면 null. */
	String status() {
		return null;
	}

	/** Figma 상자는 항목 두 개 기준이다. 항목이 늘면 그만큼 아래로 늘린다. */
	private Rectangle grown(Rectangle box) {
		return new Rectangle(box.x, box.y, box.width, box.height + (items.length - 2) * ITEM_GAP);
	}

	@Override
	void paintDesign(Graphics2D g) {
		background(g, BACKGROUND);

		int top = titleTop;
		for (int i = 0; i < titleLines.length; i++) {
			gradientTextCentered(g, titleLines[i], top, titleSizes[i], TITLE_TRACKING);
			top += TITLE_LINE_GAP;
		}

		// 바깥 흐린 테두리 + 안쪽 크림 테두리 (두 겹)
		outline(g, grown(BOX_OUTER), DIM_CREAM, BOX_BORDER);
		outline(g, grown(BOX_INNER), Ui.CREAM, BOX_BORDER);

		for (int i = 0; i < items.length; i++) {
			Color color = isSelectable(i) ? Color.WHITE : DIM_CREAM;
			text(g, items[i], ITEM_X, ITEM_TOP + i * ITEM_GAP, ITEM_SIZE, color);
		}
		drawImage(g, ARROW_ICON,
				new Rectangle(ARROW.x, ARROW.y + selected * ITEM_GAP, ARROW.width, ARROW.height));

		String status = status();
		if (status != null)
			textCentered(g, status, STATUS_TOP, STATUS_SIZE, Ui.CREAM, 0);

		gear(g);
	}
}
