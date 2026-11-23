package kr.ac.jbnu.se.tetris.game;

/** 보관함. 블록 하나당 한 번만 쓸 수 있다는 규칙(잠금)도 여기서 관리한다. */
public class HoldSlot {

    private Tetrominoes shape = Tetrominoes.NoShape;   // 비어 있으면 NoShape
    private int color;
    private boolean locked = false;

    public boolean isEmpty() {
        return shape == Tetrominoes.NoShape;
    }

    public Tetrominoes shape() {
        return shape;
    }

    public int color() {
        return color;
    }

    public boolean isLocked() {
        return locked;
    }

    /** 새 블록이 나왔으니 다시 쓸 수 있게 한다. */
    public void unlock() {
        locked = false;
    }

    /** 블록을 맡기고 이번 블록에서는 더 못 쓰게 잠근다. */
    public void store(Tetrominoes newShape, int newColor) {
        shape = newShape;
        color = newColor;
        locked = true;
    }

    public void clear() {
        shape = Tetrominoes.NoShape;
        color = 0;
        locked = false;
    }
}
