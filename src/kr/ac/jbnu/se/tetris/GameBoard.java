package kr.ac.jbnu.se.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameBoard {

    public static final int WIDTH = 10;   // 게임판 가로 칸 수
    public static final int HEIGHT = 22;  // 게임판 세로 칸 수

    private final Tetrominoes[] cells = new Tetrominoes[WIDTH * HEIGHT]; // 쌓여 있는 블록들
    private Shape curPiece = new Shape();   // 현재 조작 중인 블록
    private int curX = 0;                   // 현재 블록 기준점 x
    private int curY = 0;                   // 현재 블록 기준점 y
    private int linesRemoved = 0;           // 지운 줄 수
    private boolean gameOver = false;       // 게임 오버 여부
    private boolean spawnPending = false;   // 줄이 지워져서 새 블록을 다음 틱에 만들어야 하는 상태
    private final List<Integer> flashRows = new ArrayList<>(); // 깜빡이는 중인(곧 지워질) 줄들

    public GameBoard() {
        reset();
    }

    /** 보드를 비우고 상태를 초기화한다. (첫 블록은 spawn()으로 따로 만든다) */
    public void reset() {
        for (int i = 0; i < cells.length; ++i)
            cells[i] = Tetrominoes.NoShape;
        curPiece.setShape(Tetrominoes.NoShape);
        linesRemoved = 0;
        gameOver = false;
        spawnPending = false;
        flashRows.clear();
    }

    /** 새 블록 생성. 시작 위치에 놓을 수 없으면 게임 오버가 되고 false를 반환한다. */
    public boolean spawn() {
        curPiece.setRandomShape();
        curX = WIDTH / 2 + 1;
        curY = HEIGHT - 1 + curPiece.minY();

        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Tetrominoes.NoShape);
            gameOver = true;
            return false;
        }
        return true;
    }

    /**
     * 타이머 한 틱 처리. 줄 삭제 직후라면 새 블록을 만들고, 아니면 한 칸 내린다.
     */
    public void tick() {
        if (gameOver || isClearing())   // 줄이 깜빡이는 동안은 게임을 멈춘다
            return;
        if (spawnPending) {
            spawnPending = false;
            spawn();
        } else {
            moveDown();
        }
    }

    public boolean moveLeft() {
        return tryMove(curPiece, curX - 1, curY);
    }

    public boolean moveRight() {
        return tryMove(curPiece, curX + 1, curY);
    }

    public boolean rotateLeft() {
        return tryMove(curPiece.rotateLeft(), curX, curY);
    }

    public boolean rotateRight() {
        return tryMove(curPiece.rotateRight(), curX, curY);
    }

    public boolean moveDown() {
        if (tryMove(curPiece, curX, curY - 1))
            return true;
        lockPiece();
        return false;
    }

    public void hardDrop() {
        while (moveDown()) {
        }
    }

    // ---- 화면 쪽이 읽어 가는 조회용 메서드 ----

    public Tetrominoes shapeAt(int x, int y) {
        return cells[y * WIDTH + x];
    }

    public Tetrominoes currentType() {
        return curPiece.getShape();
    }

    /** 현재 블록 i번째 사각형의 보드상 x 좌표 */
    public int pieceX(int i) {
        return curX + curPiece.x(i);
    }

    /** 현재 블록 i번째 사각형의 보드상 y 좌표 */
    public int pieceY(int i) {
        return curY - curPiece.y(i);
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** 줄 삭제 이펙트(깜빡임) 진행 중인지 */
    public boolean isClearing() {
        return !flashRows.isEmpty();
    }

    /** 해당 줄이 깜빡이는 중인 줄인지 */
    public boolean isFlashRow(int row) {
        return flashRows.contains(row);
    }

    // ---- 내부 로직 ----

    /** (newX, newY)로 옮길 수 있는지 검사하고, 가능하면 실제로 이동한다. repaint는 하지 않는다. */
    private boolean tryMove(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; ++i) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT)
                return false;
            if (shapeAt(x, y) != Tetrominoes.NoShape)
                return false;
        }

        curPiece = newPiece;
        curX = newX;
        curY = newY;
        return true;
    }

    /** 현재 블록을 보드에 기록하고 줄을 지운다. 줄이 지워졌으면 새 블록은 다음 틱에 만든다. */
    private void lockPiece() {
        for (int i = 0; i < 4; ++i) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            cells[y * WIDTH + x] = curPiece.getShape();
        }

        // 꽉 찬 줄은 바로 지우지 않고 기록만 해 둔다. (Board가 깜빡인 뒤 finishClear()를 호출)
        for (int i = 0; i < HEIGHT; ++i) {
            if (isLineFull(i))
                flashRows.add(i);
        }

        if (isClearing()) {
            curPiece.setShape(Tetrominoes.NoShape);
        } else {
            spawn();
        }
    }

    /** 깜빡임이 끝난 뒤 호출. 기록해 둔 줄을 실제로 지우고, 새 블록은 다음 틱에 만든다. */
    public void finishClear() {
        Collections.sort(flashRows, Collections.reverseOrder());   // 높은 줄부터 당겨야 아래 줄 번호가 안 밀린다
        for (int row : flashRows)
            downLine(row);
        linesRemoved += flashRows.size();
        flashRows.clear();
        spawnPending = true;
    }

    private boolean isLineFull(int row){
        for (int j = 0; j < WIDTH; ++j) {
            if (shapeAt(j, row)==Tetrominoes.NoShape){
                return false;
            }
        }
        return true;
    }

    public void downLine(int row) {
        System.arraycopy(cells, (row + 1) * WIDTH,   // 원본: row+1 줄부터
                cells, row * WIDTH,          // 대상: row 줄부터
                (HEIGHT - 1 - row) * WIDTH); // 복사할 칸 수
        Arrays.fill(cells, (HEIGHT - 1) * WIDTH, HEIGHT * WIDTH, Tetrominoes.NoShape);
    }
    public void upLine(int row) {
        System.arraycopy(cells, (row - 1) * WIDTH,
                cells, row * WIDTH,
                (HEIGHT - 1 + row) * WIDTH);
    }
}