package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import java.util.Arrays;

/**
 * BoardFeatures 가 학습 코드(tetris.py)와 같은 값을 내는지 확인하는 자가 점검.
 * 기대값은 같은 판을 tetris.py 의 get_state_properties 에 넣어서 얻은 값이다.
 */
public class BoardFeaturesSelfCheck {
    /** 꽉 찬 한 줄. */
    private static final String FULL = "##########";

    public static void main(String[] args) {
        check("빈 판", new String[] {}, 0, 0, 0, 0);
        check("한 줄 지움", new String[] {
                ".#........",
                "##.#######",
                FULL }, 1, 0, 4, 10);
        check("가운데 줄 지움", new String[] {
                "#.........",
                "#.#.......",
                FULL,
                "#.##.#####" }, 1, 0, 8, 11);
        check("두 줄 지움 + 구멍", new String[] {
                "..#.......",
                FULL,
                "#.#.#.#.#.",
                FULL,
                ".#########" }, 2, 1, 11, 16);
        System.out.println("BoardFeaturesSelfCheck: 통과");
    }

    /** rows 는 위에서 아래 순서 (마지막이 맨 아래 줄). '#' = 블록. */
    private static void check(String name, String[] rows, double... expected) {
        int width = GameBoard.WIDTH;
        int height = GameBoard.HEIGHT;
        Tetrominoes[] board = new Tetrominoes[width * height];
        Arrays.fill(board, Tetrominoes.NoShape);
        for (int i = 0; i < rows.length; i++) {
            int y = rows.length - 1 - i;
            for (int x = 0; x < width; x++) {
                if (rows[i].charAt(x) == '#') {
                    board[y * width + x] = Tetrominoes.ZShape;
                }
            }
        }
        double[] actual = BoardFeatures.of(board, width, height);
        System.out.println(name + ": " + Arrays.toString(actual));
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(name + " 기대값 " + Arrays.toString(expected));
        }
    }
}