package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Shape;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import javax.swing.Timer;

public class AIplayer {
    public enum Level {
        EASY("쉬움", 0.05, 400, 0.2, "model/weights_easy.txt"),
        NORMAL("보통", 0.0, 300, 0.25, "model/weights_normal.txt"),
        HARD("어려움", 0.0, 200, 0.3, NeuralNet.WEIGHTS);

        private final String label;
        private final double mistakeRate;
        private final int stepMillis;
        private final double hardDropChance;
        /** 이 난이도에서 쓸 신경망. 학습 단계가 다른 모델이다. */
        private final String weights;

        Level(String label, double mistakeRate, int stepMillis, double hardDropChance, String weights) {
            this.label = label;
            this.mistakeRate = mistakeRate;
            this.stepMillis = stepMillis;
            this.hardDropChance = hardDropChance;
            this.weights = weights;
        }

        public String getLabel() {
            return label;
        }
    }

    private Board view;
    private GameBoard game;
    private final TetrisAI ai;
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
        this.ai = new TetrisAI(level.weights);
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

        if (game.getCurY() > lastY) {
            plan = null;
        }
        lastY = game.getCurY();

        if (plan == null && !makePlan(game.getCurPiece())) {
            return;
        }
        doOneAction();
        view.repaint();
    }

    /** 지금 블록을 어디에 놓을지 정한다. 놓을 곳이 없으면 false. */
    private boolean makePlan(Shape piece) {
        if (Math.random() < mistakeRate) {
            plan = ai.findRandomMove(game, piece);
        } else {
            plan = ai.findBestMove(game, piece);
        }
        if (plan == null) {
            return false;
        }
        rotationsLeft = plan.rotationCount;
        return true;
    }

    /** 계획대로 한 동작만 한다: 회전 -> 좌우 이동 -> 내리기. */
    private void doOneAction() {
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
    }
}