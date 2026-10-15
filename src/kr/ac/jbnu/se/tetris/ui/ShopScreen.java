package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;
import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.item.Box;
import kr.ac.jbnu.se.tetris.item.Skin;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 상점 화면. 왼쪽은 블록 스킨, 오른쪽은 랜덤 아이템 박스다.
 *
 * ←→ 로 칸을 옮기고 ↑↓ 로 고른 뒤 Enter 로 사거나 장착한다.
 * 산 상자는 그 자리에서 열려서 아이템이 가방에 들어간다.
 */
class ShopScreen extends DesignPanel {

	private static final Image COIN_ICON = image("images/coin.png");
	private static final Map<Box, Image> BOX_ICONS = new EnumMap<>(Box.class);

	static {
		for (Box box : Box.values())
			BOX_ICONS.put(box, image(box.icon));
	}

	// 디자인 기준 픽셀
	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;

	private static final Rectangle BACK = new Rectangle(73, 66, 232, 81);
	private static final Rectangle BACK_ARROW = new Rectangle(132, 88, 20, 37);
	private static final int BACK_TEXT_X = 179, BACK_TEXT_TOP = 79;

	private static final Rectangle TOP_COIN = new Rectangle(1546, 81, 51, 51);
	private static final int TOP_COIN_TEXT_X = 1629, TOP_COIN_TEXT_TOP = 78;

	private static final Rectangle SKIN_TAB = new Rectangle(140, 240, 260, 62);
	private static final Rectangle SKIN_BOX = new Rectangle(140, 302, 800, 560);
	private static final Rectangle BOX_TAB = new Rectangle(1060, 240, 300, 62);
	private static final Rectangle BOX_BOX = new Rectangle(1060, 302, 800, 560);

	private static final Rectangle BAG_TAB = new Rectangle(140, 900, 300, 62);
	private static final Rectangle BAG_BOX = new Rectangle(140, 962, 1720, 300);

	// 보유 아이템은 종류가 많아서 격자로 늘어놓는다
	private static final int BAG_COLS = 6;
	private static final int BAG_CELL_W = 270, BAG_CELL_H = 90;
	private static final int BAG_ICON = 48, BAG_TOP = 20;
	private static final float BAG_SIZE = 28f;

	private static final int ROW_TOP = 40, ROW_H = 110;   // 상자 안 첫 줄 위치와 줄 간격
	private static final int PAD = 50;                    // 상자 안쪽 좌우 여백
	private static final int ICON = 70, ICON_DROP = 20;   // 상자 그림 크기
	private static final Rectangle ARROW = new Rectangle(0, 0, 20, 37);

	private static final float TAB_SIZE = 36f, ROW_SIZE = 40f, SMALL_SIZE = 30f, STATUS_SIZE = 32f;
	private static final int STATUS_TOP = 1300, HINT_TOP = 1360;

	/** 각 칸의 줄 수. 0 = 스킨, 1 = 상자 */
	private static final int[] ROWS = { Skin.values().length, Box.values().length };

	private int col = 0, row = 0;
	private String status = "";

	ShopScreen(Runnable onBack) {
		onClick(BACK, onBack);

		// 마우스로도 고를 수 있다. 누른 줄로 커서를 옮기고 그대로 선택한다.
		for (int i = 0; i < Skin.values().length; i++)
			pickOnClick(SKIN_BOX, 0, i);
		for (int i = 0; i < Box.values().length; i++)
			pickOnClick(BOX_BOX, 1, i);

		Ui.bind(this, KeyEvent.VK_UP, "shopUp", () -> select(0, -1));
		Ui.bind(this, KeyEvent.VK_DOWN, "shopDown", () -> select(0, +1));
		Ui.bind(this, KeyEvent.VK_LEFT, "shopLeft", () -> select(-1, 0));
		Ui.bind(this, KeyEvent.VK_RIGHT, "shopRight", () -> select(+1, 0));
		Ui.bind(this, KeyEvent.VK_ENTER, "shopEnter", this::choose);
		Ui.bind(this, KeyEvent.VK_ESCAPE, "shopBack", onBack);
	}

	private void pickOnClick(Rectangle box, int c, int r) {
		onClick(rowBounds(box, r), () -> {
			col = c;
			row = r;
			choose();
		});
	}

	private void select(int dc, int dr) {
		col = (col + dc + ROWS.length) % ROWS.length;
		row = Math.min(Math.max(row + dr, 0), ROWS[col] - 1);
		Sfx.play(Sfx.Sound.MOVE);
		repaint();
	}

	/** 고른 줄을 사거나 장착한다. 결과는 아래 안내문에 적는다. */
	private void choose() {
		if (col == 0) {
			Skin skin = Skin.values()[row];
			if (Wallet.owns(skin)) {
				Wallet.equip(skin);
				status = skin.label + " 를 장착했습니다";
				Sfx.play(Sfx.Sound.ROTATE);
			} else if (Wallet.buy(skin)) {
				status = skin.label + " 를 구매해 장착했습니다";
				Sfx.play(Sfx.Sound.COIN);
			} else {
				status = shortOf(skin.price);
				Sfx.play(Sfx.Sound.LOCK);
			}
		} else {
			Box box = Box.values()[row];
			List<Attack> got = Wallet.buy(box);
			status = got.isEmpty() ? shortOf(box.price)
					: box.label + " 에서 " + names(got) + " 을(를) 얻었습니다!";
			Sfx.play(got.isEmpty() ? Sfx.Sound.LOCK : Sfx.Sound.COIN);
		}
		repaint();
	}

