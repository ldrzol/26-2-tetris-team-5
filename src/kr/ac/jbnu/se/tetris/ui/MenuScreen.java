package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;

/**
 * 메뉴 화면 공통. Figma "리팩 - 메인 화면 / 모드 선택 / 2인 모드 세부 선택 / 2인 모드 방 생성" 이
 * 전부 같은 모양이고 제목과 타원 버튼만 다르다.
 *
 * 버튼을 마우스로 눌러서 고른다.
 * 숟가락(또는 Esc)은 뒤로 간다.
 */
class MenuScreen extends DesignPanel {

	// 메인 화면 (Figma "리팩 - 메인 화면"). 디자인의 두 버튼 위에 상점 버튼을 하나 더 얹었다.
	private static final Rectangle HOME_LOGO = new Rectangle(468, 109, 1064, 302);
	private static final int HOME_X = 1406, HOME_W = 394, HOME_H = 184;
	private static final int[] HOME_TOP = { 700, 915, 1130 };
	private static final float HOME_SIZE = 80f;
	private static final int HOME_COIN_RIGHT = 1929;

	// 나머지 메뉴 (Figma "리팩 - 모드 선택")
	private static final int TITLE_TOP = 300;
	private static final int ITEM_X = 758, ITEM_W = 481, ITEM_H = 184, ITEM_STEP = 248;
	private static final int TOP_3 = 552, TOP_2 = 600;
	private static final float ITEM_SIZE = 64f, ITEM_SIZE_2 = 72f;   // 버튼이 둘이면 글자가 크다

	/** 디자인에 없는 안내문 자리. 로비의 연결 상태처럼 꼭 필요한 경우에만 쓴다. */
	private static final int STATUS_TOP = 1150, STATUS_H = 97;
	private static final float STATUS_SIZE = 34f;

	private final boolean home;
	private final String title;
	private final String[] items;
	private final IntConsumer onSelect;
	private final boolean showCoins;
	private final boolean hasBack;

	/** 메인 화면. 시작하기 / 상점 / 설정. */
	static MenuScreen home(IntConsumer onSelect) {
		return new MenuScreen(true, null, new String[] { "시작하기", "상점", "설정" }, onSelect, null, true);
	}

	/**
	 * 메뉴 하나. onBack 이 null 이면 숟가락을 그리지 않는다.
	 */
	MenuScreen(String title, String[] items, IntConsumer onSelect, Runnable onBack, boolean showCoins) {
		this(false, title, items, onSelect, onBack, showCoins);
	}

	private MenuScreen(boolean home, String title, String[] items, IntConsumer onSelect, Runnable onBack,
			boolean showCoins) {
		this.home = home;
		this.title = title;
		this.items = items;
		this.onSelect = onSelect;
		this.showCoins = showCoins;
		this.hasBack = onBack != null;

		for (int i = 0; i < items.length; i++) {
			final int index = i;
			onClick(button(i), () -> choose(index));
		}
		if (onBack != null) {
			onBack(onBack);
			Ui.bind(this, KeyEvent.VK_ESCAPE, "menuBack", onBack);
		}
	}

	private Rectangle button(int i) {
		if (home)
			return new Rectangle(HOME_X, HOME_TOP[HOME_TOP.length - items.length + i], HOME_W, HOME_H);
		int top = (items.length >= 3 ? TOP_3 : TOP_2) + i * ITEM_STEP;
		return new Rectangle(ITEM_X, top, ITEM_W, ITEM_H);
	}

	private void choose(int index) {
		if (isSelectable(index)) {
			Sfx.play(Sfx.Sound.ROTATE);
			select(index);
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

	/** 버튼 아래에 띄울 안내문. 없으면 null. */
	String status() {
		return null;
	}

	@Override
	void paintDesign(Graphics2D g) {
		background(g, home ? CAFE : SKY);

		if (home)
			drawImage(g, TITLE_LOGO, HOME_LOGO);
		else
			menuTitle(g, title, TITLE_TOP);

		float size = home ? HOME_SIZE : items.length >= 3 ? ITEM_SIZE : ITEM_SIZE_2;
		for (int i = 0; i < items.length; i++)
			oval(g, button(i), items[i], size);

		if (showCoins)
			coins(g, home ? HOME_COIN_RIGHT : COIN_RIGHT + 115, COIN_TOP);
		if (hasBack)
			back(g);

		String status = status();
		if (status != null) {
			int w = textWidth(g, status, STATUS_SIZE) + 80;
			Rectangle r = new Rectangle(DESIGN_W / 2 - w / 2, STATUS_TOP, w, STATUS_H);
			box(g, r, 26, Ui.DARK_CARD, Ui.DARK_CARD_BORDER, 8, 7, Ui.SHADOW);
			textInBox(g, status, r, STATUS_SIZE, Color.WHITE);
		}
	}
}
