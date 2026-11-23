package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

/** 학습한 신경망으로 점수를 매긴다. 입력은 BoardFeatures 가 뽑은 숫자 4개. */
public class NeuralEvaluator implements Evaluator {

    private final NeuralNet net;

    public NeuralEvaluator(NeuralNet net) {
        this.net = net;
    }

    @Override
    public double score(Tetrominoes[] board, int width, int height) {
        return net.score(BoardFeatures.of(board, width, height));
    }
}