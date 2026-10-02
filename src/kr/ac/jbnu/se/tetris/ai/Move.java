package kr.ac.jbnu.se.tetris.ai;

public class Move {
    public int rotationCount;
    public int x;
    public Move(int rotationCount, int x){
        this.rotationCount = rotationCount;
        this.x = x;
    }

}
