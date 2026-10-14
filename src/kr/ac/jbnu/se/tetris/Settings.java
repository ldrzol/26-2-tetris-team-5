package kr.ac.jbnu.se.tetris;

import java.awt.event.KeyEvent;

/**
 * 환경 설정 값. 화면을 새로 만들어도 남아 있어야 하므로 한 곳에 모아 둔다.
 *
 * SettingsScreen 이 사본을 고치고, '저장하기' 를 누를 때만 여기로 옮겨 적는다.
 * 값은 메모리에만 둔다. 프로그램을 다시 켜도 남기려면 Preferences 로 옮겨야 한다.
 */
final class Settings {

	/** 조작키 표의 줄 이름. 순서가 keys 의 줄 순서다. */
	static final String[] ACTIONS = { "왼쪽 이동", "오른쪽 이동", "시계 회전", "반시계 회전", "소프트 드롭", "하드 드롭", "홀드" };

	/** ACTIONS 의 줄 번호. GameKeys 가 이 번호로 동작을 연결한다. */
	static final int LEFT = 0, RIGHT = 1, ROTATE_CW = 2, ROTATE_CCW = 3, SOFT_DROP = 4, HARD_DROP = 5, HOLD = 6;

	/**
	 * [동작][0] = 1p, [동작][1] = 2p. 1p 는 가장 널리 쓰이는 배치다.
	 *   ←/→ 이동, ↑ 시계 회전, Z 반시계 회전, ↓ 소프트 드롭, Space 하드 드롭, C 홀드
	 */
	static final int[][] DEFAULT_KEYS = {
			{ KeyEvent.VK_LEFT, KeyEvent.VK_A },
			{ KeyEvent.VK_RIGHT, KeyEvent.VK_D },
			{ KeyEvent.VK_UP, KeyEvent.VK_W },
			{ KeyEvent.VK_Z, KeyEvent.VK_Q },
			{ KeyEvent.VK_DOWN, KeyEvent.VK_S },
			{ KeyEvent.VK_SPACE, KeyEvent.VK_E },
			{ KeyEvent.VK_C, KeyEvent.VK_R },
	};

	static final int DEFAULT_VOLUME = 70;
	static final boolean DEFAULT_GHOST = true, DEFAULT_GRID = false;

	/** 랭킹에 적을 이름. 게임이 끝날 때 물어보고, 다음 판의 기본값으로 남는다. */
	static String playerName = System.getProperty("user.name", "PLAYER");

	static int sfx = DEFAULT_VOLUME;   // 배경음악은 없다. 효과음 볼륨만 쓴다
	static boolean ghost = DEFAULT_GHOST, grid = DEFAULT_GRID;
	static int[][] keys = copy(DEFAULT_KEYS);

	private Settings() {
	}

	/** 줄까지 따로 떼어 복사한다. 기본값 표가 같이 바뀌면 안 된다. */
	static int[][] copy(int[][] src) {
		int[][] out = new int[src.length][];
		for (int i = 0; i < src.length; i++)
			out[i] = src[i].clone();
		return out;
	}

	/** 자가 점검. java -ea 로 실행한다. */
	public static void main(String[] args) {
		int[][] mine = copy(DEFAULT_KEYS);
		mine[0][0] = KeyEvent.VK_Z;
		assert DEFAULT_KEYS[0][0] == KeyEvent.VK_LEFT : "사본을 고치면 기본값도 바뀐다";
		assert keys.length == ACTIONS.length : "조작키 줄 수가 동작 수와 다르다";
		System.out.println("ok");
	}
}
