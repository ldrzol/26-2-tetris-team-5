package kr.ac.jbnu.se.tetris.game;

import javax.swing.Timer;

/**
 * 게임 한 판이 쓰는 타이머 3개를 한 곳에서 켜고 끈다.
 * 낙하(레벨에 따라 간격이 바뀜), 기믹용 1초, 숨김 기믹용 짧은 다시 그리기.
 */
public class GameClock {

    private static final int GIMMICK_INTERVAL = 1000;
    private static final int VISIBILITY_INTERVAL = 100;

    private final Timer fall;
    private final Timer gimmick;
    private final Timer visibility;

    public GameClock(Runnable onFall, Runnable onGimmickSecond, Runnable onVisibilityTick) {
        fall = new Timer(FallSpeed.BASE_DELAY, e -> onFall.run());
        gimmick = new Timer(GIMMICK_INTERVAL, e -> onGimmickSecond.run());
        visibility = new Timer(VISIBILITY_INTERVAL, e -> onVisibilityTick.run());
    }

    /** 새 판 시작. 기믹용 타이머는 처음부터 다시 센다. */
    public void startAll() {
        fall.start();
        gimmick.restart();
        visibility.restart();
    }

    public void stopAll() {
        fall.stop();
        gimmick.stop();
        visibility.stop();
    }

    /** 일시정지. 기믹용 타이머는 돌려 두되 호출하는 쪽에서 무시한다. */
    public void pauseFall() {
        fall.stop();
    }

    public void resumeFall() {
        fall.start();
    }

    public void setFallDelay(int ms) {
        if (fall.getDelay() != ms) {
            fall.setDelay(ms);
        }
    }
}
