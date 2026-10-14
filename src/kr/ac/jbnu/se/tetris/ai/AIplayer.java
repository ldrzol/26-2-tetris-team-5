package kr.ac.jbnu.se.tetris.ai;

import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.Board;
import kr.ac.jbnu.se.tetris.GameBoard;
import kr.ac.jbnu.se.tetris.Shape;
import kr.ac.jbnu.se.tetris.Tetrominoes;

public class AIplayer {
    public enum Level {
        EASY("쉬움", 0.1, 400, 0.2),
        NORMAL("보통", 0.05, 250, 0.25),
        HARD("어려움", 0.0, 150, 0.3);

        private final String label;
        private final double mistakeRate;
        private final int stepMillis;
        private final double hardDropChance;

        Level(String label, double mistakeRate, int stepMillis, double hardDropChance) {
            this.label = label;
            this.mistakeRate = mistakeRate;
            this.stepMillis = stepMillis;
            this.hardDropChance = hardDropChance;
        }

        public String getLabel() {
            return label;
        }
    }

    private Board view;
    private GameBoard game;
    private TetrisAI ai = new TetrisAI();
    private Timer timer;
    private Move plan = null;
    private int rotationsLeft = 0;
    private int lastY = -1;
    private final double mistakeRate;
    private final double hardDropChance;

    public AIplayer(Board view) {
        this(view, Level.NORMAL);
    }

    public AIplayer(Board view, Level level) {
        this.view = view;
        this.game = view.getGame();
        this.mistakeRate = level.mistakeRate;
        this.hardDropChance = level.hardDropChance;
        timer = new Timer(level.stepMillis, e -> playOneStep());
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
        } else if (Math.random() < hardDropChance) {
            game.hardDrop();
            plan = null;
        } else {
            game.moveDown();
        }
        view.repaint();
    }
}