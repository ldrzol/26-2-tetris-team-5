package kr.ac.jbnu.se.tetris.shared;

import kr.ac.jbnu.se.tetris.game.Shape;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.game.PieceQueue;
import kr.ac.jbnu.se.tetris.game.Playfield;

public class SharedGame implements SharedState {
    public static final int WIDTH = 14;
    public static final int HEIGHT = 22;
    private static final int[] SPAWN_X = {3, 10};
    /** 블록 한 개의 칸 수. */
    public static final int PIECE_CELLS = 4;
    /** 사람 수 (방장 P1, 상대 P2). */
    public static final int PLAYER_COUNT = 2;
    /** getWinner()가 무승부일 때 돌려주는 값. */
    public static final int DRAW = -1;

    /** 벽 킥 때 밀어 보는 칸 수 (제자리 -> 왼쪽 1 -> 오른쪽 1 -> 왼쪽 2 -> 오른쪽 2). */
    private static final int[] KICKS = {0, -1, 1, -2, 2};

    /** [고정 지연] 바닥에 닿은 뒤 고정까지 기다리는 칸 수 (한 칸 = 0.4초). */
    private static final int LOCK_DELAY_TICKS = 2;

    private static class Player {
        Shape shape = new Shape();
        int x;
        int y;
        int score;
        /** 7-bag 주머니와 다음 블록. 1인 모드와 같은 PieceQueue 를 쓴다. (색은 안 써서 1가지) */
        final PieceQueue queue = new PieceQueue(1);
        /** [고정 지연] 바닥에 닿은 채로 지나간 칸 수. */
        int groundTicks;
        /** [홀드] 저장해 둔 블록. 없으면 NoShape. */
        Tetrominoes held = Tetrominoes.NoShape;
        /** [홀드] 이번 블록에서 아직 홀드를 안 썼는지. */
        boolean canHold = true;
    }

    /** 쌓인 칸. 1인 모드와 같은 Playfield 를 쓴다 (가로만 14칸). */
    private final Playfield field = new Playfield(WIDTH, HEIGHT);
    private final Player[] players = {new Player(), new Player()};
    private boolean gameOver = false;

    public SharedGame() {
        reset();
    }

    public void reset() {
        field.clear();
        for (Player player : players) {
            player.score = 0;
            player.queue.clear();
            player.held = Tetrominoes.NoShape;
        }
        gameOver = false;
        spawn(0);
        spawn(1);
    }

    public void tick() {
        fall(0);
        fall(1);
    }

    /**
     * [고정 지연] 중력으로 한 칸 내린다. 바닥에 닿아도 바로 고정하지 않고
     * LOCK_DELAY_TICKS 번 기다리는 동안 옆으로 밀거나 돌릴 수 있게 한다.
     */
    private void fall(int p) {
        Player player = players[p];
        if (tryMove(p, player.shape, player.x, player.y - 1)) {
            player.groundTicks = 0;
            return;
        }
        if (gameOver) {
            return;
        }
        player.groundTicks++;
        if (player.groundTicks >= LOCK_DELAY_TICKS) {
            lockPiece(p);
        }
    }
    public boolean moveLeft(int p) {
        return tryMove(p, players[p].shape, players[p].x - 1, players[p].y);
    }

    public boolean moveRight(int p) {
        return tryMove(p, players[p].shape, players[p].x + 1, players[p].y);
    }

    public boolean rotateRight(int p) {
        return tryRotate(p, players[p].shape.rotateRight());
    }

    public boolean rotateLeft(int p) {
        return tryRotate(p, players[p].shape.rotateLeft());
    }

    /** 벽 킥: 회전한 모양이 막히면 왼쪽·오른쪽으로 1~2칸 밀어서 다시 해 본다. */
    private boolean tryRotate(int p, Shape rotated) {
        Player player = players[p];
        for (int dx : KICKS) {
            if (tryMove(p, rotated, player.x + dx, player.y)) {
                return true;
            }
        }
        return false;
    }
    public boolean moveDown(int p) {
        Player player = players[p];
        if (tryMove(p, player.shape, player.x, player.y - 1)) {
            player.groundTicks = 0;
            return true;
        }
        if (!gameOver) {
            lockPiece(p);
        }
        return false;
    }

    public void hardDrop(int p) {
        while (moveDown(p)) {
            // 비워 둔다: moveDown 이 한 칸씩 내리고, 더 못 내려가면 false 라서 멈춘다 (하드드롭)
        }
    }

    public Tetrominoes shapeAt(int x, int y) {
        return field.shapeAt(x, y);
    }

    public Tetrominoes pieceType(int p) {
        return players[p].shape.getShape();
    }

    public int pieceX(int p, int i) {
        return players[p].x + players[p].shape.x(i);
    }

    public int pieceY(int p, int i) {
        return players[p].y - players[p].shape.y(i);
    }

