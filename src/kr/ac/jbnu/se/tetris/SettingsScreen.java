package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

/**
 * 환경 설정 화면. Figma "환경 설정 화면".
 *
 * 마우스로 누른다. 음량은 눈금을, 고스트/그리드는 ON·OFF 를 누르면 바뀌고,
 * 조작키는 칸을 누른 뒤 새 키를 누르면 바뀐다. 저장하기를 누를 때만 Settings 에
 * 옮겨 적고, 뒤로나 Esc 는 고친 것을 버린다.
 */
class SettingsScreen extends DesignPanel {

	// ── Figma "환경 설정 화면" 수치 (디자인 기준 픽셀) ──────────────────────
	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;

	private static final float GROUP_SIZE = 36f;   // 묶음 탭
	private static final float ROW_SIZE = 40f;     // 항목 글자
	private static final float SMALL_SIZE = 32f;   // 표 머리 / ON·OFF
	private static final float HINT_SIZE = 24f;

	/** 꺼져 있는 눈금과 버튼의 색 */
	private static final Color OFF_COLOR = new Color(0x6B665C);

	private static final Rectangle BACK = new Rectangle(73, 66, 232, 81);
	private static final Rectangle BACK_ARROW = new Rectangle(132, 88, 20, 37);
	private static final int BACK_TEXT_X = 179, BACK_TEXT_TOP = 79;

	private static final Rectangle SOUND_TAB = new Rectangle(73, 201, 186, 62);
	private static final Rectangle SOUND_BOX = new Rectangle(73, 263, 927, 200);
	private static final int SOUND_ROW = 335;   // 글자 위 (상자 가운데)
	private static final int GAUGE_ROW = 337;   // 눈금 위
	private static final int SOUND_LABEL_X = 141, SOUND_VALUE_X = 873;
	private static final int GAUGE_X = 347, GAUGE_STEP = 49, GAUGE_W = 38, GAUGE_H = 52;
	private static final int GAUGE_COUNT = 10;
	private static final int GAUGE_UNIT = 100 / GAUGE_COUNT;

	private static final Rectangle GAME_TAB = new Rectangle(74, 681, 186, 62);
	private static final Rectangle GAME_BOX = new Rectangle(74, 743, 927, 466);
	private static final int[] GAME_ROW = { 862, 1053 };
	private static final int[] SWITCH_ROW = { 862, 1055 };
	private static final int GAME_LABEL_X = 142;
	private static final int ON_X = 722, OFF_X = 823, SWITCH_W = 94, SWITCH_H = 52;

	private static final Rectangle KEY_TAB = new Rectangle(1034, 201, 186, 62);
	private static final Rectangle KEY_BOX = new Rectangle(1034, 263, 891, 946);
	private static final int KEY_LABEL_X = 1121, KEY_HEAD_TOP = 322;
	private static final int[] KEY_HEAD_X = { 1587, 1769 };
	private static final int[] KEY_COL_X = { 1555, 1737 };
	private static final int KEY_TOP = 390, KEY_STEP = 95, KEY_W = 112, KEY_H = 75;
	/** 줄 이름은 키 칸보다 이만큼 아래에서 시작한다 */
	private static final int KEY_LABEL_DROP = 10;
	private static final Rectangle PAUSE_KEY = new Rectangle(1647, 1065, KEY_W, KEY_H);
	private static final int HINT_X = 1072, HINT_TOP = 1165;

	private static final Rectangle SAVE = new Rectangle(675, 1258, 326, 107);
	private static final Rectangle SAVE_ARROW = new Rectangle(728, 1284, 29, 54);
	private static final Rectangle DEFAULTS = new Rectangle(1034, 1258, 326, 107);
	private static final int BUTTON_TEXT_TOP = 1284;
	private static final int SAVE_TEXT_X = 795, DEFAULTS_TEXT_X = 1127;

	private final Runnable onBack;

	// 고치는 중인 사본
	private int sfx = Settings.sfx;
	private boolean ghost = Settings.ghost, grid = Settings.grid;
	private int[][] keys = Settings.copy(Settings.keys);

	/** 새 키를 기다리는 칸. (줄 * 2 + 열), 없으면 -1 */
	private int waiting = -1;

