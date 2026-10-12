package kr.ac.jbnu.se.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.Deque;

public class GameBoard {

    public static final int WIDTH = 10;   // 게임판 가로 칸 수
    public static final int HEIGHT = 22;  // 게임판 세로 칸 수

    protected final Tetrominoes[][] cells = new Tetrominoes[HEIGHT][WIDTH]; // 쌓여 있는 블록들
    private Shape curPiece = new Shape();   // 현재 조작 중인 블록
    private int curX = 0;                   // 현재 블록 기준점 x
    private int curY = 0;                   // 현재 블록 기준점 y
    private int linesRemoved = 0;           // 지운 줄 수
    private boolean gameOver = false;       // 게임 오버 여부
    private boolean spawnPending = false;   // 줄이 지워져서 새 블록을 다음 틱에 만들어야 하는 상태
    private final List<Integer> flashRows = new ArrayList<>(); // 깜빡이는 중인(곧 지워질) 줄들
    private final Random rand = new Random();
    private static final int QUEUE_MIN = 7;
    private final Deque<Tetrominoes> queue = new ArrayDeque<>();
    private Tetrominoes holdType = Tetrominoes.NoShape;
    private boolean holdUsed = false;

    public GameBoard() {
        reset();
    }

    /** 보드를 비우고 상태를 초기화한다. (첫 블록은 spawn()으로 따로 만든다) */
    public void reset() {
        for (int i = 0; i < HEIGHT; ++i){
            Arrays.fill(cells[i],Tetrominoes.NoShape);
        }
        curPiece.setShape(Tetrominoes.NoShape);
        linesRemoved = 0;
        gameOver = false;
        spawnPending = false;
        flashRows.clear();
        queue.clear();
        holdType = Tetrominoes.NoShape;
        holdUsed = false;
    }

    private void refillQueue(){
        while(queue.size()<QUEUE_MIN){
            List<Tetrominoes> bag = new ArrayList<>();
            for(Tetrominoes t: Tetrominoes.values()){
                if(t!=Tetrominoes.NoShape && t!=Tetrominoes.GarbageShape){
                    bag.add(t);
                }
            }
            Collections.shuffle(bag, rand);
            queue.addAll(bag);
        }
    }

    private Tetrominoes nextFromQueue(){
        refillQueue();
        Tetrominoes next = queue.pollFirst();
        refillQueue();
        return next;
    }

    public List<Tetrominoes> peekNext(int n){
        refillQueue();
        List<Tetrominoes> result = new ArrayList<>();
        for(Tetrominoes t : queue){
            if(result.size()>=n){
                break;
            }
            result.add(t);
        }
        return result;
    }

    /** 새 블록 생성. 시작 위치에 놓을 수 없으면 게임 오버가 되고 false를 반환한다. */
    public boolean spawn() {
        return spawnPiece(nextFromQueue());
    }

    private boolean spawnPiece(Tetrominoes type){
        curPiece.setShape(type);
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
        return tryRotate(curPiece.rotateLeft());
    }

    public boolean rotateRight() {
        return tryRotate(curPiece.rotateRight());
    }

    private boolean tryRotate(Shape rotated) {
        int[] kicks = { 0, -1, 1, -2, 2 };
        for (int dx : kicks) {
            if (tryMove(rotated, curX + dx, curY))
                return true;
        }
        return false;
    }
    public boolean moveDown() {
        if (gameOver || isClearing() || curPiece.getShape() == Tetrominoes.NoShape) {
            return false;
        }
        return tryMove(curPiece, curX, curY - 1);
    }

    public void hardDrop() {
        while (moveDown()) {
        }
        lockPiece();
    }

    // ---- 화면 쪽이 읽어 가는 조회용 메서드 ----

    public Tetrominoes shapeAt(int x, int y) {
        return cells[y][x];
    }

    public Tetrominoes currentType() {
        return curPiece.getShape();
    }

    public Tetrominoes getHoldType(){ return holdType; }
    public boolean isHoldUsed(){ return holdUsed; }

    public Shape getCurPiece() {
        return curPiece;
    }

    public int getCurX() {
        return curX;
    }

    public int getCurY() {
        return curY;
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
    public void lockPiece() {
        if (gameOver || isClearing() || curPiece.getShape() == Tetrominoes.NoShape) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            cells[y][x] = curPiece.getShape();
        }
        holdUsed = false;

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

    public boolean hold(){
        if(gameOver||isClearing()||holdUsed||curPiece.getShape()==Tetrominoes.NoShape){
            return false;
        }
        Tetrominoes current = curPiece.getShape();
        Tetrominoes next = (holdType==Tetrominoes.NoShape) ? nextFromQueue() : holdType;
        holdType = current;
        holdUsed = true;
        spawnPiece(next);
        return true;
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
        for(int y = row;y<HEIGHT-1;y++){
            cells[y]=cells[y+1];
        }
        cells[HEIGHT-1]=new Tetrominoes[WIDTH];
        Arrays.fill(cells[HEIGHT-1],Tetrominoes.NoShape);
    }

    public void upLine(int row) {
        for(int y = HEIGHT-1;y>row;y--){
            cells[y]=cells[y-1];
        }
        cells[row]=new Tetrominoes[WIDTH];
        Arrays.fill(cells[row],Tetrominoes.NoShape);
    }
}