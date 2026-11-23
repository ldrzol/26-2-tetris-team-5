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
 * 키보드를 왼손(WASD 쪽)과 오른손(방향키 쪽)으로 나눈다. 1p 는 왼쪽 보드라 왼손, 2p 는 오른손이다.
 *   - 혼자 하는 판: 1p 가 키보드를 다 쓴다. 왼손 보조 키(Z X C Space Shift)에 방향키까지 1p 것이다.
 *   - 한 화면 2인: 왼손 보조 키는 1p, 방향키는 2p 설정 키다.
 * P / Esc(일시정지)는 화면 쪽에서 연결한다.
 */
final class GameKeys {

	private static final String PREFIX = "game:";

	/** 아이템 키 묶음 번호. */
	static final int LEFT_HAND = 0;
	static final int RIGHT_HAND = 1;

	/** [손][칸] 손패를 고르는 키. 칸 수는 VersusPanel 손패 수(3)와 같다. */
	static final int[][] ITEM_SELECT = {
			{ KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3 },
			{ KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L } };

	/** [손] 고른 아이템을 쓰는 키. */
	static final int[] ITEM_USE = { KeyEvent.VK_F, KeyEvent.VK_SEMICOLON };

	/** 왼손 보조 키. 설정과 상관없이 늘 된다. 예전 기본 배치(Z·X·C·Space·Shift)라 손에 익은 사람이 많다. */
	private static final int[] AUX_KEYS = {
			KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT };
	private static final int[] AUX_ACTIONS = {
			Settings.ROTATE_CCW, Settings.ROTATE_CW, Settings.HOLD, Settings.HARD_DROP, Settings.HOLD };
	private static final String AUX_TEXT = "보조 Z X C Space Shift";

	/** 혼자 할 때만 더 거는 방향키. (한 화면 2인에서는 2p 기본 키다) */
	private static final int[] ARROW_KEYS = { KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN };
	private static final int[] ARROW_ACTIONS = { Settings.LEFT, Settings.RIGHT, Settings.ROTATE_CW, Settings.SOFT_DROP };

	private GameKeys() {
	}

	/**
	 * 혼자 조종하는 판. player 는 Settings.keys 의 열 번호. (0 = 1p)
	 * 왼손 보조 키를 먼저 걸고 설정 키를 건다. 사용자가 같은 키를 고르면 사용자 설정이 이긴다.
	 */
	static void bind(JComponent c, int player, Board board) {
		clear(c, player);
		bindAux(c, player, board, AUX_KEYS, AUX_ACTIONS, "aux");
		bindAux(c, player, board, ARROW_KEYS, ARROW_ACTIONS, "arrow");
		bindActions(c, player, board);
	}

	/** 한 화면 2인. 1p 는 WASD 쪽, 2p 는 방향키 쪽이라 왼손 보조 키는 1p 에게 건다. */
	static void bindPair(JComponent c, Board first, Board second) {
		clear(c, 0);
		clear(c, 1);
		bindAux(c, 0, first, AUX_KEYS, AUX_ACTIONS, "aux");
		bindActions(c, 0, first);    // 사용자 설정이 보조 키를 이긴다
		bindActions(c, 1, second);
	}

	private static void bindAux(JComponent c, int player, Board board, int[] keys, int[] actionIndex, String name) {
		Runnable[] actions = actions(board);
		for (int i = 0; i < keys.length; i++)
			bindKey(c, player, keys[i], name + i, actions[actionIndex[i]]);
	}

	private static void bindActions(JComponent c, int player, Board board) {
		Runnable[] actions = actions(board);
		for (int i = 0; i < actions.length; i++)
			bindKey(c, player, Settings.keys[i][player], String.valueOf(i), actions[i]);
	}

