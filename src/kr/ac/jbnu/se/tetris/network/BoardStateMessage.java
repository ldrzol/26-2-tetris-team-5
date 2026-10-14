package kr.ac.jbnu.se.tetris.network;

import java.io.Serializable;

import kr.ac.jbnu.se.tetris.Tetrominoes;

public class BoardStateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Tetrominoes[] cells;
    public final Tetrominoes currentType;
    public final int[] pieceX;
    public final int[] pieceY;
    public final int linesRemoved;
    public final int score;
    public final boolean gameOver;

    public BoardStateMessage(Tetrominoes[] cells, Tetrominoes currentType,
                             int[] pieceX, int[] pieceY, int linesRemoved, int score, boolean gameOver) {
        this.cells = cells;
        this.currentType = currentType;
        this.pieceX = pieceX;
        this.pieceY = pieceY;
        this.linesRemoved = linesRemoved;
        this.score = score;
        this.gameOver = gameOver;
    }
}