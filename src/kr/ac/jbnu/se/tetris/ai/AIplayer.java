package kr.ac.jbnu.se.tetris.ai;

import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.Board;
import kr.ac.jbnu.se.tetris.Shape;
import kr.ac.jbnu.se.tetris.Tetrominoes;

public class AIplayer {
    private Board board;
    private TetrisAI ai = new TetrisAI();
    private Timer timer;
    private Move plan = null;
    private int rotationsLeft=0;
    private int lastY =-1;
    private double mistakeRate = 0.2;

    public AIplayer(Board board) {
        this.board = board;
        timer = new Timer(150, e -> playOneStep());
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void playOneStep() {
        if (!board.isPlaying()) {
            return;
        }

        Shape piece = board.getCurPiece();
        if (piece.getShape() == Tetrominoes.NoShape) {
            plan = null;
            return;
        }
        if (board.getCurY() > lastY) {
            plan = null;
        }
        lastY = board.getCurY();

        if (plan == null) {
            if (Math.random() < mistakeRate) {
                plan = ai.findRandomMove(board, piece);
            } else {
                plan = ai.findBestMove(board, piece);
            }
            if (plan == null) {
                return;
            }
            rotationsLeft = plan.rotationCount;
        }

        if (rotationsLeft > 0) {
            if (board.tryRotateRight()) {
                rotationsLeft--;
            }
        } else if (board.getCurX() < plan.x) {
            board.tryMoveToX(board.getCurX() + 1);
        } else if (board.getCurX() > plan.x) {
            board.tryMoveToX(board.getCurX() - 1);
        } else if (Math.random() < 0.1) {
            board.hardDrop();
            plan = null;
        } else {
            board.moveDownOnce();
        }
    }

}