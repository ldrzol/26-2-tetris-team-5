package kr.ac.jbnu.se.tetris;

import java.util.Random;
import static kr.ac.jbnu.se.tetris.GameBoard.WIDTH;
import static kr.ac.jbnu.se.tetris.GameBoard.HEIGHT;

public class Gimmick{
    private final GameBoard game;
    private final Random rand = new Random();

    public Gimmick(GameBoard game) {
        this.game = game;
    }

    public void plusLine() {
        if (game.isClearing()) return;       // 깜빡이는 중에는 줄 번호가 어긋나므로 건너뜀

        int voidBlock = rand.nextInt(WIDTH);
        game.upLine(0);
        for (int i = 0; i < WIDTH; i++) {
            game.cells[0][i] = (i == voidBlock) ? Tetrominoes.NoShape : Tetrominoes.GarbageShape;
        }
    }

    public void boardRotate(){

    }


    public void rotateUnable() {

    }
}