    public int getScore(int p) {
        return players[p].score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void end() {
        gameOver = true;
    }

    public int getWinner() {
        if (players[0].score > players[1].score) {
            return 0;
        }
        if (players[1].score > players[0].score) {
            return 1;
        }
        return DRAW;
    }

    private boolean tryMove(int p, Shape newShape, int newX, int newY) {
        if (gameOver || !canPlace(newShape, newX, newY)) {
            return false;
        }
        Player player = players[p];
        player.shape = newShape;
        player.x = newX;
        player.y = newY;
        return true;
    }

    private boolean canPlace(Shape shape, int px, int py) {
        return field.canPlace(shape, px, py);   // 판 안·빈 칸 검사는 Playfield 를 그대로 쓴다
    }
    /** 다음 블록을 꺼내 나올 자리에 둔다. 막혔으면 게임 끝. */
    private void spawn(int p) {
        Player player = players[p];
        if (player.queue.nextShape() == Tetrominoes.NoShape) {
            player.queue.advance();   // 맨 처음에는 다음 블록이 없으니 먼저 하나 뽑는다
        }
        Tetrominoes type = player.queue.nextShape();
        player.queue.advance();       // 7-bag 은 PieceQueue 를 그대로 쓴다
        player.canHold = true;   // [홀드] 새 블록이 나오면 다시 홀드할 수 있다
        enter(p, type);
    }

    /** 이 모양의 블록을 나올 자리에 둔다. 쌓인 블록에 막혔으면 게임 끝. */
    private void enter(int p, Tetrominoes type) {
        Player player = players[p];
        player.shape = new Shape();
        player.shape.setShape(type);
        player.x = SPAWN_X[p];
        player.groundTicks = 0;
        player.y = HEIGHT - 1 + player.shape.minY();
        if (!canPlace(player.shape, player.x, player.y)) {
            player.shape.setShape(Tetrominoes.NoShape);
            gameOver = true;
        }
    }


    /** p번 사람의 다음 블록 (화면에 미리 보여 주기용). */
    public Tetrominoes nextType(int p) {
        return players[p].queue.nextShape();
    }

    /** [홀드] 지금 블록을 저장해 두고, 저장해 둔 블록(없으면 다음 블록)을 꺼낸다. 블록 하나에 한 번만 된다. */
    public void hold(int p) {
        Player player = players[p];
        if (gameOver || !player.canHold || player.shape.getShape() == Tetrominoes.NoShape) {
            return;
        }
        Tetrominoes saved = player.held;
        player.held = player.shape.getShape();
        if (saved == Tetrominoes.NoShape) {
            spawn(p);
        } else {
            enter(p, saved);
        }
        player.canHold = false;
    }

    /** [홀드] p번 사람이 저장해 둔 블록. 없으면 NoShape. */
    public Tetrominoes holdType(int p) {
        return players[p].held;
    }

    /** [고스트] 지금 블록을 바로 떨어뜨리면 몇 칸 내려가는지. 떨어질 자리를 흐리게 그릴 때 쓴다. */
    public int ghostDrop(int p) {
        Player player = players[p];
        if (player.shape.getShape() == Tetrominoes.NoShape) {
            return 0;
        }
        int drop = 0;
        while (canPlace(player.shape, player.x, player.y - drop - 1)) {
            drop++;
        }
        return drop;
    }

    private void lockPiece(int p) {
        Player player = players[p];
        for (int i = 0; i < PIECE_CELLS; i++) {
            int x = player.x + player.shape.x(i);
            int y = player.y - player.shape.y(i);
            field.set(x, y, player.shape.getShape(), 0);   // 색 번호는 땅따먹기에서 안 써서 0
        }
        player.score += clearLines();

        // [버그 13] 상대 블록이 방금 쌓인 칸과 겹치면, 예전에는 상대 블록을 지우고 새로 냈다(블록이 사라짐).
        // 이제는 지우지 않고 겹치지 않는 곳까지 위로 올린다.
        keepOutOfCells(1 - p);
        spawn(p);
    }

    /** [버그 13] 상대 블록이 쌓인 칸과 겹치면 겹치지 않을 때까지 위로 올린다. 올릴 곳이 없으면 그때만 새로 낸다. */
    private void keepOutOfCells(int p) {
        Player player = players[p];
        if (player.shape.getShape() == Tetrominoes.NoShape) {
            return;
        }
        while (!canPlace(player.shape, player.x, player.y) && player.y < HEIGHT) {
            player.y++;
        }
        if (!canPlace(player.shape, player.x, player.y)) {
            spawn(p);
        }
    }
    private int clearLines() {
        int cleared = 0;
        int y = 0;
        while (y < HEIGHT) {
            if (field.isRowFull(y)) {
                field.removeRow(y);      // 줄 검사·지우기는 Playfield 를 그대로 쓴다
                cleared++;
            } else {
                y++;
            }
        }
        return cleared;
    }
}