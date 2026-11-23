package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * 판이 끝났을 때 덮는 결과 카드. Figma "리팩 - 게임 오버 / 클리어 화면 / 승패 화면(한 화면)".
 *
 * 게임 화면 위에 반투명하게 덮인다. ←→(↑↓) 로 고르고 Enter 로 선택하거나, 버튼을 누른다.
 *   - 혼자 하는 판: 가운데 카드 하나에 점수, 시간·레벨·줄 수 (GAME OVER / CLEAR!)
 *   - 대전: 넓은 카드 안에 양쪽 결과 카드 두 장. 이긴 쪽은 크림색, 진 쪽은 회색이다.
 */
class GameOverOverlay extends DesignPanel {

	// 혼자 하는 판 (Figma "리팩 - 게임 오버")
	private static final Rectangle CARD = new Rectangle(628, 328, 746, 765);
	private static final int TITLE_TOP = 406, SCORE_TOP = 543, STAT_TOP = 656, STAT_STEP = 55;
	private static final int BUTTON_TOP = 930;

	// 대전 (Figma "리팩 - 승패 화면(한 화면)")
	private static final Rectangle WIDE = new Rectangle(259, 223, 1483, 975);
	private static final int WIDE_TITLE_TOP = 308, WIDE_BUTTON_TOP = 998;
	private static final Rectangle[] SIDES = {
			new Rectangle(493, 468, 469, 461), new Rectangle(1038, 468, 469, 461) };
	private static final Color[] BADGE = { new Color(0x89C8FD), new Color(0xFAB7CD) };
	private static final Color LOSE_FILL = new Color(0xA7A7A7);
	/** 두 카드 아래 한 줄 (정산 결과) */
	private static final int NOTE_TOP = 946;
	private static final float NOTE_SIZE = 30f;

	private static final int BUTTON_W = 281, BUTTON_H = 131;
	private static final int[] BUTTON_X = { 689, 1030 };
	private static final float TITLE_SIZE = 75f, BIG_SIZE = 64f, TEXT_SIZE = 36f;

	private final String title, scoreText, coinText;
	private final String[] stats;
	private final String[] buttons;
	private final Runnable[] actions;

	/** 대전 결과일 때만 있다. [0] = 왼쪽 보드, [1] = 오른쪽 보드. (lines / items 는 없을 수도 있다) */
	private final String[] names, results, sideScores, sideLines, sideItems;

	private int selected = 0;

	/**
	 * 혼자 하는 판의 결과. stats 는 점수 아래 작은 줄들 (예: "07:00", "Level 2", "Line 32").
	 * coins 가 0 이면 코인 줄을 그리지 않는다.
	 */
	GameOverOverlay(String title, int score, String[] stats, int coins,
			String first, Runnable onFirst, Runnable onMenu) {
		this(title, String.format("%,d", score), stats, coins, new String[] { first, "메인으로" },
				new Runnable[] { onFirst, onMenu }, null, null, null, null, null);
	}

	/**
	 * 대전 결과. names 예) "1P" / "2P", results 예) "WIN" / "LOSE" / "DRAW".
	 * lines / items 가 null 이면 그 줄을 그리지 않는다. note 는 카드 아래 한 줄 (정산 결과), 없으면 null.
	 */
	GameOverOverlay(String[] names, String[] results, int[] scores, int[] lines, int[] items, String note,
			String first, Runnable onFirst, Runnable onMenu) {
		this("GAME OVER", null, null, note, new String[] { first, "메인으로" },
				new Runnable[] { onFirst, onMenu }, names, results,
				new String[] { String.format("%,d", scores[0]), String.format("%,d", scores[1]) },
				lines == null ? null : new String[] { "Line  " + lines[0], "Line  " + lines[1] },
				items == null ? null : new String[] { "Item  " + items[0], "Item  " + items[1] });
	}

	private GameOverOverlay(String title, String scoreText, String[] stats, int coins, String[] buttons,
			Runnable[] actions, String[] names, String[] results, String[] sideScores, String[] sideLines,
			String[] sideItems) {
		this(title, scoreText, stats, coins > 0 ? "코인 +" + coins : null, buttons, actions, names, results,
				sideScores, sideLines, sideItems);
	}

