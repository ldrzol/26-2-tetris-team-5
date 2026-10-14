package kr.ac.jbnu.se.tetris;

import java.util.List;

/**
 * 머지 때 뒤섞였던 hold / next 로직이 하나로 맞물려 도는지 확인하는 자가 점검.
 * 실행: java -ea -cp out kr.ac.jbnu.se.tetris.GameBoardSelfCheck
 */
public class GameBoardSelfCheck {

	public static void main(String[] args) {
		sevenBagHasNoDuplicates();
		previewMatchesWhatSpawns();
		holdSwapsAndLocksOncePerPiece();
		bothGetterFamiliesAgree();
		levelFollowsScore();
		clearStackKeepsScore();
		System.out.println("GameBoardSelfCheck: 통과");
	}

	/** 7bag: 앞의 7개는 서로 다른 블록이어야 한다. */
	static void sevenBagHasNoDuplicates() {
		GameBoard g = new GameBoard();
		List<Tetrominoes> bag = g.peekNext(7);
		check(bag.size() == 7, "큐에서 7개를 못 꺼냄: " + bag.size());
		check(bag.stream().distinct().count() == 7, "7bag 안에 중복이 있음: " + bag);
		check(!bag.contains(Tetrominoes.NoShape) && !bag.contains(Tetrominoes.GarbageShape),
				"큐에 안 나와야 할 블록이 섞임: " + bag);
	}

	/** Nextpanel 이 보여 주는 예고와 실제로 나오는 블록이 같아야 한다. (머지로 어긋났던 부분) */
	static void previewMatchesWhatSpawns() {
		GameBoard g = new GameBoard();
		for (int i = 0; i < 20; i++) {
			Tetrominoes promised = g.nextType();
			g.spawn();
			check(g.currentType() == promised,
					i + "번째: 예고는 " + promised + " 인데 실제로는 " + g.currentType() + " 가 나옴");
		}
	}

	/** 홀드는 블록 하나당 한 번. 두 번째 호출은 거절되어야 한다. */
	static void holdSwapsAndLocksOncePerPiece() {
		GameBoard g = new GameBoard();
		g.spawn();
		Tetrominoes first = g.currentType();

		check(g.hold(), "첫 홀드가 거절됨");
		check(g.holdType() == first, "홀드함에 " + first + " 가 안 들어감: " + g.holdType());
		check(!g.hold(), "같은 블록으로 홀드를 두 번 쓸 수 있음");

		g.hardDrop();                                   // 바닥까지 내려 굳히면 다음 블록이 나온다
		check(!g.isGameOver(), "바닥에 한 번 쌓았는데 게임 오버");

		Tetrominoes current = g.currentType();
		check(g.hold(), "새 블록이 나왔는데 홀드가 안 풀림");
		check(g.holdType() == current, "홀드함이 " + current + " 로 안 바뀜: " + g.holdType());
		check(g.currentType() == first, "보관해 둔 " + first + " 가 안 나옴: " + g.currentType());
	}

	/** choi 쪽(holdType)과 donggeun 쪽(getHoldType) 게터가 같은 상태를 봐야 한다. */
	static void bothGetterFamiliesAgree() {
		GameBoard g = new GameBoard();
		g.spawn();
		check(g.getHoldType() == g.holdType(), "두 홀드 게터가 다른 값을 봄");
		check(!g.isHoldUsed(), "새 블록인데 홀드가 이미 쓰인 걸로 나옴");
		g.hold();
		check(g.getHoldType() == g.holdType(), "홀드 후 두 게터가 어긋남");
		check(g.isHoldUsed(), "홀드했는데 isHoldUsed 가 false");
	}

	/** 레벨은 누적 점수(levelGoal)를 넘길 때마다 오르고 MAX_LEVEL 에서 멈춘다. */
	static void levelFollowsScore() {
		check(GameBoard.levelGoal(1) == 1000, "레벨 1 목표가 1000점이 아님: " + GameBoard.levelGoal(1));
		check(GameBoard.levelGoal(2) == 3000, "레벨 2 목표가 3000점이 아님: " + GameBoard.levelGoal(2));

		GameBoard g = new GameBoard();
		check(g.getLevel() == 1, "0점인데 레벨이 1이 아님: " + g.getLevel());

		g.addScore(GameBoard.levelGoal(1) - 1);
		check(g.getLevel() == 1, "목표 직전인데 레벨이 올라감: " + g.getLevel());

		g.addScore(1);
		check(g.getLevel() == 2, "목표를 넘겼는데 레벨이 안 오름: " + g.getLevel());

		g.addScore(GameBoard.levelGoal(GameBoard.MAX_LEVEL) * 10);
		check(g.getLevel() == GameBoard.MAX_LEVEL, "레벨 상한을 넘김: " + g.getLevel());
	}

	/** 레벨 클리어로 판을 비워도 점수·레벨·지운 줄 수는 남아 있어야 한다. */
	static void clearStackKeepsScore() {
		GameBoard g = new GameBoard();
		g.spawn();
		g.addScore(1200);
		g.hardDrop();                       // 바닥에 한 덩어리 쌓는다

		int before = g.getScore(), level = g.getLevel();
		g.clearStack();

		for (int y = 0; y < GameBoard.HEIGHT; y++)
			for (int x = 0; x < GameBoard.WIDTH; x++)
				check(g.shapeAt(x, y) == Tetrominoes.NoShape, "판을 비웠는데 블록이 남음");
		check(g.getScore() == before, "판을 비웠더니 점수가 바뀜: " + g.getScore());
		check(g.getLevel() == level, "판을 비웠더니 레벨이 바뀜: " + g.getLevel());
		check(g.currentType() != Tetrominoes.NoShape, "판을 비운 뒤 새 블록이 안 나옴");
	}

	static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}
}