	/** Settings.ACTIONS 줄 순서대로 보드 동작. */
	private static Runnable[] actions(Board board) {
		Runnable[] actions = new Runnable[Settings.ACTIONS.length];
		actions[Settings.LEFT] = board::moveLeft;
		actions[Settings.RIGHT] = board::moveRight;
		actions[Settings.ROTATE_CW] = board::rotateRight;
		actions[Settings.ROTATE_CCW] = board::rotateLeft;
		actions[Settings.SOFT_DROP] = board::softDrop;
		actions[Settings.HARD_DROP] = board::hardDrop;
		actions[Settings.HOLD] = board::hold;
		return actions;
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

	// 안내 글자

	/** 화면에 적을 키 이름. ; / . 처럼 기호 키는 getKeyText 가 영어 이름을 주므로 글자 그대로 쓴다. */
	static String keyName(int code) {
		switch (code) {
			case KeyEvent.VK_LEFT:
				return "←";
			case KeyEvent.VK_RIGHT:
				return "→";
			case KeyEvent.VK_UP:
				return "↑";
			case KeyEvent.VK_DOWN:
				return "↓";
			default:
				break;
		}
		boolean symbol = code >= KeyEvent.VK_COMMA && code <= KeyEvent.VK_CLOSE_BRACKET;
		return symbol ? String.valueOf((char) code) : KeyEvent.getKeyText(code);
	}

	/** 아이템 칸 아래 안내. 예) "선택 1 2 3 · 사용 F" */
	static String itemHint(int hand) {
		int[] s = ITEM_SELECT[hand];
		return "선택 " + keyName(s[0]) + " " + keyName(s[1]) + " " + keyName(s[2])
				+ " · 사용 " + keyName(ITEM_USE[hand]);
	}

	/** 조작키는 이 키를 쓸 수 없다. 게임 화면이 따로 쓰는 키면 그 쓰임을, 아니면 null. */
	static String reservedUse(int code) {
		if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_P)
			return "일시 정지";
		for (int hand = 0; hand < ITEM_USE.length; hand++) {
			if (code == ITEM_USE[hand])
				return "아이템 사용";
			for (int key : ITEM_SELECT[hand])
				if (code == key)
					return "아이템 선택";
		}
		return null;
	}

	/** 일시정지 화면에 적을 조작키 목록. kind 는 MainFrame 화면 이름. */
	static String[] guide(String kind) {
		if (MainFrame.LOCAL.equals(kind))
			return new String[] {
					"1P  " + describe(0) + "  (" + AUX_TEXT + ")",
					"1P  아이템 " + itemHint(LEFT_HAND),
					"2P  " + describe(1),
					"2P  아이템 " + itemHint(RIGHT_HAND) };
		String keys = describe(0) + "  (" + AUX_TEXT + " · 방향키)";
		if (MainFrame.LOBBY.equals(kind))      // 네트워크 대전만 아이템을 쓴다
			return new String[] { keys, "아이템 " + itemHint(LEFT_HAND) };
		return new String[] { keys };
	}

	private static String describe(int player) {
		int[][] k = Settings.keys;
		return "이동 " + keyName(k[Settings.LEFT][player]) + " " + keyName(k[Settings.RIGHT][player])
				+ " · 회전 " + keyName(k[Settings.ROTATE_CW][player]) + " " + keyName(k[Settings.ROTATE_CCW][player])
				+ " · 내리기 " + keyName(k[Settings.SOFT_DROP][player])
				+ " · 떨구기 " + keyName(k[Settings.HARD_DROP][player])
				+ " · 홀드 " + keyName(k[Settings.HOLD][player]);
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
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)) != null : "기본 왼쪽 이동 키가 안 걸렸다";
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0)) != null : "혼자 할 때 보조 키가 안 걸렸다";
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)) != null : "혼자 할 때 방향키가 안 걸렸다";

		int[][] mine = Settings.copy(Settings.DEFAULT_KEYS);
		mine[Settings.LEFT][0] = KeyEvent.VK_U;          // 왼쪽 이동을 U 로 바꾼다
		Settings.keys = mine;
		bind(panel, 0, board);

		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0)) != null : "바꾼 키가 안 걸렸다";
		assert im.get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0)) == null : "예전 키가 그대로 남아 있다";

		board.start();
		int before = board.getGame().getCurX();
		panel.getActionMap().get(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0)))
				.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, ""));
		assert board.getGame().getCurX() == before - 1
				: "바꾼 키를 눌러도 블록이 안 움직인다: " + before + " -> " + board.getGame().getCurX();

		// 한 화면 2인: 1p 키(WASD), 2p 키(방향키), 1p 보조 키(X)가 모두 걸려야 한다
		Settings.keys = Settings.copy(Settings.DEFAULT_KEYS);
		Board second = new Board(new JLabel());
		bindPair(panel, board, second);
		assert String.valueOf(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0))).startsWith(prefix(0))
				: "2p 를 걸면서 1p 키가 지워졌다";
		assert String.valueOf(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0))).startsWith(prefix(1))
				: "방향키가 2p 에게 안 걸렸다";
		assert String.valueOf(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0))).startsWith(prefix(0))
				: "보조 키(X)가 1p 에게 안 걸렸다";

		// 기본 조작키가 일시정지·아이템 키와 겹치면 안 된다
		for (int[] row : Settings.DEFAULT_KEYS)
			for (int key : row)
				assert reservedUse(key) == null : keyName(key) + " 가 " + reservedUse(key) + " 키와 겹친다";
		assert keyName(KeyEvent.VK_SEMICOLON).equals(";") : "기호 키 이름이 이상하다";

		System.out.println("ok");
		System.exit(0);          // Swing 타이머가 돌고 있어서 그냥 두면 안 끝난다
	}
}
