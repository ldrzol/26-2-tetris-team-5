package kr.ac.jbnu.se.tetris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** 활성화된 기믹을 관리하고, 각 훅을 모아서 GameBoard / Board 에 알려 준다. */
public class Gimmick {

    /** 플레이어가 누른 방향키가 어떤 동작인지 나타낸다. */
    public enum Input { LEFT, RIGHT, ROTATE_LEFT, ROTATE_RIGHT }

    private static final int MAX_ACTIVE = 3;   // 동시에 켜져 있을 수 있는 기믹 수. 넘으면 가장 오래된 것이 꺼진다.

    // 전체 기믹 목록. 새 기믹은 여기에 한 줄만 추가한다.
    private final List<GimmickEffect> pool = Arrays.asList(
            new SpeedUp(),
            new FadeBlocks(),
            new NoRotate(),
            new ReverseKeys(),
            new InvisiblePiece(),
            new FlipBoard(),
            new PlusLine(),
            new CoverScreen(),
            new EraseDot(),
            new EraseCross(),
            new EraseSquare(),
            new EraseLine()
    );

    private final List<GimmickEffect> available = new ArrayList<>(pool);   // 아직 안 켜진 기믹
    private final List<GimmickEffect> active = new ArrayList<>();          // 지금 켜져 있는 기믹 (오래된 순)
    private final Random rand = new Random();
    private final GameBoard game;
    private int appliedLevel = 1;

    public Gimmick(GameBoard game) {
        this.game = game;
    }

    /** 레벨이 오른 만큼 기믹을 무작위로 하나씩 추가한다. (Board.afterAction 에서 매번 불러도 안전) */
    public void applyLevel(int level) {
        while (appliedLevel < level) {
            appliedLevel++;
            addRandom();
        }
    }

    /** 아직 켜지지 않은 기믹 중 하나를 무작위로 추가한다. */
    public void addRandom() {
        if (available.isEmpty()) return;
        add(available.get(rand.nextInt(available.size())));
    }

    /** 기믹을 추가한다. 이미 켜져 있으면 무시하고, 가득 차 있으면 가장 오래된 기믹을 끈다. */
    void add(GimmickEffect effect) {
        if (active.contains(effect)) return;
        if (active.size() >= MAX_ACTIVE)
            available.add(active.remove(0));
        available.remove(effect);
        active.add(effect);
        effect.onActivate(game);
    }

    /** 새 게임을 시작할 때 초기화한다. */
    public void clear() {
        active.clear();
        available.clear();
        available.addAll(pool);
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
        for (GimmickEffect e : active) if (e.hidesPiece(game)) return true;
        return false;
    }

    /** true 면 판을 180도 돌려서 그린다. */
    public boolean flipsBoard() {
        for (GimmickEffect e : active) if (e.flipsBoard()) return true;
        return false;
    }

    /** 화면을 덮는 비율(0~1). 켜진 기믹 중 가장 큰 값. */
    public double coverRatio() {
        double r = 0;
        for (GimmickEffect e : active) r = Math.max(r, e.coverRatio());
        return r;
    }

    public double speedFactor() {
        double f = 1.0;
        for (GimmickEffect e : active) f *= e.speedFactor();
        return f;
    }

    /** 지금 켜져 있는 기믹들 (오래된 순). 화면에 아이콘이나 이름을 그릴 때 쓴다. */
    List<GimmickEffect> activeEffects() {
        return java.util.Collections.unmodifiableList(active);
    }

    /**
     * 자가 점검. java -ea 로 실행한다.
     *
     * 아이콘은 없어도 그냥 안 그려지기만 해서 오타를 눈으로 잡기 어렵다. 그래서
     * 적어 둔 경로가 실제로 있는지만 확인한다. (게임은 돌리지 않는다)
     */
    public static void main(String[] args) {
        for (GimmickEffect e : new Gimmick(null).pool)
            assert e.icon() == null || Gimmick.class.getResource(e.icon()) != null
                    : e.name() + " 의 아이콘 파일이 없다: " + e.icon();

        for (Item item : Item.values())
            assert item.icon == null || Item.class.getResource(item.icon) != null
                    : item.label + " 의 아이콘 파일이 없다: " + item.icon;

        System.out.println("ok");
    }

}

// ====================================================================
// 아래는 같은 패키지 안에서만 쓰는 기믹 클래스들 (public 아님)
// ====================================================================

/** 기믹 하나가 구현하는 인터페이스. 필요한 훅만 오버라이드하면 된다. */
interface GimmickEffect {

    /** 이름 (상태 표시용). */
    String name();

    /** 화면에 띄울 아이콘 경로. 그림이 아직 없으면 null 이고, 그때는 안 그린다. */
    default String icon() { return null; }

    /** 이 기믹이 켜지는 순간 한 번 호출된다. (일회성 효과용) */
    default void onActivate(GameBoard game) {}

    /** 켜져 있는 동안 1초마다 호출된다. */
    default void onSecond(GameBoard game) {}

    /** 입력 변환. 바꿀 게 없으면 그대로 돌려준다. */
    default Gimmick.Input remap(Gimmick.Input in) { return in; }

    /** true 면 회전이 막힌다. */
    default boolean blocksRotation() { return false; }

