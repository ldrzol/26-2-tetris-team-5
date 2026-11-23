package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import java.util.Arrays;

/**
 * 신경망에 넣을 숫자 4개를 판에서 뽑는다. 학습 코드(tetris.py 의 get_state_properties)와 똑같이 센다.
 *
 * [지운 줄, 구멍, 울퉁불퉁함, 높이 합]
 * - 꽉 찬 줄을 먼저 지우고, 지운 뒤의 판으로 구멍, 울퉁불퉁함, 높이를 센다.
 * - 높이는 가장 높은 열 하나가 아니라 모든 열 높이의 합이다.
 *
 * 판은 TetrisAI 와 같은 모양: y = 0 이 맨 아래, 칸 번호는 y * width + x.
 */
public final class BoardFeatures {

    private BoardFeatures() {
    }

    public static double[] of(Tetrominoes[] board, int width, int height) {
        Tetrominoes[] cells = board.clone();
        int lines = clearFullRows(cells, width, height);
        int[] heights = columnHeights(cells, width, height);
        return new double[] { lines, countHoles(cells, heights, width), bumpiness(heights), sum(heights) };
    }

    /** 꽉 찬 줄을 지우고 위 줄들을 내린다. 지운 줄 수를 돌려준다. */
    private static int clearFullRows(Tetrominoes[] cells, int width, int height) {
        int write = 0;
        for (int read = 0; read < height; read++) {
            if (!isFull(cells, read, width)) {
                System.arraycopy(cells, read * width, cells, write * width, width);
                write++;
            }
        }
        Arrays.fill(cells, write * width, height * width, Tetrominoes.NoShape);
        return height - write;
    }

    private static boolean isFull(Tetrominoes[] cells, int y, int width) {
        for (int x = 0; x < width; x++) {
            if (cells[y * width + x] == Tetrominoes.NoShape) {
                return false;
            }
        }
        return true;
    }

    /** 열마다 가장 위 블록까지의 높이. 빈 열은 0. */
    private static int[] columnHeights(Tetrominoes[] cells, int width, int height) {
        int[] heights = new int[width];
        for (int x = 0; x < width; x++) {
            for (int y = height - 1; y >= 0; y--) {
                if (cells[y * width + x] != Tetrominoes.NoShape) {
                    heights[x] = y + 1;
                    break;
                }
            }
        }
        return heights;
    }

    /** 가장 위 블록보다 아래에 있는 빈칸 수. */
    private static int countHoles(Tetrominoes[] cells, int[] heights, int width) {
        int holes = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < heights[x]; y++) {
                if (cells[y * width + x] == Tetrominoes.NoShape) {
                    holes++;
                }
            }
        }
        return holes;
    }

    /** 옆 열끼리 높이 차이의 합. */
    private static int bumpiness(int[] heights) {
        int total = 0;
        for (int x = 0; x < heights.length - 1; x++) {
            total += Math.abs(heights[x] - heights[x + 1]);
        }
        return total;
    }

    private static int sum(int[] heights) {
        int total = 0;
        for (int h : heights) {
            total += h;
        }
        return total;
    }
}