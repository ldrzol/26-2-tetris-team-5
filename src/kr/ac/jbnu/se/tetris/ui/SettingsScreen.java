package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.save.Settings;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

/**
 * 환경 설정 화면. Figma "리팩 - 설정 화면".
 *
 * 마우스로 누른다. 음량은 눈금이나 -/+ 를, 고스트/그리드는 ON·OFF 를 누르면 바뀌고,
 * 조작키는 칸을 누른 뒤 새 키를 누르면 바뀐다. 저장하기를 누를 때만 Settings 에
 * 옮겨 적고, 뒤로(숟가락)나 Esc 는 고친 것을 버린다.
 *
 * 디자인의 배경음 / 화면 흔들림 / 아이템 키 줄은 아직 없는 기능이라 그리지 않는다.
 */
class SettingsScreen extends DesignPanel {

	// 소리 상자 (Figma Frame 80)
	private static final Rectangle SOUND = new Rectangle(58, 246, 901, 420);
	private static final int SOUND_ROW = 454;            // 효과음 줄 위 (상자 가운데)
	private static final int LABEL_X = 100;
	private static final Rectangle MINUS = new Rectangle(238, SOUND_ROW + 4, 56, 56);
	private static final Rectangle PLUS = new Rectangle(784, SOUND_ROW + 4, 56, 56);
	private static final int GAUGE_X = 312, GAUGE_STEP = 46, GAUGE_W = 40, GAUGE_H = 48;
	private static final int GAUGE_COUNT = 10;
	private static final int GAUGE_UNIT = 100 / GAUGE_COUNT;
	private static final int VALUE_RIGHT = 934;

	// 게임 상자 (Figma Frame 83)
	private static final Rectangle GAME = new Rectangle(58, 781, 901, 420);
	private static final int GAME_TOP = 852, GAME_STEP = 110;
	private static final int TOGGLE_X = 710, TOGGLE_W = 210, TOGGLE_H = 74;
	private static final String[][] GAME_ROWS = {
			{ "고스트 블록 표시", "블록이 떨어질 자리를 미리 보여줘요" },
			{ "그리드 표시", "보드에 칸 줄을 그려요" } };

	// 조작키 상자 (Figma Frame 82)
	private static final Rectangle KEYS = new Rectangle(1041, 246, 901, 955);
	private static final int TABLE_X = 1092, TABLE_TOP = 318, TABLE_W = 800;
	private static final int HEAD_H = 66, ROW_H = 78, COL_W = 190;
	private static final int[] COL_X = { TABLE_X + 420, TABLE_X + 610 };
	private static final int KEY_H = 58;

	private static final int LIFT = 59;   // 큰 상자의 어닝은 상자보다 59 위에서 시작한다

	private static final Rectangle SAVE = new Rectangle(685, 1257, 281, 131);
	private static final Rectangle DEFAULTS = new Rectangle(1035, 1257, 281, 131);

	private final Runnable onBack;

	private int sfx = Settings.sfx;
	private boolean ghost = Settings.ghost, grid = Settings.grid;
	private int[][] keys = Settings.copy(Settings.keys);

	/** 새 키를 기다리는 칸. (줄 * 2 + 열), 없으면 -1 */
	private int waiting = -1;

	/** 이미 쓰는 키를 골랐을 때 안내. 없으면 null */
	private String warning = null;

