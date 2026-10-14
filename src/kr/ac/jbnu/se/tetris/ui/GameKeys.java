package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.save.Settings;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * 게임 화면의 조작키를 Settings 에 적힌 대로 연결한다.
 *
 * 설정을 바꾸면 이미 떠 있는 게임 화면에는 자동으로 반영되지 않으므로,
 * 화면을 만들 때와 일시정지에서 돌아올 때 bind() 를 다시 부른다.
 * 앞서 연결해 둔 키는 지우고 새로 연결한다.
 *
 * 설정과 상관없이 늘 되는 보조 키: X(시계 회전), Shift(홀드), P(일시정지는 화면 쪽에서 연결)
 */
final class GameKeys {

	private static final String PREFIX = "game:";

	private GameKeys() {
	}

	/** player 는 Settings.keys 의 열 번호. (0 = 1p) */
	static void bind(JComponent c, int player, Board board) {
		clear(c);

		// 보조 키를 먼저 연결한다. 사용자가 같은 키를 고르면 사용자 설정이 이긴다.
		bindKey(c, KeyEvent.VK_X, "x", board::rotateRight);
		bindKey(c, KeyEvent.VK_SHIFT, "shift", board::hold);

		Runnable[] actions = new Runnable[Settings.ACTIONS.length];
		actions[Settings.LEFT] = board::moveLeft;
		actions[Settings.RIGHT] = board::moveRight;
		actions[Settings.ROTATE_CW] = board::rotateRight;
		actions[Settings.ROTATE_CCW] = board::rotateLeft;
		actions[Settings.SOFT_DROP] = board::softDrop;
		actions[Settings.HARD_DROP] = board::hardDrop;
		actions[Settings.HOLD] = board::hold;

		for (int i = 0; i < actions.length; i++)
			bindKey(c, Settings.keys[i][player], String.valueOf(i), actions[i]);
	}

	/** 이전에 연결해 둔 게임 키를 모두 지운다. */
	private static void clear(JComponent c) {
		InputMap im = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke[] old = im.allKeys();
		if (old == null)
			return;
		for (KeyStroke k : old) {
			Object name = im.get(k);
			if (name instanceof String && ((String) name).startsWith(PREFIX))
				im.remove(k);
		}
	}

	/**
	 * 키 하나를 연결한다. Shift 를 누른 채여도 동작하도록 Shift 가 섞인 모양도 같이 연결한다.
	 * Shift 자체를 고른 경우에는 눌린 순간 수정키 표시가 붙으므로 그 모양으로 연결한다.
	 */
	private static void bindKey(JComponent c, int keyCode, String name, Runnable action) {
		String id = PREFIX + name;
		if (keyCode == KeyEvent.VK_SHIFT) {
			Ui.bind(c, KeyStroke.getKeyStroke(keyCode, InputEvent.SHIFT_DOWN_MASK), id, action);
			return;
		}
		Ui.bind(c, KeyStroke.getKeyStroke(keyCode, 0), id, action);
		Ui.bind(c, KeyStroke.getKeyStroke(keyCode, InputEvent.SHIFT_DOWN_MASK), id, action);
	}
}
