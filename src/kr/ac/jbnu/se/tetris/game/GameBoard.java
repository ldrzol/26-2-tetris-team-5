package kr.ac.jbnu.se.tetris.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 한 판의 규칙. 직접 계산하지 않고 맡은 객체들에게 시키는 창구(Facade)다.
 * 쌓인 블록은 Playfield, 조작 중인 블록은 ActivePiece, 대기열은 PieceQueue,
 * 보관함은 HoldSlot, 점수는 ScoreKeeper, 기믹은 Gimmick 이 맡는다.
 */
public class GameBoard {

    public static final int WIDTH = 10;   // 게임판 가로 칸 수
    public static final int HEIGHT = 22;  // 게임판 세로 칸 수 (위 2줄은 블록 생성용 숨김 영역)

    public static final int VISIBLE_HEIGHT = 20;  // 화면에 그리는 줄 수

    public static final int COLOR_COUNT = 7;   // 블록 색 가짓수 (Board.PALETTE 와 같아야 함)

    /** 레벨 상한. */
    public static final int MAX_LEVEL = ScoreKeeper.MAX_LEVEL;

    /** 이 콤보부터 피버가 터진다. */
    public static final int FEVER_COMBO = ScoreKeeper.FEVER_COMBO;

    /** 블록 두 개를 한 덩어리로 만들 때 둘 사이를 띄우는 칸 수. */
    private static final int TWIN_GAP = 3;

    private final Playfield field = new Playfield(WIDTH, HEIGHT);           // 쌓여 있는 블록들
    private final ActivePiece piece = new ActivePiece();                    // 현재 조작 중인 블록
    private final PieceQueue pieceQueue = new PieceQueue(COLOR_COUNT);      // 7bag 큐와 다음 블록
    private final HoldSlot holdSlot = new HoldSlot();                       // 보관함
    private final ScoreKeeper scoreKeeper = new ScoreKeeper();              // 점수·콤보·레벨·피버
    private final Gimmick gimmick = new Gimmick(this);                      // [기믹] 기믹 담당
    private final Random rng = new Random();

    private boolean gameOver = false;        // 게임 오버 여부
    private boolean spawnPending = false;   // 줄이 지워져서 새 블록을 다음 틱에 만들어야 하는 상태
    private final List<Integer> flashRows = new ArrayList<>(); // 깜빡이는 중인(곧 지워질) 줄들

    public GameBoard() {
        reset();
    }

    /** [기믹] 이 보드의 기믹 관리자. */
    public Gimmick getGimmick() {
        return gimmick;
    }

    public static int levelGoal(int level) {
        return ScoreKeeper.levelGoal(level);
    }

    /** 현재 레벨. */
    public int getLevel() {
        return scoreKeeper.level();
    }

    /** 레벨 클리어 뒤: 쌓인 블록만 지우고 점수·레벨·줄 수는 그대로 이어 간다. */
    public void clearStack() {
        field.clear();
        flashRows.clear();
        scoreKeeper.resetStreak();
        spawnPending = false;
        spawn();
    }

    /** 2인 대전처럼 레벨이 오르면 안 되는 판에서는 false 로 끈다. */
    public void setLevelProgression(boolean on) {
        scoreKeeper.setLevelProgression(on);
    }

    /** 보드를 비우고 상태를 초기화한다. (첫 블록은 spawn()으로 따로 만든다) */
    public void reset() {
        field.clear();
        piece.clear();
        holdSlot.clear();
        gameOver = false;
        spawnPending = false;
        flashRows.clear();
        pieceQueue.clear();
        gimmick.clear();                        // [기믹]
        scoreKeeper.reset();
        pieceQueue.advance();
    }

    /**
     * 새 블록 생성. 예고된 next 를 꺼내 쓰고 다음 것을 새로 뽑는다.
     *
     * 블록을 막는 아이템이 걸려 있으면 조작할 블록이 없는 상태로 둔다. 효과가
     * 풀리면 tick() 이 다시 불러 준다.
     */
    public boolean spawn() {
        if (gimmick.blocksSpawn()) {
            piece.clear();
            return false;
        }
        Tetrominoes shape = pieceQueue.nextShape();
        int color = pieceQueue.nextColor();
        pieceQueue.advance();
        holdSlot.unlock();         // 새 블록이 나왔으니 홀드를 다시 쓸 수 있다
        return place(shape, color);
    }

    /** 주어진 블록을 시작 위치에 놓는다. 놓을 수 없으면 게임 오버. */
    private boolean place(Tetrominoes shape, int color) {
        piece.begin(shape, color);
        if (gimmick.doublesPiece())                 // [아이템] 블록 두 개가 한 덩어리로 떨어진다
            piece.joinWith(twinShape(), TWIN_GAP);
        piece.moveToSpawn(WIDTH, HEIGHT);
        gimmick.onSpawn();                          // [기믹] 블록마다 다시 뽑는 기믹에 알린다

        if (piece.settleAtSpawn(field))
            return true;
        piece.clear();
        gameOver = true;
        return false;
    }

