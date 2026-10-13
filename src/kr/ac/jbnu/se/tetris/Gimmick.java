package kr.ac.jbnu.se.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** 활성화된 기믹을 관리하고, 각 훅을 모아서 GameBoard / Board 에 알려 준다. */
public class Gimmick {

    /** 플레이어가 누른 방향키가 어떤 동작인지 나타낸다. */
    public enum Input { LEFT, RIGHT, ROTATE_LEFT, ROTATE_RIGHT }

    // 레벨 2부터 이 순서대로 하나씩 돌아가며 적용된다. 새 기믹은 여기에 한 줄만 추가한다.
    private final List<GimmickEffect> pool = Arrays.asList(
            new SpeedUp(),
            new FadeBlocks(),
            new NoRotate(),
            new ReverseKeys(),
            new InvisiblePiece(),
            new PlusLine()
    );


    private final List<GimmickEffect> active = new ArrayList<>();
    private final GameBoard game;
    private int appliedLevel = 1;

    public Gimmick(GameBoard game) {
        this.game = game;
    }

    /** 레벨이 바뀐 경우에만 기믹을 새로 정한다. (Board.afterAction 에서 매번 불러도 안전) */
    public void applyLevel(int level) {
        if (level == appliedLevel) return;
        appliedLevel = level;
        active.clear();
        if (level >= 2) {
            GimmickEffect e = pool.get((level - 2) % pool.size());
            active.add(e);
            e.onActivate(game);
        }
    }

    /** 새 게임을 시작할 때 초기화한다. */
    public void clear() {
        active.clear();
        appliedLevel = 1;
    }

    /** 1초마다 Board 가 호출한다. */
    public void onSecond() {
        for (GimmickEffect e : active) e.onSecond(game);
    }

    /** 켜진 기믹들을 차례로 거쳐 입력을 바꾼다. */
    public Input remap(Input in) {
        for (GimmickEffect e : active) in = e.remap(in);
        return in;
    }

    public boolean blocksRotation() {
        for (GimmickEffect e : active) if (e.blocksRotation()) return true;
        return false;
    }

    public boolean hidesPiece() {
        for (GimmickEffect e : active) if (e.hidesPiece()) return true;
        return false;
    }

    public double speedFactor() {
        double f = 1.0;
        for (GimmickEffect e : active) f *= e.speedFactor();
        return f;
    }

    /** 화면에 현재 기믹 이름을 보여 주고 싶을 때 쓴다. 없으면 빈 문자열. */
    public String activeNames() {
        StringBuilder sb = new StringBuilder();
        for (GimmickEffect e : active) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.name());
        }
        return sb.toString();
    }
}

// ====================================================================
// 아래는 같은 패키지 안에서만 쓰는 기믹 클래스들 (public 아님)
// ====================================================================

/** 기믹 하나가 구현하는 인터페이스. 필요한 훅만 오버라이드하면 된다. */
interface GimmickEffect {

    /** 이름 (상태 표시용). */
    String name();

    /** 이 기믹이 켜지는 순간 한 번 호출된다. (일회성 효과용) */
    default void onActivate(GameBoard game) {}

    /** 켜져 있는 동안 1초마다 호출된다. */
    default void onSecond(GameBoard game) {}

    /** 입력 변환. 바꿀 게 없으면 그대로 돌려준다. */
    default Gimmick.Input remap(Gimmick.Input in) { return in; }

    /** true 면 회전이 막힌다. */
    default boolean blocksRotation() { return false; }

    /** true 면 떨어지는 블록과 고스트가 화면에서 숨겨진다. */
    default boolean hidesPiece() { return false; }

    /** 낙하 간격에 곱해진다. 1.0보다 작을수록 빨라진다. */
    default double speedFactor() { return 1.0; }
}

/** 낙하 간격을 절반으로 줄인다. */
class SpeedUp implements GimmickEffect {
    @Override public String name() { return "SPEED UP"; }
    @Override public double speedFactor() { return 0.5; }
}

/** 쌓인 블록이 놓인 지 FADE_LIFE_SECONDS 초가 지나면 사라진다. (쓰레기 줄은 제외) */
class FadeBlocks implements GimmickEffect {

    private static final int FADE_LIFE_SECONDS = 15;

    @Override public String name() { return "FADE"; }

    @Override
    public void onSecond(GameBoard game) {
        if (game.isClearing()) return;   // 줄 삭제 깜빡임 중에는 건드리지 않는다
        for (int y = 0; y < GameBoard.HEIGHT; y++) {
            for (int x = 0; x < GameBoard.WIDTH; x++) {
                Tetrominoes t = game.cells[y][x];
                if (t == Tetrominoes.NoShape || t == Tetrominoes.GarbageShape) continue;
                if (++game.age[y][x] >= FADE_LIFE_SECONDS) {
                    game.cells[y][x] = Tetrominoes.NoShape;
                    game.colorIds[y][x] = 0;
                    game.age[y][x] = 0;
                }
            }
        }
    }
}

/** 회전을 막는다. */
class NoRotate implements GimmickEffect {
    @Override public String name() { return "NO ROTATE"; }
    @Override public boolean blocksRotation() { return true; }
}

/** 좌↔우, 반시계↔시계 회전 입력을 서로 바꾼다. */
class ReverseKeys implements GimmickEffect {
    @Override public String name() { return "REVERSE KEYS"; }

    @Override
    public Gimmick.Input remap(Gimmick.Input in) {
        switch (in) {
            case LEFT:         return Gimmick.Input.RIGHT;
            case RIGHT:        return Gimmick.Input.LEFT;
            case ROTATE_LEFT:  return Gimmick.Input.ROTATE_RIGHT;
            case ROTATE_RIGHT: return Gimmick.Input.ROTATE_LEFT;
            default:           return in;
        }
    }
}

/** 떨어지는 블록과 고스트를 숨긴다. */
class InvisiblePiece implements GimmickEffect {
    @Override public String name() { return "INVISIBLE"; }
    @Override public boolean hidesPiece() { return true; }
}

/** 켜지는 순간 맨 아래에 구멍 하나 뚫린 쓰레기 줄을 한 번 끼워 넣는다. */
class PlusLine implements GimmickEffect {

    private final Random rand = new Random();

    @Override public String name() { return "PLUS LINE"; }

    @Override
    public void onActivate(GameBoard game) {
        int hole = rand.nextInt(GameBoard.WIDTH);
        game.upLine(0);
        for (int i = 0; i < GameBoard.WIDTH; i++) {
            game.cells[0][i] = (i == hole) ? Tetrominoes.NoShape : Tetrominoes.GarbageShape;
        }
    }
}
