package kr.ac.jbnu.se.tetris;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class GameBoard {

    public static final int WIDTH = 10;   // 게임판 가로 칸 수
    public static final int HEIGHT = 22;  // 게임판 세로 칸 수 (위 2줄은 블록 생성용 숨김 영역)

    public static final int VISIBLE_HEIGHT = 20;  // 화면에 그리는 줄 수

    public static final int COLOR_COUNT = 7;   // 블록 색 가짓수 (Board.PALETTE 와 같아야 함)

    protected final Tetrominoes[][] cells = new Tetrominoes[HEIGHT][WIDTH]; // 쌓여 있는 블록들
    protected final int[][] colorIds = new int[HEIGHT][WIDTH];              // cells 와 같은 자리의 색 번호
    protected final int[][] age = new int[HEIGHT][WIDTH];                   // [기믹] 칸이 놓인 뒤 지난 초 (FADE 용)
    private final Gimmick gimmick = new Gimmick(this);                      // [기믹] 기믹 담당
    private final Random rng = new Random();
    private int curColor = 0;               // 지금 떨어지는 블록의 색 번호

    private Tetrominoes nextShape;          // 다음에 나올 블록
    private int nextColor;
    private Tetrominoes holdShape = Tetrominoes.NoShape;   // 보관해 둔 블록 (없으면 NoShape)
    private int holdColor;
    private boolean holdLocked = false;     // 블록 하나당 홀드는 한 번만
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

    // ---- 점수 ----
    private static final int[] LINE_POINTS    = { 0, 100, 300, 500, 800 };    // 지운 줄 수별 기본 점수 (x 레벨)
    private static final int[] PERFECT_POINTS = { 0, 800, 1200, 1800, 2000 }; // 퍼펙트 클리어 보너스 (x 레벨)
    private static final int COMBO_POINTS = 50;                               // 콤보 보너스 (x 콤보 x 레벨)
    private int score = 0;
    private int combo = -1;                 // 연속으로 줄을 지운 횟수 (-1: 연속 아님)

    public GameBoard() {
        reset();
    }

    /** [기믹] 이 보드의 기믹 관리자. */
    public Gimmick getGimmick() {
        return gimmick;
    }

    /** 레벨 상한. 가이드라인 마라톤과 같게 15에서 멈춘다. */
    public static final int MAX_LEVEL = 15;

    /**
     * 레벨 n 을 끝내는 데 필요한 '누적' 점수.
     *
     * 가이드라인은 레벨당 10줄이고 한 줄(싱글) 점수가 100 x 레벨이므로,
     * 레벨 n 을 깨는 데 드는 점수는 1000 x n 이다. 그것을 누적하면
     * 500 x n x (n+1) 이 된다. (1000, 3000, 6000, 10000, 15000 ...)
     */
    public static int levelGoal(int level) {
        return 500 * level * (level + 1);
    }

    /** 현재 레벨. 점수가 levelGoal 을 넘을 때마다 오른다. */
    public int getLevel() {
        int level = 1;
        while (level < MAX_LEVEL && score >= levelGoal(level))
            level++;
        return level;
    }

    /** 이번 레벨을 끝내는 데 필요한 점수. (화면 표시용) */
    public int getLevelGoal() {
        return levelGoal(getLevel());
    }

    /**
     * 레벨 클리어 뒤: 쌓인 블록만 지우고 점수·레벨·줄 수는 그대로 이어 간다.
     */
    public void clearStack() {
        for (int i = 0; i < HEIGHT; ++i) {
            Arrays.fill(cells[i], Tetrominoes.NoShape);
            Arrays.fill(colorIds[i], 0);
            Arrays.fill(age[i], 0);
        }
        flashRows.clear();
        combo = -1;
        spawnPending = false;
        spawn();
    }

    /** 보드를 비우고 상태를 초기화한다. (첫 블록은 spawn()으로 따로 만든다) */
    public void reset() {
        for (int i = 0; i < HEIGHT; ++i){
            Arrays.fill(cells[i],Tetrominoes.NoShape);
            Arrays.fill(colorIds[i],0);
            Arrays.fill(age[i],0);              // [기믹]
        }
        curPiece.setShape(Tetrominoes.NoShape);
        holdShape = Tetrominoes.NoShape;
        holdLocked = false;
        linesRemoved = 0;
        gameOver = false;
        spawnPending = false;
        flashRows.clear();
        queue.clear();
        gimmick.clear();                        // [기믹]
        score = 0;
        combo = -1;
        rollNext();
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

    /**
     * 앞으로 나올 블록을 순서대로 n개 미리 본다.
     * 맨 앞은 이미 큐에서 꺼내 둔 nextShape 이므로 그것부터 넣어야
     * Nextpanel 의 예고와 실제로 나오는 순서가 어긋나지 않는다.
     */
    public List<Tetrominoes> peekNext(int n){
        refillQueue();
        List<Tetrominoes> result = new ArrayList<>();
        if(n > 0 && nextShape != null && nextShape != Tetrominoes.NoShape){
            result.add(nextShape);
        }
        for(Tetrominoes t : queue){
            if(result.size()>=n){
                break;
            }
            result.add(t);
        }
        return result;
    }

    /** 새 블록 생성. 예고된 next 를 꺼내 쓰고 다음 것을 새로 뽑는다. */
    public boolean spawn() {
        Tetrominoes shape = nextShape;
        int color = nextColor;
        rollNext();
        holdLocked = false;        // 새 블록이 나왔으니 홀드를 다시 쓸 수 있다
        return place(shape, color);
    }

    /** 주어진 블록을 시작 위치에 놓는다. 놓을 수 없으면 게임 오버. */
    private boolean place(Tetrominoes shape, int color) {
        curPiece.setShape(shape);
        curColor = color;
        curX = WIDTH / 2 + 1;
        curY = HEIGHT - 1 + curPiece.minY();

        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Tetrominoes.NoShape);
            gameOver = true;
            return false;
        }
        return true;
    }

    /** 다음 블록을 7bag 큐에서 꺼낸다. 색은 따로 무작위로 고른다. */
    private void rollNext() {
        nextShape = nextFromQueue();
        nextColor = rng.nextInt(COLOR_COUNT);
    }

    /**
     * 지금 블록을 보관하고, 보관해 둔 블록을 꺼내 쓴다. 보관함이 비어 있으면
     * 다음 블록을 대신 꺼낸다. 블록 하나당 한 번만 쓸 수 있다.
     */
    public boolean hold() {
        if (holdLocked || gameOver || isClearing() || curPiece.getShape() == Tetrominoes.NoShape)
            return false;

        Tetrominoes keptShape = curPiece.getShape();
        int keptColor = curColor;

        if (holdShape == Tetrominoes.NoShape)
            spawn();
        else
            place(holdShape, holdColor);

        holdShape = keptShape;
        holdColor = keptColor;
        holdLocked = true;         // spawn() 이 풀어 놓은 것을 다시 잠근다
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

    // [기믹] 이동·회전은 입력을 Gimmick 을 거쳐(키 반전) 실제 동작으로 바꿔 실행한다.
    public boolean moveLeft()    { return handle(Gimmick.Input.LEFT); }

    public boolean moveRight()   { return handle(Gimmick.Input.RIGHT); }

    public boolean rotateLeft()  { return handle(Gimmick.Input.ROTATE_LEFT); }

    public boolean rotateRight() { return handle(Gimmick.Input.ROTATE_RIGHT); }

    /** 눌린 입력을 remap() 으로 바꾼 뒤 실행한다. 회전 불가도 여기서 막는다. */
    private boolean handle(Gimmick.Input pressed) {
        switch (gimmick.remap(pressed)) {
            case LEFT:
                return tryMove(curPiece, curX - 1, curY);
            case RIGHT:
                return tryMove(curPiece, curX + 1, curY);
            case ROTATE_LEFT:
                if (gimmick.blocksRotation()) return false;
                return tryRotate(curPiece.rotateLeft());
            case ROTATE_RIGHT:
                if (gimmick.blocksRotation()) return false;
                return tryRotate(curPiece.rotateRight());
            default:
                return false;
        }
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
        int cells = 0;
        while (moveDown())
            cells++;
        score += cells * 2;                     // 하드 드롭: 떨어뜨린 칸마다 2점
        lockPiece();
    }

    // ---- 화면 쪽이 읽어 가는 조회용 메서드 ----

    /** 쌓여 있는 칸의 색 번호. */
    public int colorAt(int x, int y) {
        return colorIds[y][x];
    }

    /** 지금 떨어지는 블록의 색 번호. */
    public int getCurColor() {
        return curColor;
    }

    public Tetrominoes shapeAt(int x, int y) {
        return cells[y][x];
    }

    public Tetrominoes currentType() {
        return curPiece.getShape();
    }

    public Tetrominoes getHoldType(){ return holdShape; }
    public boolean isHoldUsed(){ return holdLocked; }

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

    public Tetrominoes nextType() {
        return nextShape;
    }

    public int getNextColor() {
        return nextColor;
    }

    /** 보관해 둔 블록. 비어 있으면 NoShape. */
    public Tetrominoes holdType() {
        return holdShape;
    }

    public int getHoldColor() {
        return holdColor;
    }

    public int getScore() {
        return score;
    }

    /** 소프트 드롭처럼 바깥에서 점수를 더할 때 쓴다. */
    void addScore(int points) {
        score += points;
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
            colorIds[y][x] = curColor;
            age[y][x] = 0;                      // [기믹] 새로 놓인 칸은 나이 0
        }
        // 꽉 찬 줄은 바로 지우지 않고 기록만 해 둔다. (Board가 깜빡인 뒤 finishClear()를 호출)
        for (int i = 0; i < HEIGHT; ++i) {
            if (isLineFull(i))
                flashRows.add(i);
        }

        if (isClearing()) {
            curPiece.setShape(Tetrominoes.NoShape);
        } else {
            combo = -1;                         // 줄을 못 지웠으니 콤보가 끊긴다
            spawn();
        }
    }

    /** 깜빡임이 끝난 뒤 호출. 기록해 둔 줄을 실제로 지우고, 새 블록은 다음 틱에 만든다. */
    public void finishClear() {
        int n = flashRows.size();
        int level = getLevel();                                    // 줄을 더하기 전 레벨로 점수를 계산한다

        Collections.sort(flashRows, Collections.reverseOrder());   // 높은 줄부터 당겨야 아래 줄 번호가 안 밀린다
        for (int row : flashRows)
            downLine(row);
        linesRemoved += n;
        flashRows.clear();
        spawnPending = true;

        int gained = LINE_POINTS[n] * level;                       // 줄 삭제
        combo++;
        gained += COMBO_POINTS * combo * level;                    // 콤보 (첫 삭제는 combo=0 이라 0점)
        if (isBoardEmpty())
            gained += PERFECT_POINTS[n] * level;                   // 퍼펙트 클리어
        score += gained;
    }

    private boolean isBoardEmpty() {
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                if (cells[y][x] != Tetrominoes.NoShape)
                    return false;
        return true;
    }

    private boolean isLineFull(int row){
        for (int j = 0; j < WIDTH; ++j) {
            if (shapeAt(j, row)==Tetrominoes.NoShape){
                return false;
            }
        }
        return true;
    }

    // ---- [기믹] Gimmick 이 쓰는 도우미 ----

    /** 쌓인 칸 하나를 지운다. 보드 밖이면 무시한다. */
    void eraseCell(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        cells[y][x] = Tetrominoes.NoShape;
        colorIds[y][x] = 0;
        age[y][x] = 0;
    }

    /** 블록이 쌓인 칸 중 하나를 무작위로 고른다. {x, y} 를 돌려주고, 하나도 없으면 null. */
    int[] randomStackedCell() {
        List<int[]> filled = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                if (cells[y][x] != Tetrominoes.NoShape)
                    filled.add(new int[] { x, y });
        return filled.isEmpty() ? null : filled.get(rng.nextInt(filled.size()));
    }

    /** 줄이 한 칸 올라온 뒤 조작 중인 블록이 겹치면 한 칸 위로 올린다. 그래도 안 되면 게임 오버. */
    void liftPieceIfBlocked() {
        if (curPiece.getShape() == Tetrominoes.NoShape) return;   // 줄 삭제 중에는 조작 블록이 없다
        if (!tryMove(curPiece, curX, curY) && !tryMove(curPiece, curX, curY + 1))
            gameOver = true;
    }

    public void downLine(int row) {
        for(int y = row;y<HEIGHT-1;y++){
            cells[y]=cells[y+1];
            colorIds[y]=colorIds[y+1];          // 색도 블록을 따라 내려와야 한다
            age[y]=age[y+1];                    // [기믹] 나이도 같이
        }
        cells[HEIGHT-1]=new Tetrominoes[WIDTH];
        Arrays.fill(cells[HEIGHT-1],Tetrominoes.NoShape);
        colorIds[HEIGHT-1]=new int[WIDTH];
        age[HEIGHT-1]=new int[WIDTH];           // [기믹]
    }

    public void upLine(int row) {
        for(int y = HEIGHT-1;y>row;y--){
            cells[y]=cells[y-1];
            colorIds[y]=colorIds[y-1];          // 색도 블록을 따라 올라가야 한다
            age[y]=age[y-1];                    // [기믹] 나이도 같이
        }
        cells[row]=new Tetrominoes[WIDTH];
        Arrays.fill(cells[row],Tetrominoes.NoShape);
        colorIds[row]=new int[WIDTH];
        age[row]=new int[WIDTH];                // [기믹]
    }
}