package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.save.Settings;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

	/**
	 * player 는 Settings.keys 의 열 번호. (0 = 1p)
	 *
	 * 한 화면 2인 대전에서는 같은 패널에 두 번 부르므로, 연결 이름에 player 를 넣어
	 * 서로를 지우지 않게 한다.
	 */
	static void bind(JComponent c, int player, Board board) {
		clear(c, player);

		// 보조 키를 먼저 연결한다. 사용자가 같은 키를 고르면 사용자 설정이 이긴다.
		// 2p 에게는 걸지 않는다. 같은 키를 두 보드가 나눠 쓸 수 없다.
		if (player == 0) {
			bindKey(c, player, KeyEvent.VK_X, "x", board::rotateRight);
			bindKey(c, player, KeyEvent.VK_SHIFT, "shift", board::hold);
		}

		Runnable[] actions = new Runnable[Settings.ACTIONS.length];
		actions[Settings.LEFT] = board::moveLeft;
		actions[Settings.RIGHT] = board::moveRight;
		actions[Settings.ROTATE_CW] = board::rotateRight;
		actions[Settings.ROTATE_CCW] = board::rotateLeft;
		actions[Settings.SOFT_DROP] = board::softDrop;
		actions[Settings.HARD_DROP] = board::hardDrop;
		actions[Settings.HOLD] = board::hold;

		for (int i = 0; i < actions.length; i++)
			bindKey(c, player, Settings.keys[i][player], String.valueOf(i), actions[i]);
	}

	/** 그 플레이어에게 연결해 둔 게임 키를 모두 지운다. */
	private static void clear(JComponent c, int player) {
		InputMap im = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke[] old = im.allKeys();
		if (old == null)
			return;
		for (KeyStroke k : old) {
			Object name = im.get(k);
			if (name instanceof String && ((String) name).startsWith(prefix(player)))
				im.remove(k);
		}
	}

	private static String prefix(int player) {
		return PREFIX + player + ":";
	}

	/**
	 * 키 하나를 연결한다. Shift 를 누른 채여도 동작하도록 Shift 가 섞인 모양도 같이 연결한다.
	 * Shift 자체를 고른 경우에는 눌린 순간 수정키 표시가 붙으므로 그 모양으로 연결한다.
	 */
	private static void bindKey(JComponent c, int player, int keyCode, String name, Runnable action) {
		String id = prefix(player) + name;
		if (keyCode == KeyEvent.VK_SHIFT) {
			Ui.bind(c, KeyStroke.getKeyStroke(keyCode, InputEvent.SHIFT_DOWN_MASK), id, action);
			return;
		}
		Ui.bind(c, KeyStroke.getKeyStroke(keyCode, 0), id, action);
		Ui.bind(c, KeyStroke.getKeyStroke(keyCode, InputEvent.SHIFT_DOWN_MASK), id, action);
	}

	/**
	 * 자가 점검. java -ea 로 실행한다.
	 *
	 * 설정에서 키를 바꿨을 때 (1) 새 키가 연결되고 (2) 예전 키는 지워지고
	 * (3) 눌렀을 때 실제로 보드가 움직이는지까지 본다.
	 */
	public static void main(String[] args) {
		JPanel panel = new JPanel();
		Board board = new Board(new JLabel());
		InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		Settings.keys = Settings.copy(Settings.DEFAULT_KEYS);
		bind(panel, 0, board);
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) != null : "기본 왼쪽 이동 키가 안 걸렸다";

		int[][] mine = Settings.copy(Settings.DEFAULT_KEYS);
		mine[Settings.LEFT][0] = KeyEvent.VK_J;          // 왼쪽 이동을 J 로 바꾼다
		Settings.keys = mine;
		bind(panel, 0, board);

		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0)) != null : "바꾼 키가 안 걸렸다";
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) == null : "예전 키가 그대로 남아 있다";

		board.start();
		int before = board.getGame().getCurX();
		panel.getActionMap().get(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0)))
				.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, ""));
		assert board.getGame().getCurX() == before - 1
				: "바꾼 키를 눌러도 블록이 안 움직인다: " + before + " -> " + board.getGame().getCurX();

		// 한 화면 2인: 같은 패널에 둘을 걸어도 서로를 지우지 않아야 한다
		Settings.keys = Settings.copy(Settings.DEFAULT_KEYS);
		Board second = new Board(new JLabel());
		bind(panel, 0, board);
		bind(panel, 1, second);
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) != null : "2p 를 걸면서 1p 키가 지워졌다";
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)) != null : "2p 키가 안 걸렸다";

		System.out.println("ok");
		System.exit(0);          // Swing 타이머가 돌고 있어서 그냥 두면 안 끝난다
	}
}
