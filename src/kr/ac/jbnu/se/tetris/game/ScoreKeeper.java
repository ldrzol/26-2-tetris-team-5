package kr.ac.jbnu.se.tetris.game;

/** 점수, 콤보, 레벨, 지운 줄 수, 피버 신호를 관리한다. 보드 모양은 모르고 숫자만 다룬다. */
public class ScoreKeeper {

    /** 레벨 상한. 가이드라인 마라톤과 같게 15에서 멈춘다. */
    public static final int MAX_LEVEL = 15;

    /** 이 콤보부터 피버가 터진다. 한 번에 4줄(테트리스)로도 터진다. */
    public static final int FEVER_COMBO = 4;

    private static final int[] LINE_POINTS    = { 0, 100, 300, 500, 800 };    // 지운 줄 수별 기본 점수 (x 레벨)
    private static final int[] PERFECT_POINTS = { 0, 800, 1200, 1800, 2000 }; // 퍼펙트 클리어 보너스 (x 레벨)
    private static final int COMBO_POINTS = 50;                               // 콤보 보너스 (x 콤보 x 레벨)

    private int score = 0;
    private int combo = -1;                  // 연속으로 줄을 지운 횟수 (-1: 연속 아님)
    private int linesRemoved = 0;
    private boolean levelProgression = true; // false 면 레벨이 1로 고정된다
    private boolean feverPending = false;    // 조건을 채웠고 아직 화면이 가져가지 않은 상태

    /**
     * 레벨 n 을 끝내는 데 필요한 '누적' 점수.
     *
     * 가이드라인은 레벨당 10줄이고 한 줄(싱글) 점수가 100 x 레벨이므로,
     * 레벨 n 을 깨는 데 드는 점수는 1000 x n 이다. 그것을 누적하면
     * 500 x n x (n+1) 이 된다. (1000, 3000, 6000, 10000, 15000 ...)
     */
    public static int levelGoal(int level) {
        return 500 * level * (level + 1);
    }

    /** 새 판. 점수와 줄 수까지 전부 0 으로 되돌린다. */
    public void reset() {
        score = 0;
        linesRemoved = 0;
        resetStreak();
    }

    /** 레벨 클리어 뒤처럼 점수·줄 수는 두고 콤보와 피버만 끊는다. */
    public void resetStreak() {
        combo = -1;
        feverPending = false;
    }

    /** 2인 대전처럼 레벨이 오르면 안 되는 판에서는 false 로 끈다. */
    public void setLevelProgression(boolean on) {
        levelProgression = on;
    }

    /** 현재 레벨. 점수가 levelGoal 을 넘을 때마다 오른다. 레벨 올리기를 껐으면 늘 1이다. */
    public int level() {
        if (!levelProgression)
            return 1;
        int level = 1;
        while (level < MAX_LEVEL && score >= levelGoal(level))
            level++;
        return level;
    }

    public int score() {
        return score;
    }

    public int combo() {
        return combo;
    }

    public int linesRemoved() {
        return linesRemoved;
    }

    /** 줄을 못 지우고 블록이 굳었을 때. */
    public void breakCombo() {
        combo = -1;
    }

    /** 점수 배수를 적용해서 더한다. 점수가 붙는 곳은 전부 여기를 거친다. */
    public void add(int points, double factor) {
        score += (int) Math.round(points * factor);
    }

    /**
     * n 줄이 지워졌을 때 줄·콤보·퍼펙트 점수를 계산해 더하고 피버 조건을 본다.
     * 점수는 줄을 더하기 전 레벨로 계산한다.
     */
    public void lineCleared(int n, boolean perfect, double factor) {
        int level = level();
        linesRemoved += n;

        int gained = LINE_POINTS[n] * level;                 // 줄 삭제
        combo++;
        gained += COMBO_POINTS * combo * level;              // 콤보 (첫 삭제는 combo=0 이라 0점)
        if (perfect)
            gained += PERFECT_POINTS[n] * level;             // 퍼펙트 클리어
        add(gained, factor);

        // 테트리스(4줄)이거나 콤보가 막 기준에 닿았을 때. 콤보가 더 길어져도 다시 터지지 않게
        // '넘은' 것이 아니라 '닿은' 순간만 본다. 점수 배수가 이미 걸려 있으면 겹치지 않게 건너뛴다.
        if ((n == 4 || combo == FEVER_COMBO) && factor == 1.0)
            feverPending = true;
    }

    /** 피버 조건을 채웠으면 한 번만 true 를 돌려준다. (가져가면 꺼진다) */
    public boolean takeFever() {
        boolean on = feverPending;
        feverPending = false;
        return on;
    }
}
