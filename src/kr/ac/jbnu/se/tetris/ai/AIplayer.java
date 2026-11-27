package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Shape;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import javax.swing.Timer;

public class AIplayer {
    public enum Level {
        EASY("쉬움", 0.05, 400, 0.2, "model/weights_easy.txt"),
        NORMAL("보통", 0.03, 280, 0.6, NeuralNet.WEIGHTS),   // 어려움 모델 + 5% 실수 + 느리게
        HARD("어려움", 0.0, 250, 0.8, NeuralNet.WEIGHTS);

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
            dropHere();   // 놓을 곳이 없으면 멈춰 있지 말고 떨어뜨려서 다음 블록으로 넘어간다
            view.repaint();
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

    /** 계획대로 한 동작만 한다: 회전 -> 좌우 이동 -> 내리기. 막히면 포기하고 다음으로 넘어간다. */
    private void doOneAction() {
        if (rotationsLeft > 0) {
            if (game.rotateRight()) {
                rotationsLeft--;
            } else if (!game.moveDown()) {
                rotationsLeft = 0;   // 내려갈 수도 없으면(바닥에 닿았거나 회전 금지 기믹) 그때 회전을 포기한다
            }
            // 위가 좁아서 못 돌았으면 한 칸 내렸으니 다음 번에 다시 돌린다
        } else if (game.getCurX() != plan.x) {
            if (!moveToward(plan.x)) {
                dropHere();          // 가는 길이 막혔으면 지금 자리에 떨어뜨린다
            }
        } else if (Math.random() < hardDropChance) {
            dropHere();
        } else {
            game.moveDown();
        }
    }

    /** 목표 열 쪽으로 한 칸 옮긴다. 못 옮기면 false. */
    private boolean moveToward(int targetX) {
        return game.getCurX() < targetX ? game.moveRight() : game.moveLeft();
    }

    /** 지금 자리에 바로 떨어뜨리고 계획을 버린다. */
    private void dropHere() {
        game.hardDrop();
        plan = null;

    }
}