    /** 덩어리에 같이 붙일 블록을 큐에서 하나 더 꺼낸다. (2개 동시이므로 큐도 두 개를 쓴다) */
    private Shape twinShape() {
        Shape other = new Shape();
        other.setShape(pieceQueue.next());
        return other;
    }

    /** 조작할 수 있는 상태인지. 끝났거나 줄이 깜빡이는 중이거나 조작할 블록이 없으면 false. */
    private boolean canControl() {
        return !gameOver && !isClearing() && !piece.isNone();
    }

    /**
     * [아이템] 조작 중인 블록을 다른 모양으로 바꾼다. (블록 교환)
     *
     * 그 자리에 안 들어가면 좌우로 밀어 보고, 그래도 안 되면 바꾸지 않는다.
     */
    public boolean replaceCurrentPiece(Tetrominoes shape) {
        if (!canControl() || shape == Tetrominoes.NoShape || shape == Tetrominoes.GarbageShape)
            return false;
        return piece.replaceWith(field, shape);
    }

    /**
     * [기믹] 기믹이 블록을 저절로 돌릴 때. 플레이어 입력이 아니므로 키 반전과
     * 회전 금지의 영향을 받지 않는다.
     */
    void forceRotate() {
        if (canControl())
            piece.rotateRight(field);
    }

    /**
     * 지금 블록을 보관하고, 보관해 둔 블록을 꺼내 쓴다. 보관함이 비어 있으면
     * 다음 블록을 대신 꺼낸다. 블록 하나당 한 번만 쓸 수 있다.
     */
    public boolean hold() {
        if (holdSlot.isLocked() || !canControl())
            return false;
        if (gimmick.blocksSpawn())   // [아이템] 막혀 있는 동안은 홀드로도 새 블록을 꺼낼 수 없다
            return false;

        Tetrominoes keptShape = piece.type();
        int keptColor = piece.color();

        if (holdSlot.isEmpty())
            spawn();
        else
            place(holdSlot.shape(), holdSlot.color());

        holdSlot.store(keptShape, keptColor);   // spawn() 이 풀어 놓은 것을 다시 잠근다
        return true;
    }

    /** 타이머 한 틱 처리. 줄 삭제 직후라면 새 블록을 만들고, 아니면 한 칸 내린다. */
    public void tick() {
        if (gameOver || isClearing())   // 줄이 깜빡이는 동안은 게임을 멈춘다
            return;
        if (spawnPending) {
            spawnPending = false;
            spawn();
        } else if (piece.isNone()) {
            spawn();           // [아이템] 블록을 막는 효과가 풀리면 여기서 다시 나온다
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
                return piece.move(field, -1, 0);
            case RIGHT:
                return piece.move(field, 1, 0);
            case ROTATE_LEFT:
                return !gimmick.blocksRotation() && piece.rotateLeft(field);
            case ROTATE_RIGHT:
                return !gimmick.blocksRotation() && piece.rotateRight(field);
            default:
                return false;
        }
    }

    public boolean moveDown() {
        return canControl() && piece.move(field, 0, -1);
    }

    public void hardDrop() {
        int dropped = 0;
        while (moveDown())
            dropped++;
        gain(dropped * 2);                        // 하드 드롭: 떨어뜨린 칸마다 2점
        lockPiece();
    }

    // ---- 화면 쪽이 읽어 가는 조회용 메서드 ----

    /** 쌓여 있는 칸의 색 번호. */
    public int colorAt(int x, int y) {
        return field.colorAt(x, y);
    }

    public Tetrominoes shapeAt(int x, int y) {
        return field.shapeAt(x, y);
    }

    /** 지금 떨어지는 블록의 색 번호. */
    public int getCurColor() {
        return piece.color();
    }

    /** AI 가 판을 읽을 때 쓴다. */
    public Shape getCurPiece() {
        return piece.shape();
    }

    public int getCurX() {
        return piece.x();
    }

    public int getCurY() {
        return piece.y();
    }

    public Tetrominoes currentType() {
        return piece.type();
    }

    /** 지금 떨어지는 블록이 몇 칸짜리인지. 보통 4, 2개 동시 아이템이 걸리면 8. */
    public int pieceCount() {
        return piece.count();
    }

    /** 현재 블록 i번째 사각형의 보드상 x 좌표 */
    public int pieceX(int i) {
        return piece.cellX(i);
    }

    /** 현재 블록 i번째 사각형의 보드상 y 좌표 */
    public int pieceY(int i) {
        return piece.cellY(i);
    }

    /**
     * 지금 화면에 보이는 칸들을 한 줄로 편다. 쌓인 칸 위에 떨어지는 블록까지 찍어 넣는다.
     *
     * 상대에게 보낼 화면을 만들 때 쓴다. 두 배열은 WIDTH * HEIGHT 길이여야 한다.
     */
    public void writeVisible(Tetrominoes[] shapes, int[] colors) {
        field.writeTo(shapes, colors);
        piece.writeInto(shapes, colors, WIDTH, HEIGHT);
    }