	private GameOverOverlay(String title, String scoreText, String[] stats, String coinText, String[] buttons,
			Runnable[] actions, String[] names, String[] results, String[] sideScores, String[] sideLines,
			String[] sideItems) {
		this.title = title;
		this.scoreText = scoreText;
		this.stats = stats;
		this.coinText = coinText;
		this.buttons = buttons;
		this.actions = actions;
		this.names = names;
		this.results = results;
		this.sideScores = sideScores;
		this.sideLines = sideLines;
		this.sideItems = sideItems;
		setOpaque(false);   // 뒤 게임 화면이 비쳐야 한다

		Ui.bind(this, KeyEvent.VK_LEFT, "overLeft", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_RIGHT, "overRight", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_UP, "overUp", () -> move(-1));
		Ui.bind(this, KeyEvent.VK_DOWN, "overDown", () -> move(+1));
		Ui.bind(this, KeyEvent.VK_ENTER, "overEnter", this::choose);
		for (int i = 0; i < 2; i++) {
			final int index = i;
			onClick(button(i), () -> {
				selected = index;
				choose();
			});
		}
	}

	private boolean versus() {
		return names != null;
	}

	private void move(int d) {
		selected = (selected + d + 2) % 2;
		Sfx.play(Sfx.Sound.MOVE);
		repaint();
	}

	private void choose() {
		Sfx.play(Sfx.Sound.ROTATE);
		actions[selected].run();
	}

	private Rectangle button(int index) {
		return new Rectangle(BUTTON_X[index], versus() ? WIDE_BUTTON_TOP : BUTTON_TOP, BUTTON_W, BUTTON_H);
	}

	@Override
	void paintDesign(Graphics2D g) {
		scrim(g);
		if (versus())
			paintVersus(g);
		else
			paintSingle(g);

		for (int i = 0; i < 2; i++)
			smallOval(g, button(i), buttons[i], TEXT_SIZE, i == selected);
	}

	private void paintSingle(Graphics2D g) {
		darkCard(g, CARD);
		textCentered(g, title, TITLE_TOP, TITLE_SIZE, Color.WHITE);
		textCentered(g, scoreText, SCORE_TOP, BIG_SIZE, Color.WHITE);
		int top = STAT_TOP;
		for (String s : stats) {
			textCentered(g, s, top, TEXT_SIZE, Color.WHITE);
			top += STAT_STEP;
		}
		if (coinText != null)
			textCentered(g, coinText, top, TEXT_SIZE, Ui.YELLOW);
	}

	private void paintVersus(Graphics2D g) {
		darkCard(g, WIDE);
		textCentered(g, title, WIDE_TITLE_TOP, TITLE_SIZE, Color.WHITE);

		for (int i = 0; i < SIDES.length; i++) {
			Rectangle r = SIDES[i];
			double cx = r.x + r.width / 2.0;
			Color fill = "LOSE".equals(results[i]) ? LOSE_FILL : Ui.CREAM;
			box(g, r, 15, fill, Ui.BROWN, 10, 19, Ui.SHADOW);

			int badgeW = Math.max(112, textWidth(g, names[i], 40) + 50);
			Rectangle badge = new Rectangle((int) Math.round(cx - badgeW / 2.0), r.y + 31, badgeW, 69);
			box(g, badge, 34, BADGE[i], null, 0, 0, null);
			textInBox(g, names[i], badge, 40, Color.BLACK);

			textAt(g, results[i], cx, r.y + 118, BIG_SIZE, Color.BLACK);
			textAt(g, sideScores[i], cx, r.y + 212, BIG_SIZE, Color.BLACK);
			if (sideLines != null)
				textAt(g, sideLines[i], cx, r.y + 307, TEXT_SIZE, Color.BLACK);
			if (sideItems != null)
				textAt(g, sideItems[i], cx, r.y + 351, TEXT_SIZE, Color.BLACK);
		}
		if (coinText != null)
			textCentered(g, coinText, NOTE_TOP, NOTE_SIZE, Ui.YELLOW);
	}
}
