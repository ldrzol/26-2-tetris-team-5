package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;
import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.item.Box;
import kr.ac.jbnu.se.tetris.item.Skin;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 상점 화면. Figma "리팩 - 상점 화면". 왼쪽은 블록 스킨, 오른쪽은 랜덤 아이템 상자다.
 *
 * ←→ 로 칸을 옮기고 ↑↓ 로 고른 뒤 Enter 로 사거나 장착한다. 줄을 눌러도 된다.
 * 산 상자는 그 자리에서 열려서 아이템이 가방에 들어간다.
 *
 * 상자·아이템 그림은 게임에서 쓰던 것을 그대로 쓴다. 보유 아이템은 디자인(4종)보다
 * 종류가 많아서 작은 카드 두 줄로 늘어놓는다.
 */
class ShopScreen extends DesignPanel {

	private static final Map<Box, Image> BOX_ICONS = new EnumMap<>(Box.class);

	static {
		for (Box box : Box.values())
			BOX_ICONS.put(box, image(box.icon));
	}

	private static final int LIFT = 59;   // 어닝이 상자보다 59 위에서 시작한다

	private static final Rectangle SKIN_PANEL = new Rectangle(58, 290, 901, 400);
	private static final Rectangle BOX_PANEL = new Rectangle(1041, 290, 901, 400);
	private static final Rectangle BAG_PANEL = new Rectangle(58, 790, 1884, 330);

	/** 상자 안 줄: 첫 줄 위치와 간격, 크기 */
	private static final int[] ROWS_X = { 90, 1073 };
	private static final int ROWS_TOP = 368, ROW_STEP = 100, ROW_W = 836, ROW_H = 90;

	// 보유 아이템 카드
	private static final int BAG_X = 90, BAG_TOP = 862, BAG_COLS = 8;
	private static final int CARD_W = 217, CARD_H = 100, CARD_GAP = 12;
	private static final Color CARD_FILL = new Color(0xFFFDF7);

	private static final int STATUS_TOP = 1150, STATUS_H = 90;
	private static final Rectangle GUIDE = new Rectangle(575, 1270, 850, 74);

	/** 각 칸의 줄 수. 0 = 스킨, 1 = 상자 */
	private static final int[] ROWS = { Skin.values().length, Box.values().length };

	private int col = 0, row = 0;
	private String status = "";

	ShopScreen(Runnable onBack) {
		onBack(onBack);

		// 마우스로도 고를 수 있다. 누른 줄로 커서를 옮기고 그대로 선택한다.
		for (int c = 0; c < ROWS.length; c++)
			for (int r = 0; r < ROWS[c]; r++)
				pickOnClick(c, r);

		Ui.bind(this, KeyEvent.VK_UP, "shopUp", () -> select(0, -1));
		Ui.bind(this, KeyEvent.VK_DOWN, "shopDown", () -> select(0, +1));
		Ui.bind(this, KeyEvent.VK_LEFT, "shopLeft", () -> select(-1, 0));
		Ui.bind(this, KeyEvent.VK_RIGHT, "shopRight", () -> select(+1, 0));
		Ui.bind(this, KeyEvent.VK_ENTER, "shopEnter", this::choose);
		Ui.bind(this, KeyEvent.VK_ESCAPE, "shopBack", onBack);
	}

	private void pickOnClick(int c, int r) {
		onClick(rowBounds(c, r), () -> {
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
				status = skin.label + " 를 장착했어요";
				Sfx.play(Sfx.Sound.ROTATE);
			} else if (Wallet.buy(skin)) {
				status = skin.label + " 를 사서 장착했어요";
				Sfx.play(Sfx.Sound.COIN);
			} else {
				status = shortOf(skin.price);
				Sfx.play(Sfx.Sound.LOCK);
			}
		} else {
			Box box = Box.values()[row];
			List<Attack> got = Wallet.buy(box);
			status = got.isEmpty() ? shortOf(box.price)
					: box.label + "에서 " + names(got) + " 을(를) 얻었어요!";
			Sfx.play(got.isEmpty() ? Sfx.Sound.LOCK : Sfx.Sound.COIN);
		}
		repaint();
	}

	/** 얼마가 모자라는지 적어 준다. 그냥 '부족합니다' 보다 알아보기 쉽다. */
	private String shortOf(int price) {
		return "코인이 " + (price - Wallet.coins()) + " 부족해요 (" + price + " 필요)";
	}

	private String names(List<Attack> items) {
		StringBuilder out = new StringBuilder();
		for (Attack a : items)
			out.append(out.length() == 0 ? "" : ", ").append(a.label);
		return out.toString();
	}

	private static Rectangle rowBounds(int c, int r) {
		return new Rectangle(ROWS_X[c], ROWS_TOP + r * ROW_STEP, ROW_W, ROW_H);
	}

