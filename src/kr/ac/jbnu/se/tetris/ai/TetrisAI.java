package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Shape;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        Set<String> tried = new HashSet<>();   // 이미 시험한 자리 (O 블록처럼 돌려도 같은 모양이면 자리가 겹친다)
        Shape shape = piece;
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int curX = 0; curX < width; curX++) {
                int landingY = findLandingY(board, shape, curX);
                if (landingY == -1) {
                    continue;
                }
                if (!tried.add(placementKey(shape, curX, landingY))) {
                    continue;   // 다른 회전으로 이미 시험한 것과 똑같은 자리
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

    /** 블록이 놓일 칸 번호들을 정렬해서 글자로 만든다. 칸이 같으면 같은 글자가 된다. */
    private String placementKey(Shape shape, int curX, int landingY) {
        int[] cells = new int[shape.size()];
        for (int i = 0; i < shape.size(); i++) {
            cells[i] = (landingY - shape.y(i)) * GameBoard.WIDTH + (curX + shape.x(i));
        }
        Arrays.sort(cells);
        return Arrays.toString(cells);
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