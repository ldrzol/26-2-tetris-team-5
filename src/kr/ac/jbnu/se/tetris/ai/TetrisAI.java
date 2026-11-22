package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Shape;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import java.io.IOException;

public class TetrisAI {

    private final Evaluator evaluator;

    /** 기본: 가장 잘하는 신경망 (model/weights.txt). */
    public TetrisAI() {
        this(NeuralNet.WEIGHTS);
    }

    /** 이 가중치 파일의 신경망을 쓴다. 못 읽으면 기존 점수식으로 대신한다 (게임이 멈추지 않게). */
    public TetrisAI(String weightsPath) {
        this(loadEvaluator(weightsPath));
    }

    public TetrisAI(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    private static Evaluator loadEvaluator(String weightsPath) {
        try {
            return new NeuralEvaluator(new NeuralNet(weightsPath));
        } catch (IOException | NumberFormatException e) {
            System.err.println("신경망을 읽지 못해 기존 점수식을 씁니다: " + e.getMessage());
            return new HeuristicEvaluator();
        }
    }

    public Move findBestMove(GameBoard board, Shape piece) {
        int width = GameBoard.WIDTH;
        int height = GameBoard.HEIGHT;
        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        Shape shape = piece;
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int curX = 0; curX < width; curX++) {
                int landingY = findLandingY(board, shape, curX);
                if (landingY == -1) {
                    continue;
                }
                Tetrominoes[] simulated = simulatePlace(board, shape, curX, landingY);
                double score = evaluator.score(simulated, width, height);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = new Move(rotation, curX);
                }
            }
            shape = shape.rotateRight();
        }
        return bestMove;
    }

    public Move findRandomMove(GameBoard board, Shape piece) {
        int width = GameBoard.WIDTH;
        for (int attempt = 0; attempt < 50; attempt++) {
            int rotation = (int) (Math.random() * 4);
            int curX = (int) (Math.random() * width);

            Shape shape = piece;
            for (int i = 0; i < rotation; i++) {
                shape = shape.rotateRight();
            }

            if (findLandingY(board, shape, curX) != -1) {
                return new Move(rotation, curX);
            }
        }
        return findBestMove(board, piece);
    }

    private boolean canPlace(GameBoard board, Shape shape, int curX, int curY) {
        for (int i = 0; i < shape.size(); i++) {
            int x = curX + shape.x(i);
            int y = curY - shape.y(i);
            if (x < 0 || x >= GameBoard.WIDTH || y < 0 || y >= GameBoard.HEIGHT) {
                return false;
            }
            if (board.shapeAt(x, y) != Tetrominoes.NoShape) {
                return false;
            }
        }
        return true;
    }

    private int findLandingY(GameBoard board, Shape shape, int curX) {
        int y = GameBoard.HEIGHT - 1 + shape.minY();
        if (!canPlace(board, shape, curX, y)) {
            return -1;
        }
        while (canPlace(board, shape, curX, y - 1)) {
            y--;
        }
        return y;
    }

    private Tetrominoes[] simulatePlace(GameBoard board, Shape shape, int curX, int landingY) {
        int width = GameBoard.WIDTH;
        int height = GameBoard.HEIGHT;

        Tetrominoes[] copy = new Tetrominoes[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                copy[y * width + x] = board.shapeAt(x, y);
            }
        }

        for (int i = 0; i < shape.size(); i++) {
            int x = curX + shape.x(i);
            int y = landingY - shape.y(i);
            copy[y * width + x] = shape.getShape();
        }

        return copy;
    }
}