	// 그리기
	@Override
	void paintDesign(Graphics2D g) {
		background(g, SKY);
		back(g);
		titlePill(g, "상점");
		coins(g, COIN_RIGHT, COIN_TOP);
		gear(g);

		awningPanel(g, SKIN_PANEL, "블록 스킨", LIFT);
		for (int i = 0; i < Skin.values().length; i++)
			skinRow(g, i);

		awningPanel(g, BOX_PANEL, "아이템 상자", LIFT);
		for (int i = 0; i < Box.values().length; i++)
			boxRow(g, i);

		awningPanel(g, BAG_PANEL, "보유 아이템", LIFT);
		bag(g);

		if (!status.isEmpty()) {
			int w = textWidth(g, status, 36) + 72;
			Rectangle r = new Rectangle(DESIGN_W / 2 - w / 2, STATUS_TOP, w, STATUS_H);
			box(g, r, 26, Ui.DARK_CARD, Ui.DARK_CARD_BORDER, 8, 7, Ui.SHADOW);
			textInBox(g, status, r, 36, Color.WHITE);
		}
		guide(g);
	}

	/** 고른 줄은 연노랑 칸에 빨간 테두리를 두른다. */
	private void cursor(Graphics2D g, Rectangle r, boolean on) {
		if (on)
			box(g, r, 16, Ui.LIGHT_YELLOW, Ui.RED, 4, 0, null);
	}

	private void skinRow(Graphics2D g, int i) {
		Skin skin = Skin.values()[i];
		Rectangle r = rowBounds(0, i);
		cursor(g, r, col == 0 && row == i);

		text(g, skin.label, r.x + 68, r.y + 22, 36, Ui.TEXT);
		int x = r.x + 266;
		for (Color c : skin.colors) {
			box(g, new Rectangle(x, r.y + 29, 32, 32), 7, c, null, 0, 0, null);
			x += 38;
		}

		int right = r.x + r.width - 22;
		if (skin == Wallet.skin() || Wallet.owns(skin)) {
			String label = skin == Wallet.skin() ? "장착 중" : "보유";
			int w = textWidth(g, label, 28) + 44;
			Rectangle tag = new Rectangle(right - w, r.y + 17, w, 55);
			box(g, tag, 26, skin == Wallet.skin() ? Ui.DARK_BROWN : Ui.SUB_TEXT, null, 0, 0, null);
			textInBox(g, label, tag, 28, Color.WHITE);
		} else {
			coinPill(g, right, r.y + 13, String.valueOf(skin.price), 30, 30, 64);
		}
	}

	private void boxRow(Graphics2D g, int i) {
		Box box = Box.values()[i];
		Rectangle r = rowBounds(1, i);
		cursor(g, r, col == 1 && row == i);

		drawImage(g, BOX_ICONS.get(box), new Rectangle(r.x + 60, r.y + 15, 60, 60));
		text(g, box.label, r.x + 146, r.y + 8, 36, Ui.TEXT);
		text(g, "아이템 " + box.picks + "개", r.x + 146, r.y + 52, 22, Ui.SUB_TEXT);
		coinPill(g, r.x + r.width - 22, r.y + 13, String.valueOf(box.price), 30, 30, 64);
	}

	/** 아이템 전부를 카드로 늘어놓는다. 하나도 없는 것은 흐리게 그린다. */
	private void bag(Graphics2D g) {
		for (int i = 0; i < Attack.values().length; i++) {
			Attack item = Attack.values()[i];
			Rectangle card = new Rectangle(BAG_X + (i % BAG_COLS) * (CARD_W + CARD_GAP),
					BAG_TOP + (i / BAG_COLS) * (CARD_H + CARD_GAP), CARD_W, CARD_H);
			int count = Wallet.count(item);

			Composite old = g.getComposite();
			if (count == 0)
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			box(g, card, 16, CARD_FILL, count > 0 ? Ui.BROWN : Ui.DASH, 4, 0, null);
			drawImage(g, image(item.icon), new Rectangle(card.x + 14, card.y + 26, 48, 48));
			text(g, item.label, card.x + 70, card.y + 18, 24, Ui.TEXT);
			text(g, "x" + count, card.x + 70, card.y + 54, 24, Ui.SUB_TEXT);
			g.setComposite(old);
		}
	}

	/** 아래 키 안내 알약. */
	private void guide(Graphics2D g) {
		box(g, GUIDE, 37, Ui.CREAM, Ui.BROWN, 5, 5, new Color(168, 105, 63, 230));
		int x = GUIDE.x + 33, top = GUIDE.y + 17;
		String[][] groups = { { "←", "→", "칸 옮기기" }, { "↑", "↓", "고르기" }, { "Enter", "선택 / 구매" }, { "Esc", "뒤로" } };
		for (String[] group : groups) {
			for (int k = 0; k < group.length - 1; k++) {
				int w = Math.max(46, textWidth(g, group[k], 20) + 24);
				smallKeycap(g, new Rectangle(x, top, w, 40), group[k]);
				x += w + 8;
			}
			String label = group[group.length - 1];
			text(g, label, x, top + 4, 26, Ui.TEXT);
			x += textWidth(g, label, 26) + 34;
		}
	}
}
