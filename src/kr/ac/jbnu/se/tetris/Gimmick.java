package kr.ac.jbnu.se.tetris;

import static kr.ac.jbnu.se.tetris.GameBoard.WIDTH;

import java.util.Random;

/** 상대 보드를 방해하는 기믹. 아직 쓰레기 줄 올리기만 있다. */
public class Gimmick {

    private final GameBoard game;
    private final Random rand = new Random();

    public Gimmick(GameBoard game) {
        this.game = game;
    }

    /** 맨 아래에 구멍 하나 뚫린 쓰레기 줄을 끼워 넣는다. */
    public void plusLine() {
        if (game.isClearing()) return;       // 깜빡이는 중에는 줄 번호가 어긋나므로 건너뜀

        int voidBlock = rand.nextInt(WIDTH);
        game.upLine(0);
        for (int i = 0; i < WIDTH; i++) {
            game.cells[0][i] = (i == voidBlock) ? Tetrominoes.NoShape : Tetrominoes.GarbageShape;
        }
    }
}
