package kr.ac.jbnu.se.tetris.shared;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

public interface SharedState {
    Tetrominoes shapeAt(int x, int y);

    Tetrominoes pieceType(int p);

    int pieceX(int p, int i);

    int pieceY(int p, int i);

    int getScore(int p);

    boolean isGameOver();

    int getWinner();

    /** p번 사람의 다음 블록 (미리보기). */
    Tetrominoes nextType(int p);

    /** p번 사람이 홀드해 둔 블록. 없으면 NoShape. */
    Tetrominoes holdType(int p);

    /** p번 사람 블록이 바로 떨어지면 몇 칸 내려가는지 (고스트: pieceY 에서 이만큼 빼면 떨어질 자리). */
    int ghostDrop(int p);
}