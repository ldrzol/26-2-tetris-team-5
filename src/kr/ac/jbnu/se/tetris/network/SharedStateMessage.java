package kr.ac.jbnu.se.tetris.network;

import java.io.Serializable;

import kr.ac.jbnu.se.tetris.SharedGame;
import kr.ac.jbnu.se.tetris.SharedState;
import kr.ac.jbnu.se.tetris.Tetrominoes;

public class SharedStateMessage implements Serializable, SharedState {
    private static final long serialVersionUID = 1L;

    private final Tetrominoes[] cells;
    private final Tetrominoes[] types;
    private final int[] xs;
    private final int[] ys;
    private final int[] scores;
    private final boolean gameOver;
    private final int winner;

    public SharedStateMessage(SharedState source) {
        cells = new Tetrominoes[SharedGame.WIDTH * SharedGame.HEIGHT];
        for (int y = 0; y < SharedGame.HEIGHT; y++) {
            for (int x = 0; x < SharedGame.WIDTH; x++) {
                cells[y * SharedGame.WIDTH + x] = source.shapeAt(x, y);
            }
        }

        types = new Tetrominoes[2];
        xs = new int[8];
        ys = new int[8];
        scores = new int[2];
        for (int p = 0; p < 2; p++) {
            types[p] = source.pieceType(p);
            scores[p] = source.getScore(p);
            for (int i = 0; i < 4; i++) {
                xs[p * 4 + i] = source.pieceX(p, i);
                ys[p * 4 + i] = source.pieceY(p, i);
            }
        }

        gameOver = source.isGameOver();
        winner = source.getWinner();
    }

    @Override
    public Tetrominoes shapeAt(int x, int y) {
        return cells[y * SharedGame.WIDTH + x];
    }

    @Override
    public Tetrominoes pieceType(int p) {
        return types[p];
    }

    @Override
    public int pieceX(int p, int i) {
        return xs[p * 4 + i];
    }

    @Override
    public int pieceY(int p, int i) {
        return ys[p * 4 + i];
    }

    @Override
    public int getScore(int p) {
        return scores[p];
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    @Override
    public int getWinner() {
        return winner;
    }
}