package kr.ac.jbnu.se.tetris.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** 7-bag 방식으로 다음 블록을 공급한다. 7종을 한 번씩 섞어 넣고, 큐가 모자라면 다시 채운다. */
public class PieceQueue {

    private static final int QUEUE_MIN = 7;

    private final Deque<Tetrominoes> queue = new ArrayDeque<>();
    private final Random random;
    private final int colorCount;
    private Tetrominoes nextShape = Tetrominoes.NoShape;   // 미리 보여 주는 다음 블록
    private int nextColor;

    public PieceQueue(int colorCount) {
        this(new Random(), colorCount);
    }

    /** 테스트에서 시드를 고정할 수 있도록 난수 생성기를 주입받는다. */
    public PieceQueue(Random random, int colorCount) {
        this.random = random;
        this.colorCount = colorCount;
    }

    /** 미리 보여 주는 다음 블록. 아직 뽑기 전이면 NoShape. */
    public Tetrominoes nextShape() {
        return nextShape;
    }

    public int nextColor() {
        return nextColor;
    }

    /** 다음 블록을 큐에서 새로 뽑는다. 색은 따로 무작위로 고른다. */
    public void advance() {
        nextShape = next();
        nextColor = random.nextInt(colorCount);
    }

    /** 다음 블록을 정해 준다. 색은 늘 하던 대로 새로 뽑는다. */
    public void setNext(Tetrominoes shape) {
        nextShape = shape;
        nextColor = random.nextInt(colorCount);
    }

    public Tetrominoes next() {
        refill();
        Tetrominoes next = queue.pollFirst();
        refill();
        return next;
    }

    public void clear() {
        queue.clear();
        nextShape = Tetrominoes.NoShape;
    }

    private void refill() {
        while (queue.size() < QUEUE_MIN) {
            List<Tetrominoes> bag = new ArrayList<>();
            for (Tetrominoes t : Tetrominoes.values()) {
                if (t != Tetrominoes.NoShape && t != Tetrominoes.GarbageShape) {
                    bag.add(t);
                }
            }
            Collections.shuffle(bag, random);
            queue.addAll(bag);
        }
    }
}
