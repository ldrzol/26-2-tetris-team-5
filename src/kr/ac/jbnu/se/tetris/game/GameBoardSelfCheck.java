package kr.ac.jbnu.se.tetris.game;

import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.item.Skin;

import java.util.ArrayList;
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
		levelFollowsScore();
		clearStackKeepsScore();
		skinsHaveEveryColor();
		lockedBoardStopsAndResumes();
		twinPieceFallsAsOneBlock();
		scoreBoostDoublesPoints();
		swapReplacesCurrentPiece();
		forceRotateIgnoresNoRotate();
		blockedSpawnEndsGame();
		bombErasesRandomShape();
		System.out.println("GameBoardSelfCheck: 통과");
	}

	/** 7bag: 처음 나오는 7개는 서로 다른 블록이어야 한다. */
	static void sevenBagHasNoDuplicates() {
		GameBoard g = new GameBoard();
		List<Tetrominoes> bag = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			g.spawn();
			bag.add(g.currentType());
		}
		check(bag.stream().distinct().count() == 7, "7bag 안에 중복이 있음: " + bag);
		check(!bag.contains(Tetrominoes.NoShape) && !bag.contains(Tetrominoes.GarbageShape),
				"나오면 안 되는 블록이 섞임: " + bag);
	}

	/** Next 상자에 보여 준 예고와 실제로 나오는 블록이 같아야 한다. (머지로 어긋났던 부분) */
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

	/** 스킨마다 보드가 쓰는 색 수를 다 갖춰야 한다. 모자라면 그리다가 범위를 벗어난다. */
	static void skinsHaveEveryColor() {
		for (Skin skin : Skin.values())
			check(skin.colors.length == GameBoard.COLOR_COUNT,
					skin.label + " 의 색이 " + skin.colors.length + "개뿐이다");
	}

	/** 블록 잠그기: 효과가 도는 동안은 새 블록이 안 나오고, 풀리면 다시 나와야 한다. */
	static void lockedBoardStopsAndResumes() {
		GameBoard g = new GameBoard();
		g.spawn();
		g.getGimmick().apply(Attack.LOCK);

		g.hardDrop();                       // 굳힌 뒤에 다음 블록이 나오면 안 된다
		check(g.currentType() == Tetrominoes.NoShape, "잠겼는데 새 블록이 나옴: " + g.currentType());
		check(!g.isGameOver(), "블록이 안 나온 것을 게임 오버로 처리함");

		g.tick();                           // 잠겨 있는 동안은 틱이 와도 그대로
		check(g.currentType() == Tetrominoes.NoShape, "잠긴 동안 틱으로 블록이 나옴");

		for (int i = 0; i < 6; i++)         // NoSpawn 은 6초짜리다
			g.getGimmick().onSecond();

		g.tick();
		check(g.currentType() != Tetrominoes.NoShape, "효과가 끝났는데 블록이 안 나옴");
	}

	/** 블록 2개: 여덟 칸짜리 덩어리로 나와서 여덟 칸 그대로 굳어야 한다. */
	static void twinPieceFallsAsOneBlock() {
		GameBoard g = new GameBoard();
		g.getGimmick().apply(Attack.TWIN);
		g.spawn();

		check(!g.isGameOver(), "덩어리가 자리를 못 잡고 게임 오버");
		check(g.pieceCount() == 8, "덩어리가 여덟 칸이 아님: " + g.pieceCount());

		g.hardDrop();
		check(countFilled(g) == 8, "굳은 칸이 여덟 개가 아님: " + countFilled(g));

		// 효과가 끝나면 다시 네 칸짜리로 돌아온다
		for (int i = 0; i < 12; i++)
			g.getGimmick().onSecond();
		g.spawn();
		check(g.pieceCount() == 4, "효과가 끝났는데 아직 덩어리로 나옴: " + g.pieceCount());
	}

	/** 점수 2배: 효과가 도는 동안 들어오는 점수가 두 배여야 한다. */
	static void scoreBoostDoublesPoints() {
		GameBoard g = new GameBoard();
		g.addScore(100);
		check(g.getScore() == 100, "평소에 점수가 그대로 안 들어감: " + g.getScore());

		g.getGimmick().apply(Attack.SCORE_X2);
		g.addScore(100);
		check(g.getScore() == 300, "두 배가 안 됨: " + g.getScore());

		for (int i = 0; i < 15; i++)        // ScoreBoost 는 15초짜리다
			g.getGimmick().onSecond();
		g.addScore(100);
		check(g.getScore() == 400, "효과가 끝났는데 아직 두 배임: " + g.getScore());
	}

	/** 블록 교환: 조작 중인 블록이 건네받은 모양으로 바뀌어야 한다. */
	static void swapReplacesCurrentPiece() {
		GameBoard g = new GameBoard();
		g.spawn();

		check(g.replaceCurrentPiece(Tetrominoes.TShape), "교환이 거절됨");
		check(g.currentType() == Tetrominoes.TShape, "블록이 안 바뀜: " + g.currentType());

		check(!g.replaceCurrentPiece(Tetrominoes.NoShape), "빈 모양으로도 바꿔 줌");
		check(g.currentType() == Tetrominoes.TShape, "거절했는데 블록이 바뀜: " + g.currentType());
	}

	/** 회전 고정: 저절로 도는 회전은 '회전 불가' 기믹에 막히지 않아야 한다. */
	static void forceRotateIgnoresNoRotate() {
		GameBoard g = new GameBoard();
		g.spawn();

		// 처음 나오는 블록은 무작위라 꼭대기에 세로로 긴 블록이 안 들어갈 수 있다.
		// 몇 칸 내려 자리를 만들고, 교체가 됐는지까지 확인해야 점검이 흔들리지 않는다.
		for (int i = 0; i < 3; i++)
			g.moveDown();
		check(g.replaceCurrentPiece(Tetrominoes.LineShape), "일자 블록으로 못 바꿈");

		g.getGimmick().apply(Attack.NO_ROTATE);

		int[] before = pieceCoords(g);
		check(!g.rotateRight(), "회전 불가인데 플레이어 회전이 먹힘");
		check(java.util.Arrays.equals(before, pieceCoords(g)), "막혔는데 블록이 돌아감");

		g.forceRotate();
		check(!java.util.Arrays.equals(before, pieceCoords(g)), "저절로 도는 회전까지 막힘");
	}

	/** 새 블록 자리가 쌓인 블록에 막히면 옆으로 비껴 나지 말고 게임 오버여야 한다. */
	static void blockedSpawnEndsGame() {
		for (int i = 0; i < 20; i++) {          // 나오는 모양이 무작위라 여러 번 돌려 본다
			GameBoard g = new GameBoard();
			Tetrominoes[] shapes = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
			java.util.Arrays.fill(shapes, Tetrominoes.NoShape);
			int top = GameBoard.HEIGHT - 1;
			for (int x = 5; x < GameBoard.WIDTH; x++)   // 블록 나오는 자리만 막고 왼쪽은 비워 둔다
				shapes[top * GameBoard.WIDTH + x] = Tetrominoes.GarbageShape;
			g.applyRemoteState(shapes, new int[shapes.length]);

			check(!g.spawn(), "자리가 막혔는데 블록이 나옴: " + g.currentType());
			check(g.isGameOver(), "자리가 막혔는데 게임이 안 끝남 (x=" + g.getCurX() + ")");
		}
	}

	/** 폭탄: 쌓인 칸을 무작위 모양(한 칸~3x3, 일자 4칸)만큼 지워야 한다. */
	static void bombErasesRandomShape() {
		for (int i = 0; i < 30; i++) {
			GameBoard g = new GameBoard();
			Tetrominoes[] shapes = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
			java.util.Arrays.fill(shapes, Tetrominoes.GarbageShape);
			g.applyRemoteState(shapes, new int[shapes.length]);

			int before = countFilled(g);
			check(g.explode(), "쌓인 블록이 가득인데 터뜨릴 자리를 못 찾음");
			int erased = before - countFilled(g);
			check(erased >= 1 && erased <= 9, "지운 칸 수가 폭탄 모양을 벗어남: " + erased);
		}

		GameBoard empty = new GameBoard();
		check(!empty.explode(), "빈 판에서 폭탄이 터졌다고 함");
	}

	/** 판에 쌓여 있는 칸 수. */
	static int countFilled(GameBoard g) {
		int n = 0;
		for (int y = 0; y < GameBoard.HEIGHT; y++)
			for (int x = 0; x < GameBoard.WIDTH; x++)
				if (g.shapeAt(x, y) != Tetrominoes.NoShape)
					n++;
		return n;
	}

	/** 지금 블록이 차지한 자리를 한 줄로 편 것. 돌았는지 비교할 때 쓴다. */
	static int[] pieceCoords(GameBoard g) {
		int[] out = new int[g.pieceCount() * 2];
		for (int i = 0; i < g.pieceCount(); i++) {
			out[i * 2] = g.pieceX(i);
			out[i * 2 + 1] = g.pieceY(i);
		}
		return out;
	}

	static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}
}
