package kr.ac.jbnu.se.tetris.game;

/** 레벨과 기믹 배율로 낙하 간격(ms)을 계산한다. */
public final class FallSpeed {

    public static final int BASE_DELAY = 400;
    private static final int MIN_DELAY = 100;
    private static final int DELAY_STEP_PER_LEVEL = 30;

    private FallSpeed() {
    }

    public static int delayFor(int level, double speedFactor) {
        int delay = Math.max(MIN_DELAY, BASE_DELAY - (level - 1) * DELAY_STEP_PER_LEVEL);
        return Math.max(MIN_DELAY / 2, (int) (delay * speedFactor));
    }
}
