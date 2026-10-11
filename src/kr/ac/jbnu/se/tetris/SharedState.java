package kr.ac.jbnu.se.tetris;

public interface SharedState {
    Tetrominoes shapeAt(int x, int y);

    Tetrominoes pieceType(int p);

    int pieceX(int p, int i);

    int pieceY(int p, int i);

    int getScore(int p);

    boolean isGameOver();

    int getWinner();
}