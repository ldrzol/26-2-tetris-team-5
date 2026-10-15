package kr.ac.jbnu.se.tetris.network;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.item.Attack;

import java.io.Serializable;

/**
 * 내 판을 상대에게 보여 주기 위해 보내는 화면 한 장.
 *
 * 떨어지는 블록은 보내는 쪽에서 이미 칸에 찍어 넣는다. 받는 쪽은 칸만 그대로 그리면
 * 되므로 블록 좌표를 따로 들고 다닐 필요가 없다.
 */
public class BoardStateMessage implements Serializable {
    private static final long serialVersionUID = 3L;

    public final Tetrominoes[] cells;
    public final int[] colors;
    public final int linesRemoved;
    public final int score;
    public final boolean gameOver;

    /** 보내는 쪽이 손에 든 아이템. 빈 자리는 null. 상대 화면에 그대로 보여 준다. */
    public final Attack[] hand;

    /** 연속 삭제 횟수. 받는 쪽은 칸만 그리므로 따로 알려 줘야 한다. */
    public final int combo;

    public BoardStateMessage(Tetrominoes[] cells, int[] colors,
                             int linesRemoved, int score, boolean gameOver, Attack[] hand,
                             int combo) {
        this.cells = cells;
        this.colors = colors;
        this.linesRemoved = linesRemoved;
        this.score = score;
        this.gameOver = gameOver;
        this.hand = hand;
        this.combo = combo;
    }
}
