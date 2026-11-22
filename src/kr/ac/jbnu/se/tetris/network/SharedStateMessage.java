package kr.ac.jbnu.se.tetris.network;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.shared.SharedGame;
import kr.ac.jbnu.se.tetris.shared.SharedState;

import java.io.Serializable;

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

        types =new Tetrominoes[SharedGame.PLAYER_COUNT];
        xs =new int[SharedGame.PLAYER_COUNT * SharedGame.PIECE_CELLS];
        ys = new int[SharedGame.PLAYER_COUNT * SharedGame.PIECE_CELLS];
        scores = new int[SharedGame.PLAYER_COUNT];
        for (int p = 0; p < SharedGame.PLAYER_COUNT; p++) {
            types[p] = source.pieceType(p);
            scores[p] = source.getScore(p);
            for (int i = 0; i < SharedGame.PIECE_CELLS; i++) {
                xs[p * SharedGame.PIECE_CELLS + i] = source.pieceX(p, i);
                ys[p * SharedGame.PIECE_CELLS + i] = source.pieceY(p, i);
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
        return xs[p * SharedGame.PIECE_CELLS + i];
    }

    @Override
    public int pieceY(int p, int i) {
        return ys[p * SharedGame.PIECE_CELLS + i];
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