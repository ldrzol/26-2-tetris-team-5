package kr.ac.jbnu.se.tetris.network;

import java.io.Serializable;

import kr.ac.jbnu.se.tetris.Tetrominoes;

/**
 * 내 판을 상대에게 보여 주기 위해 보내는 화면 한 장.
 *
 * 떨어지는 블록은 보내는 쪽에서 이미 칸에 찍어 넣는다. 받는 쪽은 칸만 그대로 그리면
 * 되므로 블록 좌표를 따로 들고 다닐 필요가 없다.
 */
public class BoardStateMessage implements Serializable {
    private static final long serialVersionUID = 2L;

    public final Tetrominoes[] cells;
    public final int[] colors;
    public final int linesRemoved;
    public final int score;
    public final boolean gameOver;

    public BoardStateMessage(Tetrominoes[] cells, int[] colors,
                             int linesRemoved, int score, boolean gameOver) {
        this.cells = cells;
        this.colors = colors;
        this.linesRemoved = linesRemoved;
        this.score = score;
        this.gameOver = gameOver;
    }
}
