package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

/**
 * 기존 AI 의 점수식: 지운 줄 x10 - 가장 높은 열 - 구멍 x5 - 울퉁불퉁함.
 * 신경망 AI 와 비교할 때, 그리고 가중치 파일을 못 읽었을 때 대신 쓴다.
 */
public class HeuristicEvaluator implements Evaluator {

    @Override
    public double score(Tetrominoes[] board, int width, int height) {
        return countClearedLines(board, width, height) * 10.0
                - calculateHeight(board, width, height)
                - countHoles(board, width, height) * 5
                - countBumpiness(board, width, height);
    }

    private int countClearedLines(Tetrominoes[] board, int width, int height) {
        int count = 0;
        for (int y = 0; y < height; y++) {
            boolean full = true;
            for (int x = 0; x < width; x++) {
                if (board[y * width + x] == Tetrominoes.NoShape) {
                    full = false;
                    break;
                }
            }
            if (full) {
                count++;
            }
        }
        return count;
    }

    private int calculateHeight(Tetrominoes[] board, int width, int height) {
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if (board[y * width + x] != Tetrominoes.NoShape) {
                    return y + 1;
                }
            }
        }
        return 0;
    }

    private int countHoles(Tetrominoes[] board, int width, int height) {
        int holes = 0;
        for (int x = 0; x < width; x++) {
            boolean blockFound = false;
            for (int y = height - 1; y >= 0; y--) {
                if (board[y * width + x] != Tetrominoes.NoShape) {
                    blockFound = true;
                } else if (blockFound) {
                    holes++;
                }
            }
        }
        return holes;
    }

    private int countBumpiness(Tetrominoes[] board, int width, int height) {
        int[] tops = new int[width];
        for (int x = 0; x < width; x++) {
            for (int y = height - 1; y >= 0; y--) {
                if (board[y * width + x] != Tetrominoes.NoShape) {
                    tops[x] = y + 1;
                    break;
                }
            }
        }
        int bumpiness = 0;
        for (int x = 0; x < width - 1; x++) {
            bumpiness += Math.abs(tops[x] - tops[x + 1]);
        }
        return bumpiness;
    }
}