	SettingsScreen(Runnable onBack) {
		this.onBack = onBack;

		onClick(BACK, onBack);
		onClick(SAVE, this::save);
		onClick(DEFAULTS, this::resetDefaults);

		for (int i = 0; i < GAUGE_COUNT; i++) {
			final int volume = (i + 1) * GAUGE_UNIT;
			onClick(gaugeCell(i), () -> setVolume(volume));
		}

		for (int row = 0; row < 2; row++) {
			final int r = row;
			onClick(switchBox(row, true), () -> setSwitch(r, true));
			onClick(switchBox(row, false), () -> setSwitch(r, false));
		}

		for (int i = 0; i < Settings.ACTIONS.length; i++)
			for (int c = 0; c < 2; c++) {
				final int cell = i * 2 + c;
				onClick(keyBox(i, c), () -> waitForKey(cell));
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

	// ── 값 바꾸기 ───────────────────────────────────────────────────────────

	private void setVolume(int volume) {
		sfx = volume;
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
		repaint();
	}

	/** 기다리는 칸이 있으면 그 칸의 키를 바꾼다. 없으면 Esc 로 나간다. */
	private void onKey(int code) {
		if (waiting < 0) {
			if (code == KeyEvent.VK_ESCAPE)
				onBack.run();
			return;
		}
		if (code != KeyEvent.VK_ESCAPE)   // Esc 는 취소
			keys[waiting / 2][waiting % 2] = code;
		waiting = -1;
		repaint();
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
		repaint();
	}

	// ── 자리 ────────────────────────────────────────────────────────────────

	private Rectangle gaugeCell(int index) {
		return new Rectangle(GAUGE_X + index * GAUGE_STEP, GAUGE_ROW, GAUGE_W, GAUGE_H);
	}

	private Rectangle switchBox(int row, boolean on) {
		return new Rectangle(on ? ON_X : OFF_X, SWITCH_ROW[row], SWITCH_W, SWITCH_H);
	}

	private Rectangle keyBox(int row, int col) {
		return new Rectangle(KEY_COL_X[col], KEY_TOP + row * KEY_STEP, KEY_W, KEY_H);
	}

	// ── 그리기 ──────────────────────────────────────────────────────────────

	@Override
	void paintDesign(Graphics2D g) {
		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);

		outline(g, BACK);
		drawImageFlipped(g, ARROW_ICON, BACK_ARROW);
		text(g, "뒤로", BACK_TEXT_X, BACK_TEXT_TOP, ROW_SIZE, Ui.CREAM);

		outline(g, SOUND_BOX);
		tab(g, SOUND_TAB, "소리", GROUP_SIZE);
		gauge(g, "효과음", sfx);

		outline(g, GAME_BOX);
		tab(g, GAME_TAB, "게임", GROUP_SIZE);
		switchRow(g, 0, "고스트 블록 표시", ghost);
		switchRow(g, 1, "그리드 표시", grid);

		outline(g, KEY_BOX);
		tab(g, KEY_TAB, "조작키", GROUP_SIZE);
		keyTable(g);

		fill(g, SAVE, Ui.CREAM);
		drawImage(g, ARROW_ICON, SAVE_ARROW);
		text(g, "저장하기", SAVE_TEXT_X, BUTTON_TEXT_TOP, ROW_SIZE, Color.BLACK);
		outline(g, DEFAULTS);
		text(g, "기본값으로", DEFAULTS_TEXT_X, BUTTON_TEXT_TOP, ROW_SIZE, Ui.CREAM);
	}

	/** 음량 한 줄: 이름 + 눈금 열 개 + 숫자. 켜진 눈금은 크림색으로 채운다. */
	private void gauge(Graphics2D g, String label, int volume) {
		text(g, label, SOUND_LABEL_X, SOUND_ROW, ROW_SIZE, Ui.CREAM);
		text(g, String.valueOf(volume), SOUND_VALUE_X, SOUND_ROW, ROW_SIZE, Ui.CREAM);
		for (int i = 0; i < GAUGE_COUNT; i++) {
			Rectangle cell = gaugeCell(i);
			if (i < volume / GAUGE_UNIT)
				fill(g, cell, Ui.CREAM);
			else
				outline(g, cell, OFF_COLOR, BORDER);
		}
	}

	private void switchRow(Graphics2D g, int row, String label, boolean on) {
		text(g, label, GAME_LABEL_X, GAME_ROW[row], ROW_SIZE, Ui.CREAM);
		button(g, switchBox(row, true), "ON", on);
		button(g, switchBox(row, false), "OFF", !on);
	}

	/** 고른 쪽은 크림색으로 채우고 글자를 검게, 나머지는 테두리만 그린다. */
	private void button(Graphics2D g, Rectangle r, String label, boolean on) {
		if (on)
			fill(g, r, Ui.CREAM);
		else
			outline(g, r, OFF_COLOR, BORDER);
		textInBox(g, label, r, SMALL_SIZE, on ? Color.BLACK : OFF_COLOR);
	}

	private void keyTable(Graphics2D g) {
		text(g, "동작", KEY_LABEL_X, KEY_HEAD_TOP, SMALL_SIZE, DIM_CREAM);
		text(g, "1p", KEY_HEAD_X[0], KEY_HEAD_TOP, SMALL_SIZE, DIM_CREAM);
		text(g, "2p", KEY_HEAD_X[1], KEY_HEAD_TOP, SMALL_SIZE, DIM_CREAM);

		for (int i = 0; i < Settings.ACTIONS.length; i++) {
			text(g, Settings.ACTIONS[i], KEY_LABEL_X, KEY_TOP + i * KEY_STEP + KEY_LABEL_DROP,
					ROW_SIZE, Ui.CREAM);
			for (int c = 0; c < 2; c++)
				keyCell(g, keyBox(i, c), KeyEvent.getKeyText(keys[i][c]), waiting == i * 2 + c);
		}

		// 일시 정지는 Esc 고정이라 두 열 가운데에 한 칸만 둔다
		text(g, "일시 정지", KEY_LABEL_X, PAUSE_KEY.y + KEY_LABEL_DROP, ROW_SIZE, Ui.CREAM);
		keyCell(g, PAUSE_KEY, "Esc", false);

		text(g, "바꿀 키를 선택한 뒤 새 키를 누르세요", HINT_X, HINT_TOP, HINT_SIZE, DIM_CREAM);
	}

	/** 키 한 칸. 새 키를 기다리는 칸은 크림색으로 채워서 표시한다. */
	private void keyCell(Graphics2D g, Rectangle r, String label, boolean picked) {
		if (picked)
			fill(g, r, Ui.CREAM);
		else
			outline(g, r, Ui.CREAM, BORDER);
		// Space, Right 처럼 긴 이름은 칸을 넘치므로 작게 쓴다
		textInBox(g, label, r, label.length() <= 2 ? ROW_SIZE : HINT_SIZE, picked ? Color.BLACK : Ui.CREAM);
	}
}
