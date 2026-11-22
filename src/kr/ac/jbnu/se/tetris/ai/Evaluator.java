package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

/**
 * 블록을 놓아 본 판에 점수를 매기는 방법. 점수가 높을수록 좋은 자리다.
 * TetrisAI 는 자리를 전부 시험하고 이 점수가 가장 높은 곳을 고른다.
 */
public interface Evaluator {
    double score(Tetrominoes[] board, int width, int height);
}