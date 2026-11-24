package kr.ac.jbnu.se.tetris.game;

import javax.swing.Timer;

/** 바닥에 닿은 블록이 굳기까지의 유예 시간. 움직여서 되돌릴 수 있는 횟수에도 한도가 있다. */
public class LockDelay {

    private static final int LOCK_DELAY = 500;
    private static final int MAX_RESETS = 15;

    private final Timer timer;
    private int resets = 0;

    public LockDelay(Runnable onTimeout) {
        timer = new Timer(LOCK_DELAY, e -> onTimeout.run());
        timer.setRepeats(false);
    }

    /** 바닥에 닿았을 때. 이미 돌고 있으면 그대로 둔다. */
    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        timer.stop();
    }

    /** 멈추고 되돌리기 횟수도 0으로 되돌린다. */
    public void clear() {
        timer.stop();
        resets = 0;
    }

    /** 바닥에서 움직이면 유예를 다시 준다. MAX_RESETS 번까지만. */
    public void reset() {
        if (timer.isRunning() && resets < MAX_RESETS) {
            resets++;
            timer.restart();
        }
    }
}
