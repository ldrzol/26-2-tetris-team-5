package kr.ac.jbnu.se.tetris.game;

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

	/**
	 * 칸들의 상대 좌표. 보통은 네 칸이지만, joinedWith() 로 두 블록을 붙이면 여덟 칸이 된다.
	 * 그래서 칸 수를 4로 박지 말고 size() 를 쓴다.
	 */
	private int[][] coords = new int[4][2];

	public Shape() {
		setShape(Tetrominoes.NoShape);
	}

	/** 이 블록이 몇 칸짜리인지. */
	public int size() {
		return coords.length;
	}

	public void setShape(Tetrominoes shape) {
		int[][] table = COORDS[shape.ordinal()];
		coords = new int[table.length][2];
		for (int i = 0; i < table.length; i++) {
			coords[i][0] = table[i][0];
			coords[i][1] = table[i][1];
		}
		pieceShape = shape;
	}

	public void setRandomShape() {
		setShape(Tetrominoes.values()[RANDOM.nextInt(PICKABLE) + 1]);
	}

	/**
	 * 다른 블록을 dx 칸 옆에 붙여 한 덩어리로 만든다. (블록 2개가 동시에 떨어지는 아이템)
	 *
	 * 모양 이름은 앞 블록 것을 쓴다. 화면에서 색을 고르는 데만 쓰이기 때문이다.
	 */
	public Shape joinedWith(Shape other, int dx) {
		Shape result = new Shape();
		result.pieceShape = pieceShape;
		result.coords = new int[size() + other.size()][2];
		for (int i = 0; i < size(); i++) {
			result.coords[i][0] = x(i);
			result.coords[i][1] = y(i);
		}
		for (int i = 0; i < other.size(); i++) {
			result.coords[size() + i][0] = other.x(i) + dx;
			result.coords[size() + i][1] = other.y(i);
		}
		return result;
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
		for (int i = 0; i < size(); i++) {
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

	/**
	 * dir = +1 반시계, -1 시계. 네모 블록은 돌려도 똑같으므로 그대로 돌려준다.
	 * (붙인 덩어리는 네모가 둘이라도 모양이 달라지므로 그냥 돌린다)
	 */
	private Shape rotate(int dir) {
		if (pieceShape == Tetrominoes.SquareShape && size() == 4)
			return this;

		Shape result = new Shape();
		result.pieceShape = pieceShape;
		result.coords = new int[size()][2];
		for (int i = 0; i < size(); ++i) {
			result.coords[i][0] = dir * y(i);
			result.coords[i][1] = -dir * x(i);
		}
		return result;
	}

	/** 자가 점검. java -ea 로 실행한다. */
	public static void main(String[] args) {
		Shape t = new Shape();
		t.setShape(Tetrominoes.TShape);
		assert t.size() == 4 : "기본 블록은 네 칸이다";

		Shape line = new Shape();
		line.setShape(Tetrominoes.LineShape);

		Shape twin = t.joinedWith(line, 3);
		assert twin.size() == 8 : "붙인 덩어리는 여덟 칸이다";
		assert twin.getShape() == Tetrominoes.TShape : "모양 이름은 앞 블록 것을 쓴다";
		assert t.size() == 4 && line.size() == 4 : "붙여도 원래 블록은 그대로여야 한다";

		// 붙인 칸이 dx 만큼 옆으로 간다
		assert twin.x(4) == line.x(0) + 3 && twin.y(4) == line.y(0) : "붙인 블록의 자리가 어긋났다";

		assert twin.rotateRight().size() == 8 : "덩어리도 칸 수를 지키며 돌아야 한다";

		// 네모를 혼자 돌리면 그대로, 붙여 놓으면 실제로 돌아야 한다
		Shape square = new Shape();
		square.setShape(Tetrominoes.SquareShape);
		assert square.rotateRight() == square : "네모 한 개는 돌려도 그대로다";
		assert square.joinedWith(square, 3).rotateRight().size() == 8 : "붙인 네모는 돌아야 한다";

		// 네 번 돌리면 제자리로 돌아온다
		Shape back = t.rotateRight().rotateRight().rotateRight().rotateRight();
		for (int i = 0; i < 4; i++)
			assert back.x(i) == t.x(i) && back.y(i) == t.y(i) : "네 번 돌리면 제자리여야 한다";

		System.out.println("ok");
	}
}
