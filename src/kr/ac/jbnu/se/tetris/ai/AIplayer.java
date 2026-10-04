package kr.ac.jbnu.se.tetris.ai;

import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.Board;
import kr.ac.jbnu.se.tetris.GameBoard;
import kr.ac.jbnu.se.tetris.Shape;
import kr.ac.jbnu.se.tetris.Tetrominoes;

public class AIplayer {
    private Board view;
    private GameBoard game;
    private TetrisAI ai = new TetrisAI();
    private Timer timer;
    private Move plan = null;
    private int rotationsLeft = 0;
    private int lastY = -1;
    private double mistakeRate = 0.2;

    public AIplayer(Board view) {
        this.view = view;
        this.game = view.getGame();
        timer = new Timer(300, e -> playOneStep());
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void playOneStep() {
        if (!view.isRunning()) {
            return;
        }

        if (game.currentType() == Tetrominoes.NoShape) {
            plan = null;
            return;
        }
        Shape piece = game.getCurPiece();

        if (game.getCurY() > lastY) {
            plan = null;
        }
        lastY = game.getCurY();

        if (plan == null) {
            if (Math.random() < mistakeRate) {
                plan = ai.findRandomMove(game, piece);
            } else {
                plan = ai.findBestMove(game, piece);
            }
            if (plan == null) {
                return;
            }
            rotationsLeft = plan.rotationCount;
        }

        if (rotationsLeft > 0) {
            if (game.rotateRight()) {
                rotationsLeft--;
            }
        } else if (game.getCurX() < plan.x) {
            game.moveRight();
        } else if (game.getCurX() > plan.x) {
            game.moveLeft();
        } else if (Math.random() < 0.2) {
            game.hardDrop();
            plan = null;
        } else {
            game.moveDown();
        }
        view.repaint();
    }
}