	SettingsScreen(Runnable onBack) {
		this.onBack = onBack;

		onBack(onBack);
		onClick(SAVE, this::save);
		onClick(DEFAULTS, this::resetDefaults);

		for (int i = 0; i < GAUGE_COUNT; i++) {
			final int volume = (i + 1) * GAUGE_UNIT;
			onClick(gaugeCell(i), () -> setVolume(volume));
		}
		onClick(MINUS, () -> setVolume(sfx - GAUGE_UNIT));
		onClick(PLUS, () -> setVolume(sfx + GAUGE_UNIT));

		for (int row = 0; row < GAME_ROWS.length; row++) {
			final int r = row;
			onClick(toggleHalf(toggle(row), true), () -> setSwitch(r, true));
			onClick(toggleHalf(toggle(row), false), () -> setSwitch(r, false));
		}

		for (int i = 0; i < Settings.ACTIONS.length; i++)
			for (int c = 0; c < 2; c++) {
				final int cell = i * 2 + c;
				onClick(keyColumn(i, c), () -> waitForKey(cell));
			}

		setFocusable(true);   // 새 키를 받아내려면 포커스가 필요하다
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				onKey(e.getKeyCode());
			}
		});
	}

	@Override
	public void addNotify() {
		super.addNotify();
		SwingUtilities.invokeLater(this::requestFocusInWindow);
	}

	private void setVolume(int volume) {
		sfx = Math.max(0, Math.min(100, volume));
		repaint();
	}

	private void setSwitch(int row, boolean on) {
		if (row == 0)
			ghost = on;
		else
			grid = on;
		repaint();
	}

	private void waitForKey(int cell) {
		waiting = cell;
		warning = null;
		repaint();
	}

	/** 기다리는 칸이 있으면 그 칸의 키를 바꾼다. 없으면 Esc 로 나간다. */
	private void onKey(int code) {
		if (waiting < 0) {
			if (code == KeyEvent.VK_ESCAPE)
				onBack.run();
			return;
		}
		if (code != KeyEvent.VK_ESCAPE) {   // Esc 는 취소
			String clash = clash(code, waiting);
			if (clash != null) {
				warning = GameKeys.keyName(code) + " 는 이미 " + clash + " 키입니다. 다른 키를 누르세요";
				repaint();
				return;
			}
			keys[waiting / 2][waiting % 2] = code;
		}
		warning = null;
		waiting = -1;
		repaint();
	}

	/** code 를 이미 쓰는 곳. 바꾸려는 칸 자신은 빼고 본다. 없으면 null. */
	private String clash(int code, int cell) {
		for (int i = 0; i < keys.length; i++)
			for (int c = 0; c < 2; c++)
				if (i * 2 + c != cell && keys[i][c] == code)
					return (c + 1) + "P " + Settings.ACTIONS[i];
		return GameKeys.reservedUse(code);
	}

	private void save() {
		Settings.sfx = sfx;
		Settings.ghost = ghost;
		Settings.grid = grid;
		Settings.keys = Settings.copy(keys);
		onBack.run();
	}

	private void resetDefaults() {
		sfx = Settings.DEFAULT_VOLUME;
		ghost = Settings.DEFAULT_GHOST;
		grid = Settings.DEFAULT_GRID;
		keys = Settings.copy(Settings.DEFAULT_KEYS);
		waiting = -1;
		warning = null;
		repaint();
	}

	private Rectangle gaugeCell(int index) {
		return new Rectangle(GAUGE_X + index * GAUGE_STEP, SOUND_ROW + 8, GAUGE_W, GAUGE_H);
	}

	private Rectangle toggle(int row) {
		return new Rectangle(TOGGLE_X, GAME_TOP + row * GAME_STEP + 17, TOGGLE_W, TOGGLE_H);
	}

	/** 키 줄 하나의 위쪽. 0 번 줄이 머리 바로 아래다. */
	private static int rowTop(int row) {
		return TABLE_TOP + HEAD_H + row * ROW_H;
	}

	/** 마우스로 누를 수 있는 한 칸 (열 전체 폭). */
	private Rectangle keyColumn(int row, int col) {
		return new Rectangle(COL_X[col], rowTop(row), COL_W, ROW_H);
	}

	@Override
	void paintDesign(Graphics2D g) {
		background(g, SKY);
		back(g);
		titlePill(g, "환경 설정");
		gear(g);

		awningPanel(g, SOUND, "소리", LIFT);
		sound(g);

		awningPanel(g, GAME, "게임", LIFT);
		for (int i = 0; i < GAME_ROWS.length; i++)
			switchRow(g, i, i == 0 ? ghost : grid);

		awningPanel(g, KEYS, "조작키", LIFT);
		keyTable(g);

		smallOval(g, SAVE, "저장하기", 36, false);
		smallOval(g, DEFAULTS, "기본값으로", 36, false);
	}

	/** 효과음 한 줄: 이름, -, 눈금 열 개, +, 숫자. 켜진 눈금은 노랗게 채운다. */
	private void sound(Graphics2D g) {
		text(g, "효과음", LABEL_X, SOUND_ROW + 6, 40, Ui.TEXT);
		roundButton(g, MINUS, "−");
		roundButton(g, PLUS, "+");
		for (int i = 0; i < GAUGE_COUNT; i++) {
			Rectangle cell = gaugeCell(i);
			if (i < sfx / GAUGE_UNIT)
				box(g, cell, 7, Ui.YELLOW, null, 0, 0, null);
			else
				box(g, cell, 7, null, Ui.DASH, 2, 0, null);
		}
		textRight(g, String.valueOf(sfx), VALUE_RIGHT, SOUND_ROW, 50, Ui.TEXT);
	}

	private void roundButton(Graphics2D g, Rectangle r, String label) {
		box(g, r, 28, Ui.CREAM, Ui.DARK_BROWN, 4, 3, new Color(168, 105, 63, 153));
		textInBox(g, label, r, 34, Ui.DARK_BROWN);
	}

	private void switchRow(Graphics2D g, int row, boolean on) {
		int top = GAME_TOP + row * GAME_STEP;
		text(g, GAME_ROWS[row][0], LABEL_X, top + 14, 38, Ui.TEXT);
		text(g, GAME_ROWS[row][1], LABEL_X, top + 66, 22, Ui.SUB_TEXT);
		toggle(g, toggle(row), "ON", "OFF", on, 26);
		if (row < GAME_ROWS.length - 1)
			dashed(g, LABEL_X, top + GAME_STEP, 820);
	}

	private void keyTable(Graphics2D g) {
		text(g, "동작", TABLE_X, TABLE_TOP + 14, 28, Ui.SUB_TEXT);
		badge(g, COL_X[0], "1P", Ui.RED);
		badge(g, COL_X[1], "2P", Ui.BLUE);
		dashed(g, TABLE_X, TABLE_TOP + HEAD_H, TABLE_W);

		for (int i = 0; i < Settings.ACTIONS.length; i++) {
			int top = rowTop(i);
			text(g, Settings.ACTIONS[i], TABLE_X, top + 14, 38, Ui.TEXT);
			for (int c = 0; c < 2; c++)
				cap(g, COL_X[c] + COL_W / 2, top + 9, GameKeys.keyName(keys[i][c]), waiting == i * 2 + c);
			dashed(g, TABLE_X, top + ROW_H, TABLE_W);
		}

		int pauseTop = rowTop(Settings.ACTIONS.length);
		text(g, "일시 정지", TABLE_X, pauseTop + 14, 38, Ui.TEXT);
		cap(g, (COL_X[0] + COL_X[1] + COL_W) / 2, pauseTop + 9, "Esc", false);

		// 안내 줄
		Rectangle bar = new Rectangle(TABLE_X, pauseTop + ROW_H + 14, TABLE_W, 56);
		box(g, bar, 14, Ui.LIGHT_YELLOW, null, 0, 0, null);
		boolean busy = waiting >= 0;
		keycap(g, new Rectangle(bar.x + 16, bar.y + 10, 52, 36), "…", 20, busy);
		String hint = warning != null ? warning
				: busy ? (waiting % 2 + 1) + "P " + Settings.ACTIONS[waiting / 2] + " 키를 바꾸는 중 - 새 키를 누르세요 (취소: Esc)"
						: "바꿀 키 칸을 누른 뒤 새 키를 누르세요";
		text(g, hint, bar.x + 82, bar.y + 12, 25, warning != null ? Ui.RED : Ui.TEXT);
	}

	/** 1P / 2P 머리 배지. */
	private void badge(Graphics2D g, int colX, String label, Color color) {
		int w = 70;
		Rectangle r = new Rectangle(colX + (COL_W - w) / 2, TABLE_TOP + 8, w, 46);
		box(g, r, 23, color, null, 0, 3, new Color(0, 0, 0, 38));
		textInBox(g, label, r, 28, Color.WHITE);
	}

	/** 키캡 하나. 글자 폭에 맞춰 넓어진다. centerX 는 가운데. */
	private void cap(Graphics2D g, int centerX, int top, String label, boolean picked) {
		int w = Math.max(84, textWidth(g, label, 26) + 44);
		keycap(g, new Rectangle(centerX - w / 2, top, w, KEY_H), label, 26, picked);
	}
}
