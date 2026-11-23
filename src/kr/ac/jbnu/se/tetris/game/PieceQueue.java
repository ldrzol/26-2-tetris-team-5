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

    public PieceQueue() {
        this(new Random());
    }

    /** 테스트에서 시드를 고정할 수 있도록 난수 생성기를 주입받는다. */
    public PieceQueue(Random random) {
        this.random = random;
    }

    public Tetrominoes next() {
        refill();
        Tetrominoes next = queue.pollFirst();
        refill();
        return next;
    }

    public void clear() {
        queue.clear();
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
