package kr.ac.jbnu.se.tetris;

import java.util.Random;

public class Shape {

	/** Tetrominoes 순서대로 적은 네 칸의 상대 좌표. 모양을 늘리면 여기도 한 줄 늘려야 한다. */
	private static final int[][][] COORDS = {
			{ { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },      // NoShape
			{ { 0, -1 }, { 0, 0 }, { -1, 0 }, { -1, 1 } },   // ZShape
			{ { 0, -1 }, { 0, 0 }, { 1, 0 }, { 1, 1 } },     // SShape
			{ { 0, -1 }, { 0, 0 }, { 0, 1 }, { 0, 2 } },     // LineShape
			{ { -1, 0 }, { 0, 0 }, { 1, 0 }, { 0, 1 } },     // TShape
			{ { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } },      // SquareShape
			{ { -1, -1 }, { 0, -1 }, { 0, 0 }, { 0, 1 } },   // LShape
			{ { 1, -1 }, { 0, -1 }, { 0, 0 }, { 0, 1 } },    // MirroredLShape
			{ { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },      // GarbageShape (쌓인 칸 전용, 조작하지 않는다)
	};

	/** 뽑을 수 있는 모양은 ZShape(1) ~ MirroredLShape(7). */
	private static final int PICKABLE = 7;

	private static final Random RANDOM = new Random();

	private Tetrominoes pieceShape;
	private final int[][] coords = new int[4][2];

	public Shape() {
		setShape(Tetrominoes.NoShape);
	}

	public void setShape(Tetrominoes shape) {
		int[][] table = COORDS[shape.ordinal()];
		for (int i = 0; i < 4; i++) {
			coords[i][0] = table[i][0];
			coords[i][1] = table[i][1];
		}
		pieceShape = shape;
	}

	public void setRandomShape() {
		setShape(Tetrominoes.values()[RANDOM.nextInt(PICKABLE) + 1]);
	}

	public int x(int index) {
		return coords[index][0];
	}

	public int y(int index) {
		return coords[index][1];
	}

	public Tetrominoes getShape() {
		return pieceShape;
	}

	public int minY() {
		int m = coords[0][1];
		for (int i = 0; i < 4; i++) {
			m = Math.min(m, coords[i][1]);
		}
		return m;
	}

	public Shape rotateLeft() {
		return rotate(1);
	}

	public Shape rotateRight() {
		return rotate(-1);
	}

	/** dir = +1 반시계, -1 시계. 네모 블록은 돌려도 똑같으므로 그대로 돌려준다. */
	private Shape rotate(int dir) {
		if (pieceShape == Tetrominoes.SquareShape)
			return this;

		Shape result = new Shape();
		result.pieceShape = pieceShape;
		for (int i = 0; i < 4; ++i) {
			result.coords[i][0] = dir * y(i);
			result.coords[i][1] = -dir * x(i);
		}
		return result;
	}
}