	/** 얼마가 모자라는지 적어 준다. 그냥 '부족합니다' 보다 알아보기 쉽다. */
	private String shortOf(int price) {
		return "코인이 " + (price - Wallet.coins()) + " 부족합니다 (" + price + " 필요)";
	}

	private String names(List<Attack> items) {
		StringBuilder out = new StringBuilder();
		for (Attack a : items)
			out.append(out.length() == 0 ? "" : ", ").append(a.label);
		return out.toString();
	}

	// 그리기
	@Override
	void paintDesign(Graphics2D g) {
		textCenteredIn(g, "상점", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);

		drawImageFlipped(g, ARROW_ICON, BACK_ARROW);
		text(g, "뒤로", BACK_TEXT_X, BACK_TEXT_TOP, ROW_SIZE, Ui.CREAM);

		drawImage(g, COIN_ICON, TOP_COIN);
		text(g, Wallet.coinText(), TOP_COIN_TEXT_X, TOP_COIN_TEXT_TOP, ROW_SIZE, Ui.CREAM);

		tab(g, SKIN_TAB, "블록 스킨", TAB_SIZE);
		outline(g, SKIN_BOX);
		for (int i = 0; i < Skin.values().length; i++)
			skinRow(g, i);

		tab(g, BOX_TAB, "아이템 박스", TAB_SIZE);
		outline(g, BOX_BOX);
		for (int i = 0; i < Box.values().length; i++)
			boxRow(g, i);

		tab(g, BAG_TAB, "보유 아이템", TAB_SIZE);
		outline(g, BAG_BOX);
		bag(g);

		textCentered(g, status, STATUS_TOP, STATUS_SIZE, Color.WHITE, 0);
		textCentered(g, "←→ 칸 옮기기   ↑↓ 고르기   Enter/클릭 선택   Esc 뒤로",
				HINT_TOP, SMALL_SIZE, DIM_CREAM, 0);

		gear(g);
	}

	private void skinRow(Graphics2D g, int i) {
		Skin skin = Skin.values()[i];
		Rectangle r = rowBounds(SKIN_BOX, i);
		boolean on = col == 0 && row == i;

		cursor(g, r, on);
		text(g, skin.label, r.x + PAD, r.y + 30, ROW_SIZE, on ? Color.WHITE : Ui.CREAM);
		swatch(g, skin, r);
		right(g, r, skin == Wallet.skin() ? "장착 중" : Wallet.owns(skin) ? "보유" : price(skin.price));
	}

	private void boxRow(Graphics2D g, int i) {
		Box box = Box.values()[i];
		Rectangle r = rowBounds(BOX_BOX, i);
		boolean on = col == 1 && row == i;

		cursor(g, r, on);
		drawImage(g, BOX_ICONS.get(box), new Rectangle(r.x + PAD, r.y + ICON_DROP, ICON, ICON));
		text(g, box.label, r.x + PAD + ICON + 30, r.y + 30, ROW_SIZE, on ? Color.WHITE : Ui.CREAM);
		right(g, r, price(box.price));
	}

	/** 스킨 색 일곱 개를 작은 네모로 늘어놓는다. 오른쪽 값과 겹치지 않게 가운데에 둔다. */
	private void swatch(Graphics2D g, Skin skin, Rectangle r) {
		int size = 26, gap = 6;
		int x = r.x + 290;
		for (Color c : skin.colors) {
			fill(g, new Rectangle(x, r.y + 34, size, size), c);
			x += size + gap;
		}
	}

	/** 아이템 전부를 격자로 늘어놓는다. 하나도 없는 것은 흐리게 적는다. */
	private void bag(Graphics2D g) {
		for (int i = 0; i < Attack.values().length; i++) {
			Attack item = Attack.values()[i];
			int x = BAG_BOX.x + PAD + (i % BAG_COLS) * BAG_CELL_W;
			int y = BAG_BOX.y + BAG_TOP + (i / BAG_COLS) * BAG_CELL_H;
			int count = Wallet.count(item);

			drawImage(g, image(item.icon), new Rectangle(x, y, BAG_ICON, BAG_ICON));
			text(g, item.label + " x" + count, x + BAG_ICON + 14, y + 10, BAG_SIZE,
					count > 0 ? Ui.CREAM : DIM_CREAM);
		}
	}

	/** 고른 줄 왼쪽에 화살표를 둔다. */
	private void cursor(Graphics2D g, Rectangle r, boolean on) {
		if (on)
			drawImage(g, ARROW_ICON, new Rectangle(r.x + 12, r.y + 32, ARROW.width, ARROW.height));
	}

	private void right(Graphics2D g, Rectangle r, String value) {
		valueRight(g, r, value, ROW_SIZE, PAD);
	}

	private String price(int coins) {
		return coins + " 코인";
	}

	private Rectangle rowBounds(Rectangle box, int i) {
		return new Rectangle(box.x, box.y + ROW_TOP + i * ROW_H, box.width, ROW_H);
	}
}
