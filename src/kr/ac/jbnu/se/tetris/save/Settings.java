package kr.ac.jbnu.se.tetris.save;

import java.awt.event.KeyEvent;

/**
 * 환경 설정 값. 화면을 새로 만들어도 남아 있어야 하므로 한 곳에 모아 둔다.
 *
 * SettingsScreen 이 사본을 고치고, '저장하기' 를 누를 때만 여기로 옮겨 적는다.
 * 값은 메모리에만 둔다. 프로그램을 다시 켜도 남기려면 Preferences 로 옮겨야 한다.
 */
public final class Settings {

	/** 조작키 표의 줄 이름. 순서가 keys 의 줄 순서다. */
	public static final String[] ACTIONS = { "왼쪽 이동", "오른쪽 이동", "시계 회전", "반시계 회전", "소프트 드롭", "하드 드롭", "홀드" };

	/** ACTIONS 의 줄 번호. GameKeys 가 이 번호로 동작을 연결한다. */
	public static final int LEFT = 0, RIGHT = 1, ROTATE_CW = 2, ROTATE_CCW = 3, SOFT_DROP = 4, HARD_DROP = 5, HOLD = 6;

	/**
	 * [동작][0] = 1p, [동작][1] = 2p.
	 *
	 * 한 화면 2인에서 손이 겹치지 않게 1p 는 왼손(WASD 쪽, 왼쪽 보드), 2p 는 오른손(방향키 쪽, 오른쪽 보드)만 쓴다.
	 *   1p: A/D 이동, W 시계 회전, Q 반시계 회전, S 소프트 드롭, E 하드 드롭, R 홀드
	 *   2p: ←/→ 이동, ↑ 시계 회전, / 반시계 회전, ↓ 소프트 드롭, Enter 하드 드롭, . 홀드
	 * 혼자 할 때는 예전 배치(방향키, Z 반시계, X 시계, C·Shift 홀드, Space 하드 드롭)도 보조 키로 된다. (ui.GameKeys)
	 */
	public static final int[][] DEFAULT_KEYS = {
			{ KeyEvent.VK_A, KeyEvent.VK_LEFT },
			{ KeyEvent.VK_D, KeyEvent.VK_RIGHT },
			{ KeyEvent.VK_W, KeyEvent.VK_UP },
			{ KeyEvent.VK_Q, KeyEvent.VK_SLASH },
			{ KeyEvent.VK_S, KeyEvent.VK_DOWN },
			{ KeyEvent.VK_E, KeyEvent.VK_ENTER },
			{ KeyEvent.VK_R, KeyEvent.VK_PERIOD },
	};

	public static final int DEFAULT_VOLUME = 70;
	public static final boolean DEFAULT_GHOST = true, DEFAULT_GRID = false;

	/** 랭킹에 적을 이름. 게임이 끝날 때 물어보고, 다음 판의 기본값으로 남는다. */
	public static String playerName = System.getProperty("user.name", "PLAYER");

	public static int sfx = DEFAULT_VOLUME;   // 배경음악은 없다. 효과음 볼륨만 쓴다
	public static boolean ghost = DEFAULT_GHOST, grid = DEFAULT_GRID;
	public static int[][] keys = copy(DEFAULT_KEYS);

	private Settings() {
	}

	/** 줄까지 따로 떼어 복사한다. 기본값 표가 같이 바뀌면 안 된다. */
	public static int[][] copy(int[][] src) {
		int[][] out = new int[src.length][];
		for (int i = 0; i < src.length; i++)
			out[i] = src[i].clone();
		return out;
	}

	/** 자가 점검. java -ea 로 실행한다. */
	public static void main(String[] args) {
		int[][] mine = copy(DEFAULT_KEYS);
		mine[0][0] = KeyEvent.VK_Z;
		assert DEFAULT_KEYS[0][0] == KeyEvent.VK_A : "사본을 고치면 기본값도 바뀐다";
		assert keys.length == ACTIONS.length : "조작키 줄 수가 동작 수와 다르다";
		System.out.println("ok");
	}
}