    /**
     * 상대 화면을 그대로 비춰 준다. 받은 칸으로 덮어쓰고 조작 블록은 비운다.
     *
     * 떨어지는 블록은 이미 칸에 찍혀서 오므로, 조작 블록을 비워 둬야 고스트가 안 그려진다.
     */
    public void applyRemoteState(Tetrominoes[] shapes, int[] colors) {
        field.readFrom(shapes, colors);
        piece.clear();
        flashRows.clear();
    }

    public Tetrominoes nextType() {
        return pieceQueue.nextShape();
    }

    public int getNextColor() {
        return pieceQueue.nextColor();
    }

    /** [피버] 다음에 나올 블록을 지정한다. 색은 늘 하던 대로 새로 뽑는다. */
    public void setNext(Tetrominoes shape) {
        pieceQueue.setNext(shape);
    }

    /** 보관해 둔 블록. 비어 있으면 NoShape. */
    public Tetrominoes holdType() {
        return holdSlot.shape();
    }

    public int getHoldColor() {
        return holdSlot.color();
    }

    public int getScore() {
        return scoreKeeper.score();
    }

    /** 소프트 드롭처럼 바깥에서 점수를 더할 때 쓴다. */
    void addScore(int points) {
        gain(points);
    }

    /** [아이템] 점수 배수를 적용해서 더한다. 점수가 붙는 곳은 전부 여기를 거친다. */
    private void gain(int points) {
        scoreKeeper.add(points, gimmick.scoreFactor());
    }

    /**
     * [피버] 피버 조건을 채웠으면 한 번만 true 를 돌려준다. (가져가면 꺼진다)
     *
     * 판을 멈추고 블록을 고르게 하는 것은 화면 쪽 일이라 여기서는 신호만 세워 둔다.
     */
    public boolean takeFever() {
        return scoreKeeper.takeFever();
    }

    /** 지금 이어지는 콤보. 0 이하면 콤보 표시 안 함. (첫 삭제 0, 두 번째 삭제부터 1) */
    public int getCombo() {
        return scoreKeeper.combo();
    }

    public int getLinesRemoved() {
        return scoreKeeper.linesRemoved();
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

    /** 현재 블록을 보드에 기록하고 줄을 지운다. 줄이 지워졌으면 새 블록은 다음 틱에 만든다. */
    public void lockPiece() {
        if (!canControl())
            return;
        piece.stampOn(field);
        // 꽉 찬 줄은 바로 지우지 않고 기록만 해 둔다. (Board가 깜빡인 뒤 finishClear()를 호출)
        for (int i = 0; i < HEIGHT; ++i) {
            if (field.isRowFull(i))
                flashRows.add(i);
        }

        if (isClearing()) {
            piece.clear();
        } else {
            scoreKeeper.breakCombo();           // 줄을 못 지웠으니 콤보가 끊긴다
            spawn();
        }
    }

    /** 깜빡임이 끝난 뒤 호출. 기록해 둔 줄을 실제로 지우고, 새 블록은 다음 틱에 만든다. */
    public void finishClear() {
        int n = flashRows.size();
        Collections.sort(flashRows, Collections.reverseOrder());   // 높은 줄부터 당겨야 아래 줄 번호가 안 밀린다
        for (int row : flashRows)
            field.removeRow(row);
        flashRows.clear();
        spawnPending = true;

        scoreKeeper.lineCleared(n, field.isBoardEmpty(), gimmick.scoreFactor());
    }

    // ---- [기믹] Gimmick 이 쓰는 도우미 ----

    /** 쌓인 칸 하나를 지운다. 보드 밖이면 무시한다. */
    void eraseCell(int x, int y) {
        field.erase(x, y);
    }

    /**
     * [아이템] 폭탄. 쌓인 칸 하나를 골라 무작위 모양(한 칸/십자/정사각/일자)으로 터뜨린다.
     * 터뜨릴 블록이 하나도 없으면 false.
     */
    public boolean explode() {
        int[] c = randomStackedCell();
        if (c == null)
            return false;
        for (int[] o : Gimmick.randomBlastOffsets())
            eraseCell(c[0] + o[0], c[1] + o[1]);
        return true;
    }

    /** 블록이 쌓인 칸 중 하나를 무작위로 고른다. {x, y} 를 돌려주고, 하나도 없으면 null. */
    int[] randomStackedCell() {
        return field.randomFilledCell(rng);
    }

    /** 줄이 한 칸 올라온 뒤 조작 중인 블록이 겹치면 한 칸 위로 올린다. 그래도 안 되면 게임 오버. */
    void liftPieceIfBlocked() {
        if (!piece.liftIfBlocked(field))
            gameOver = true;
    }

    /** [기믹] 기믹이 판 칸을 다룰 때 쓴다. */
    Playfield field() {
        return field;
    }
}
