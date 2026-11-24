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
		comboCountsStraightClears();
		feverFiresOnceAndBoostsScore();
		clearedRowDropsStackAbove();
		rotationIsBlockedByStackAndWalls();
		gimmickKeepsThreeAndIgnoresSameEffect();
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
		for (int i = 0; i < 3; i++)
			g.getGimmick().onSpawn();                // 블록 3개마다 1개씩 막으므로 3번째 블록에서 막힌다

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

	/** 연속으로 줄을 지우면 콤보가 1씩 쌓이고, 못 지우면 끊긴다. */
	static void comboCountsStraightClears() {
		GameBoard g = new GameBoard();
		g.spawn();
		for (int i = 0; i < 3; i++) {
			fillBottomRow(g);
			g.hardDrop();           // 꽉 찬 줄을 찾아 깜빡임만 걸어 둔다
			g.finishClear();
			check(g.getCombo() == i, (i + 1) + "번째 연속 삭제인데 콤보가 " + g.getCombo());
			g.spawn();
		}
		g.hardDrop();               // 줄을 못 지운 채 놓으면 콤보가 끊긴다
		check(g.getCombo() < 0, "줄을 못 지웠는데 콤보가 남음: " + g.getCombo());
	}

	/**
	 * [피버] 콤보가 기준에 닿으면 한 번만 터지고, 터진 뒤에는 점수가 두 배가 된다.
	 * 콤보가 더 이어져도 다시 터지지 않아야 한다. (점수 배수가 겹치면 4배가 된다)
	 */
	static void feverFiresOnceAndBoostsScore() {
		GameBoard g = new GameBoard();
		g.spawn();

		int fired = 0;
		for (int i = 0; i < GameBoard.FEVER_COMBO + 3; i++) {
			fillBottomRow(g);
			g.hardDrop();
			g.finishClear();
			if (g.takeFever()) {
				fired++;
				g.setNext(Tetrominoes.LineShape);
				g.getGimmick().apply(Attack.SCORE_X2);   // 화면이 하는 일을 대신 한다
				check(g.nextType() == Tetrominoes.LineShape,
						"고른 블록이 다음 블록으로 안 들어감: " + g.nextType());
			}
			g.spawn();
		}
		check(fired == 1, "피버가 " + fired + "번 터짐 (한 번이어야 함)");

		int before = g.getScore();
		g.addScore(100);
		check(g.getScore() - before == 200, "피버 중인데 점수가 두 배가 아님: " + (g.getScore() - before));
	}

	/** 줄 삭제: 꽉 찬 줄이 사라지고, 그 위에 쌓인 블록이 한 칸 내려와야 한다. */
	static void clearedRowDropsStackAbove() {
		GameBoard g = new GameBoard();
		g.spawn();
		for (int x = 0; x < GameBoard.WIDTH; x++)
			g.field().set(x, 0, Tetrominoes.SquareShape, 0);
		g.field().set(0, 1, Tetrominoes.TShape, 3);      // 맨 아랫줄 바로 위에 표시용 블록
		g.hardDrop();                                    // 놓는 순간 꽉 찬 줄에 깜빡임이 걸린다
		check(g.isClearing(), "꽉 찬 줄이 있는데 깜빡임이 안 걸림");
		g.finishClear();
		check(!g.isClearing(), "줄을 지운 뒤에도 깜빡임이 남음");
		check(g.getLinesRemoved() == 1, "지운 줄 수가 1이 아님: " + g.getLinesRemoved());
		check(g.shapeAt(0, 0) == Tetrominoes.TShape && g.colorAt(0, 0) == 3,
				"위에 있던 블록이 모양·색과 함께 내려오지 않음: " + g.shapeAt(0, 0));
		check(g.shapeAt(1, 0) == Tetrominoes.NoShape, "지워진 줄의 다른 칸이 남아 있음");
	}

	/** 회전 충돌: 좁은 틈에서는 어느 쪽으로 밀어도 안 들어가므로 회전이 거절되고 블록은 그대로여야 한다. */
	static void rotationIsBlockedByStackAndWalls() {
		Playfield f = new Playfield(GameBoard.WIDTH, GameBoard.HEIGHT);
		ActivePiece open = new ActivePiece();
		open.begin(Tetrominoes.LineShape, 0);
		open.moveToSpawn(f.width(), f.height());
		check(open.rotateRight(f), "빈 판에서 회전이 안 됨");

		for (int y = 0; y < 10; y++)
			for (int x = 0; x < f.width(); x++)
				if (x != 5)
					f.set(x, y, Tetrominoes.SquareShape, 0);   // 5번 칸만 비운 좁은 틈
		ActivePiece p = new ActivePiece();
		p.begin(Tetrominoes.LineShape, 0);
		p.moveToSpawn(f.width(), f.height());
		check(p.move(f, -1, 0), "틈 위로 옮기지 못함");
		while (p.move(f, 0, -1)) {
			// 틈 바닥까지 내린다
		}
		int x = p.x();
		int y = p.y();
		check(x == 5 && y < 10, "틈 안으로 내려가지 못함: x=" + x + ", y=" + y);
		check(!p.rotateRight(f) && !p.rotateLeft(f), "좁은 틈인데 회전이 됨");
		check(p.x() == x && p.y() == y && p.type() == Tetrominoes.LineShape,
				"회전이 거절됐는데 블록이 변함");
	}

	/** 기믹: 같은 효과는 한 번만 켜지고, 동시에 3개까지만 켜지며 넘으면 가장 오래된 것이 꺼진다. */
	static void gimmickKeepsThreeAndIgnoresSameEffect() {
		GameBoard g = new GameBoard();
		Gimmick gm = g.getGimmick();

		SpeedUp speed = new SpeedUp();
		gm.add(speed);
		gm.add(speed);
		check(gm.speedFactor() == 0.5, "같은 기믹을 두 번 넣었는데 배수가 겹침: " + gm.speedFactor());

		gm.add(new ScoreBoost());
		gm.add(new TwinPieces());
		check(gm.speedFactor() == 0.5 && gm.scoreFactor() == 2.0 && gm.doublesPiece(),
				"3개까지는 모두 켜져 있어야 함");

		gm.add(new ReverseKeys());                       // 4번째: 가장 오래된 SpeedUp 이 꺼진다
		check(gm.speedFactor() == 1.0, "가득 찼는데 가장 오래된 기믹이 안 꺼짐");
		check(gm.scoreFactor() == 2.0 && gm.doublesPiece()
				&& gm.remap(Gimmick.Input.LEFT) == Gimmick.Input.RIGHT, "나중에 켠 기믹이 같이 꺼짐");
	}

	/** 맨 아랫줄을 꽉 채운다. 다음 블록을 놓으면 그 줄이 지워진다. */
	static void fillBottomRow(GameBoard g) {
		for (int x = 0; x < GameBoard.WIDTH; x++)
			g.field().set(x, GameBoard.HEIGHT - 1, Tetrominoes.SquareShape, 0);
	}

	static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}
}