    /** true 면 떨어지는 블록과 고스트가 화면에서 숨겨진다. */
    default boolean hidesPiece(GameBoard game) { return false; }

    /** true 면 판이 180도 돌아간다. (화면만 돌고 게임 규칙은 그대로) */
    default boolean flipsBoard() { return false; }

    /** 낙하 간격에 곱해진다. 1.0보다 작을수록 빨라진다. */
    default double speedFactor() { return 1.0; }

    /** 화면 위를 덮는 비율(0~1). 0이면 안 덮는다. */
    default double coverRatio() { return 0; }
}

/** 낙하 간격을 절반으로 줄인다. */
class SpeedUp implements GimmickEffect {
    @Override public String name() { return "SPEED UP"; }
    @Override public String icon() { return "images/items/atk_speed.png"; }
    @Override public double speedFactor() { return 0.5; }
}

/** 쌓인 블록이 놓인 지 FADE_LIFE_SECONDS 초가 지나면 사라진다. (쓰레기 줄은 제외) */
class FadeBlocks implements GimmickEffect {

    private static final int FADE_LIFE_SECONDS = 15;

    @Override public String name() { return "FADE"; }
    @Override public String icon() { return "images/items/gim_fade.png"; }

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
    @Override public String icon() { return "images/items/atk_reverse.png"; }

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

/** 떨어지는 블록과 고스트를 숨긴다. 단, 블록이 나온 첫 1초 동안은 보인다. */
class InvisiblePiece implements GimmickEffect {
    private static final long VISIBLE_MS = 1000;

    @Override public String name() { return "INVISIBLE"; }
    @Override public String icon() { return "images/items/gim_invisible.png"; }
    @Override public boolean hidesPiece(GameBoard game) { return game.msSinceSpawn() >= VISIBLE_MS; }
}

/** 판을 180도 돌리고, 화면과 맞도록 좌↔우 입력을 뒤집는다. (회전 방향은 180도 돌려도 같아서 그대로) */
class FlipBoard implements GimmickEffect {
    @Override public String name() { return "FLIP 180"; }
    @Override public String icon() { return "images/items/gim_gravity.png"; }
    @Override public boolean flipsBoard() { return true; }

    @Override
    public Gimmick.Input remap(Gimmick.Input in) {
        switch (in) {
            case LEFT:  return Gimmick.Input.RIGHT;
            case RIGHT: return Gimmick.Input.LEFT;
            default:    return in;
        }
    }
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
        game.liftPieceIfBlocked();   // 올라온 줄과 떨어지는 블록이 겹치면 블록을 위로 올린다
    }
}

/** 보드 아래쪽 절반을 5초 중 3초 동안 가린다. */
class CoverScreen implements GimmickEffect {
    private int sec = 0;

    @Override public String name() { return "COVER"; }
    @Override public String icon() { return "images/items/atk_ink.png"; }
    @Override public void onActivate(GameBoard game) { sec = 0; }
    @Override public void onSecond(GameBoard game) { sec++; }
    @Override public double coverRatio() { return (sec % 5) < 3 ? 0.5 : 0; }
}

/** 일정 시간마다 쌓인 블록 하나를 중심으로 모양대로 지우는 기믹의 공통 부분. */
abstract class EraseShape implements GimmickEffect {
    private static final int INTERVAL_SEC = 8;
    private int sec = 0;

    /** 중심 기준 상대 좌표(dx, dy) 목록. 지울 때마다 호출된다. */
    abstract int[][] offsets();

    @Override public String icon() { return "images/items/atk_bomb.png"; }

    @Override public void onActivate(GameBoard game) { sec = 0; }

    @Override
    public void onSecond(GameBoard game) {
        if (game.isClearing()) return;          // 줄 삭제 깜빡임 중에는 건드리지 않는다
        if (++sec < INTERVAL_SEC) return;
        sec = 0;
        int[] c = game.randomStackedCell();
        if (c == null) return;
        for (int[] o : offsets())
            game.eraseCell(c[0] + o[0], c[1] + o[1]);
    }
}

/** 1x1 : 한 칸만 지운다. */
class EraseDot extends EraseShape {
    @Override public String name() { return "ERASE 1x1"; }
    @Override public String icon() { return "images/items/atk_tiny.png"; }
    @Override int[][] offsets() { return new int[][] { { 0, 0 } }; }
}

/** 십자가 : 중심과 상하좌우 한 칸씩. */
class EraseCross extends EraseShape {
    @Override public String name() { return "ERASE CROSS"; }
    @Override int[][] offsets() {
        return new int[][] { { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    }
}

/** 정사각형 : 중심 주변 3x3. */
class EraseSquare extends EraseShape {
    @Override public String name() { return "ERASE SQUARE"; }
    @Override int[][] offsets() {
        int[][] o = new int[9][];
        int n = 0;
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                o[n++] = new int[] { dx, dy };
        return o;
    }
}

/** 일자 : 4칸을 가로 또는 세로로 무작위로 지운다. */
class EraseLine extends EraseShape {
    private final Random rand = new Random();
    @Override public String name() { return "ERASE LINE"; }
    @Override int[][] offsets() {
        boolean horizontal = rand.nextBoolean();
        int[][] o = new int[4][];
        for (int i = 0; i < 4; i++)
            o[i] = horizontal ? new int[] { i, 0 } : new int[] { 0, i };
        return o;
    }
}