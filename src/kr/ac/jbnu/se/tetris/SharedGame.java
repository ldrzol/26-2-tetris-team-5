package kr.ac.jbnu.se.tetris;

public class SharedGame implements SharedState {
    public static final int WIDTH = 14;
    public static final int HEIGHT = 22;
    private static final int[] SPAWN_X = {3, 10};

    private static class Player {
        Shape shape = new Shape();
        int x;
        int y;
        int score;
    }

    private final Tetrominoes[] cells = new Tetrominoes[WIDTH * HEIGHT];
    private final Player[] players = {new Player(), new Player()};
    private boolean gameOver = false;

    public SharedGame() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = Tetrominoes.NoShape;
        }
        for (Player player : players) {
            player.score = 0;
        }
        gameOver = false;
        spawn(0);
        spawn(1);
    }

    public void tick() {
        moveDown(0);
        moveDown(1);
    }

    public boolean moveLeft(int p) {
        return tryMove(p, players[p].shape, players[p].x - 1, players[p].y);
    }

    public boolean moveRight(int p) {
        return tryMove(p, players[p].shape, players[p].x + 1, players[p].y);
    }

    public boolean rotateRight(int p) {
        return tryMove(p, players[p].shape.rotateRight(), players[p].x, players[p].y);
    }

    public boolean moveDown(int p) {
        Player player = players[p];
        if (tryMove(p, player.shape, player.x, player.y - 1)) {
            return true;
        }
        if (!gameOver) {
            lockPiece(p);
        }
        return false;
    }

    public void hardDrop(int p) {
        while (moveDown(p)) {
        }
    }

    public Tetrominoes shapeAt(int x, int y) {
        return cells[y * WIDTH + x];
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

    public int getWinner() {
        if (players[0].score > players[1].score) {
            return 0;
        }
        if (players[1].score > players[0].score) {
            return 1;
        }
        return -1;
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
        for (int i = 0; i < 4; i++) {
            int x = px + shape.x(i);
            int y = py - shape.y(i);
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
                return false;
            }
            if (cells[y * WIDTH + x] != Tetrominoes.NoShape) {
                return false;
            }
        }
        return true;
    }

    private boolean spawn(int p) {
        Player player = players[p];
        player.shape.setRandomShape();
        player.x = SPAWN_X[p];
        player.y = HEIGHT - 1 + player.shape.minY();
        if (!canPlace(player.shape, player.x, player.y)) {
            player.shape.setShape(Tetrominoes.NoShape);
            gameOver = true;
            return false;
        }
        return true;
    }

    private void lockPiece(int p) {
        Player player = players[p];
        for (int i = 0; i < 4; i++) {
            int x = player.x + player.shape.x(i);
            int y = player.y - player.shape.y(i);
            cells[y * WIDTH + x] = player.shape.getShape();
        }
        player.score += clearLines();

        int other = 1 - p;
        if (overlapsCells(players[other])) {
            spawn(other);
        }
        spawn(p);
    }

    private boolean overlapsCells(Player player) {
        if (player.shape.getShape() == Tetrominoes.NoShape) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            int x = player.x + player.shape.x(i);
            int y = player.y - player.shape.y(i);
            if (cells[y * WIDTH + x] != Tetrominoes.NoShape) {
                return true;
            }
        }
        return false;
    }

    private int clearLines() {
        int cleared = 0;
        int y = 0;
        while (y < HEIGHT) {
            if (isRowFull(y)) {
                removeRow(y);
                cleared++;
            } else {
                y++;
            }
        }
        return cleared;
    }

    private boolean isRowFull(int y) {
        for (int x = 0; x < WIDTH; x++) {
            if (cells[y * WIDTH + x] == Tetrominoes.NoShape) {
                return false;
            }
        }
        return true;
    }

    private void removeRow(int y) {
        for (int row = y; row < HEIGHT - 1; row++) {
            for (int x = 0; x < WIDTH; x++) {
                cells[row * WIDTH + x] = cells[(row + 1) * WIDTH + x];
            }
        }
        for (int x = 0; x < WIDTH; x++) {
            cells[(HEIGHT - 1) * WIDTH + x] = Tetrominoes.NoShape;
        }
    }
}