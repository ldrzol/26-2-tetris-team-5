package kr.ac.jbnu.se.tetris.ai;

import kr.ac.jbnu.se.tetris.GameBoard;
import kr.ac.jbnu.se.tetris.Shape;
import kr.ac.jbnu.se.tetris.Tetrominoes;

public class TetrisAI {
    public Move findBestMove(GameBoard board, Shape piece) {
        int width = GameBoard.WIDTH;
        int height = GameBoard.HEIGHT;
        Move bestMove=null;
        int bestScore = Integer.MIN_VALUE;
        Shape shape = piece;
        for (int rotation=0; rotation<4; rotation++){
            for(int curX =0; curX<width; curX++){
                int landingY = findLandingY(board, shape, curX);
                if(landingY==-1){
                    continue;
                }
                Tetrominoes[] simulated = simulatePlace(board, shape, curX, landingY);
                int score = countClearedLines(simulated, width, height) * 10
                        - calculateHeight(simulated, width, height)
                        - countHoles(simulated, width, height) * 5
                        - countBumpiness(simulated, width, height);
                if (score>bestScore){
                    bestScore=score;
                    bestMove=new Move(rotation, curX);
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
        for (int i = 0; i < 4; i++) {
            int x = curX + shape.x(i);
            int y = curY - shape.y(i);
            if (x < 0 || x >=GameBoard.WIDTH || y < 0 || y >= GameBoard.HEIGHT) {
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

        for (int i = 0; i < 4; i++) {
            int x = curX + shape.x(i);
            int y = landingY - shape.y(i);
            copy[y * width + x] = shape.getShape();
        }

        return copy;
    }
    private int countClearedLines(Tetrominoes[] simulatedBoard, int width, int height) {
        int count = 0;
        for (int y = 0; y < height; y++) {
            boolean full = true;
            for (int x = 0; x < width; x++) {
                if (simulatedBoard[y * width + x] == Tetrominoes.NoShape) {
                    full = false;
                    break;
                }
            }
            if (full) {
                count++;
            }
        }
        return count;
    }
    private int calculateHeight(Tetrominoes[] simulatedBoard, int width, int height){
        for (int y= height-1; y>=0; y--){
            for(int x=0; x<width; x++){
                if(simulatedBoard[y*width+x] != Tetrominoes.NoShape)
                    return y+1;
            }
        }
        return 0;
    }
    private int countHoles(Tetrominoes[] simulatedBoard, int width, int height) {
        int holes = 0;
        for (int x = 0; x < width; x++) {
            boolean blockFound = false;
            for (int y = height - 1; y >= 0; y--) {
                if (simulatedBoard[y * width + x] != Tetrominoes.NoShape) {
                    blockFound = true;
                } else if (blockFound) {
                    holes++;
                }
            }
        }
        return holes;
    }
    private int countBumpiness(Tetrominoes[] simulatedBoard, int width, int height) {
        int[] tops = new int[width];
        for (int x = 0; x < width; x++) {
            for (int y = height - 1; y >= 0; y--) {
                if (simulatedBoard[y * width + x] != Tetrominoes.NoShape) {
                    tops[x] = y + 1;
                    break;
                }
            }
        }

        int bumpiness = 0;
        for (int x = 0; x < width - 1; x++) {
            bumpiness += Math.abs(tops[x] - tops[x + 1]);
        }
        return bumpiness;
    }
}