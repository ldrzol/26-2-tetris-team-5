package kr.ac.jbnu.se.tetris.game;

/**
 * 지금 조작 중인 블록 하나. 모양, 기준점(x, y), 색을 함께 갖고 다니며
 * 판(Playfield) 위에서 옮기고 돌리고 시작 위치에 놓는 규칙을 안다.
 */
class ActivePiece {

    /** 블록 덩어리가 가운데에 안 들어갈 때 옆으로 밀어 보는 칸 수. */
    private static final int[] SPAWN_KICKS = { 0, -1, 1, -2, 2, -3, 3 };

    /** 회전했을 때 벽이나 쌓인 블록에 걸리면 옆으로 밀어 보는 칸 수. */
    private static final int[] ROTATE_KICKS = { 0, -1, 1, -2, 2 };

    private Shape shape = new Shape();
    private int x = 0;
    private int y = 0;
    private int color = 0;

    Shape shape() {
        return shape;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    int color() {
        return color;
    }

    Tetrominoes type() {
        return shape.getShape();
    }

    /** 조작할 블록이 없는 상태인지. */
    boolean isNone() {
        return shape.getShape() == Tetrominoes.NoShape;
    }

    void clear() {
        shape.setShape(Tetrominoes.NoShape);
    }

    /** 몇 칸짜리인지. 보통 4, 2개 동시 아이템이 걸리면 8. */
    int count() {
        return shape.size();
    }

    /** i번째 사각형의 보드상 x 좌표. */
    int cellX(int i) {
        return x + shape.x(i);
    }

    /** i번째 사각형의 보드상 y 좌표. */
    int cellY(int i) {
        return y - shape.y(i);
    }

    /** 새 블록으로 바꾼다. 위치는 moveToSpawn 으로 따로 정한다. */
    void begin(Tetrominoes type, int newColor) {
        shape.setShape(type);
        color = newColor;
    }

    /** 다른 블록을 옆에 붙여 한 덩어리로 만든다. (2개 동시 아이템) */
    void joinWith(Shape other, int gap) {
        shape = shape.joinedWith(other, gap);
    }

    /** 판 위쪽 가운데로 기준점을 옮긴다. */
    void moveToSpawn(int boardWidth, int boardHeight) {
        x = boardWidth / 2 + 1;
        y = boardHeight - 1 + shape.minY();
    }

    /**
     * 시작 위치에 놓을 수 있는지 본다. 놓을 수 있으면 true.
     *
     * 덩어리가 커서 벽을 넘을 때만 옆으로 밀어 본다. 판 안에는 들어가는데
     * 쌓인 블록에 막힌 거라면 그대로 실패여야 한다. (게임 오버)
     */
    boolean settleAtSpawn(Playfield field) {
        if (field.fitsInWalls(shape, x, y))
            return tryMove(field, shape, x, y);
        return kickInto(field, shape, SPAWN_KICKS);
    }

    /** (dx, dy) 만큼 옮길 수 있으면 옮긴다. */
    boolean move(Playfield field, int dx, int dy) {
        return tryMove(field, shape, x + dx, y + dy);
    }

    boolean rotateLeft(Playfield field) {
        return kickInto(field, shape.rotateLeft(), ROTATE_KICKS);
    }

    boolean rotateRight(Playfield field) {
        return kickInto(field, shape.rotateRight(), ROTATE_KICKS);
    }

    /** 조작 중인 블록을 다른 모양으로 바꾼다. 안 들어가면 좌우로 밀어 보고, 그래도 안 되면 false. */
    boolean replaceWith(Playfield field, Tetrominoes type) {
        Shape replacement = new Shape();
        replacement.setShape(type);
        return kickInto(field, replacement, SPAWN_KICKS);
    }

    /** 줄이 올라온 뒤 겹치면 한 칸 위로 올린다. 그래도 안 되면 false. */
    boolean liftIfBlocked(Playfield field) {
        if (isNone())
            return true;   // 줄 삭제 중에는 조작 블록이 없다
        return move(field, 0, 0) || move(field, 0, 1);
    }

    /** 현재 위치에 블록을 판에 굳힌다. */
    void stampOn(Playfield field) {
        for (int i = 0; i < shape.size(); ++i)
            field.set(cellX(i), cellY(i), shape.getShape(), color);   // 새로 놓인 칸은 나이 0
    }

    /** 평평한 배열(y * width + x)에 이 블록을 찍어 넣는다. 판 밖 칸은 건너뛴다. */
    void writeInto(Tetrominoes[] shapes, int[] colors, int width, int height) {
        if (isNone())
            return;
        for (int i = 0; i < shape.size(); i++) {
            int cx = cellX(i);
            int cy = cellY(i);
            if (cx < 0 || cx >= width || cy < 0 || cy >= height)
                continue;
            shapes[cy * width + cx] = shape.getShape();
            colors[cy * width + cx] = color;
        }
    }

    private boolean kickInto(Playfield field, Shape candidate, int[] kicks) {
        for (int dx : kicks) {
            if (tryMove(field, candidate, x + dx, y))
                return true;
        }
        return false;
    }

    private boolean tryMove(Playfield field, Shape candidate, int newX, int newY) {
        if (!field.canPlace(candidate, newX, newY))
            return false;
        shape = candidate;
        x = newX;
        y = newY;
        return true;
    }
}
