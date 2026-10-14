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

    // 레벨이 오를 때 무작위로 뽑는 기믹 목록. 새 기믹은 여기에 한 줄만 추가한다.
    // 아이템 전용 효과(잠그기, 점수 2배 등)는 넣지 않는다. 혼자 하는 판에서 저절로
    // 걸리면 곤란하기 때문이다.
    private final List<GimmickEffect> pool = Arrays.asList(
            new SpeedUp(),
            new FadeBlocks(),
            new NoRotate(),
            new ReverseKeys(),
            new InvisiblePiece(),
            new FlipBoard(),
            new PlusLine(),
            new CoverScreen(),
            new RandomKeys(),
            new SpinBlock(),
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
            remove(active.get(0));
        available.remove(effect);
        active.add(effect);
        effect.onActivate(game);
    }

    /**
     * 기믹을 끈다. 레벨에서 뽑는 기믹이면 다시 뽑힐 수 있게 돌려 놓고,
     * 아이템으로 생긴 일회성 효과면 그냥 버린다.
     */
    private void remove(GimmickEffect effect) {
        if (!active.remove(effect))
            return;
        if (pool.contains(effect))
            available.add(effect);
    }

    /** 새 게임을 시작할 때 초기화한다. */
    public void clear() {
        active.clear();
        available.clear();
        available.addAll(pool);
        appliedLevel = 1;
    }

    /** 1초마다 Board 가 호출한다. 시간이 다 된 효과는 여기서 꺼진다. */
    public void onSecond() {
        for (GimmickEffect e : new ArrayList<>(active)) {   // 도는 중에 목록이 바뀔 수 있다
            e.onSecond(game);
            if (e.expired())
                remove(e);
        }
    }

    /** 새 블록이 나올 때마다 GameBoard 가 호출한다. (블록마다 다시 뽑는 기믹용) */
    public void onSpawn() {
        for (GimmickEffect e : active) e.onSpawn(game);
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

    /** true 면 새 블록이 나오지 않는다. */
    public boolean blocksSpawn() {
        for (GimmickEffect e : active) if (e.blocksSpawn()) return true;
        return false;
    }

    /** true 면 블록 두 개가 한 덩어리로 같이 떨어진다. */
    public boolean doublesPiece() {
        for (GimmickEffect e : active) if (e.doublesPiece()) return true;
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

    /** 얻는 점수에 곱해진다. */
    public double scoreFactor() {
        double f = 1.0;
        for (GimmickEffect e : active) f *= e.scoreFactor();
        return f;
    }

    /**
     * 지금 켜져 있는 기믹들의 아이콘 경로 (오래된 순). 그림이 없는 기믹은 빠진다.
     *
     * 화면 쪽이 기믹 클래스를 직접 알 필요가 없도록 경로만 넘긴다.
     */
    public List<String> activeIcons() {
        List<String> icons = new ArrayList<>();
        for (GimmickEffect e : active)
            if (e.icon() != null)
                icons.add(e.icon());
        return icons;
    }

    /**
     * 아이템 효과를 이 보드에 건다. 걸 기믹이 없는 아이템(교환)이면 아무 일도 하지 않는다.
     *
     * 바깥에서 기믹 클래스를 직접 만들지 않아도 되도록 여기서 한 번에 처리한다.
     */
    public void apply(Attack attack) {
        GimmickEffect effect = effectFor(attack);
        if (effect != null)
            add(effect);
    }

    /**
     * 아이템 하나가 거는 기믹을 만든다. 쓸 때마다 새로 만들어야 남은 시간이 따로 돈다.
     *
     * 블록 교환(SWAP)은 그 자리에서 한 번에 끝나므로 걸 기믹이 없어 null 이다.
     */
    static GimmickEffect effectFor(Attack attack) {
        switch (attack) {
            case BOMB:        return randomBomb();
            case TINY:        return new EraseDot();
            case INK:         return new CoverScreen();
            case SPEED:       return new SpeedUp();
            case REVERSE:     return new ReverseKeys();
            case LOCK:        return new NoSpawn();
            case TWIN:        return new TwinPieces();
            case SCORE_X2:    return new ScoreBoost();
            case REST:        return new RestSpawn();
            case RANDOM_KEYS: return new RandomKeys();
            case SPIN:        return new SpinBlock();
            case FADE:        return new FadeBlocks();
            case GRAVITY:     return new FlipBoard();
            case FLOOR:       return new PlusLine();
            case INVISIBLE:   return new InvisiblePiece();
            case NO_ROTATE:   return new NoRotate();
            default:          return null;
        }
    }

    /** 폭탄은 터지는 모양을 무작위로 고른다. (한 칸짜리는 1X1 아이템이 따로 있다) */
    private static GimmickEffect randomBomb() {
        GimmickEffect[] shapes = { new EraseCross(), new EraseSquare(), new EraseLine() };
        return shapes[new Random().nextInt(shapes.length)];
    }

    /**
     * 자가 점검. java -ea 로 실행한다.
     *
     * 아이콘은 없어도 그냥 안 그려지기만 해서 오타를 눈으로 잡기 어렵다. 그래서
     * 적어 둔 경로가 실제로 있는지, 아이템마다 거는 기믹이 있는지를 확인한다.
     */
    public static void main(String[] args) {
        List<GimmickEffect> all = new ArrayList<>(new Gimmick(null).pool);

        for (Attack a : Attack.values()) {
            assert Gimmick.class.getResource(a.icon) != null
                    : a.label + " 의 아이콘 파일이 없다: " + a.icon;
            GimmickEffect e = effectFor(a);
            assert e != null || a == Attack.SWAP : a.label + " 에 대응하는 기믹이 없다";
            if (e != null)
                all.add(e);
        }

        for (GimmickEffect e : all)
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

    /** 새 블록이 나올 때마다 호출된다. (블록마다 다시 뽑는 기믹용) */
    default void onSpawn(GameBoard game) {}

    /** true 면 효과가 끝난 것으로 보고 Gimmick 이 꺼 준다. 시간 제한이 없으면 늘 false. */
    default boolean expired() { return false; }

    /** 입력 변환. 바꿀 게 없으면 그대로 돌려준다. */
    default Gimmick.Input remap(Gimmick.Input in) { return in; }

    /** true 면 회전이 막힌다. */
    default boolean blocksRotation() { return false; }

    /** true 면 떨어지는 블록과 고스트가 화면에서 숨겨진다. */
    default boolean hidesPiece(GameBoard game) { return false; }

    /** true 면 판이 180도 돌아간다. (화면만 돌고 게임 규칙은 그대로) */
    default boolean flipsBoard() { return false; }

    /** true 면 새 블록이 나오지 않는다. */
    default boolean blocksSpawn() { return false; }

    /** true 면 블록 두 개가 한 덩어리로 같이 떨어진다. */
    default boolean doublesPiece() { return false; }

    /** 낙하 간격에 곱해진다. 1.0보다 작을수록 빨라진다. */
    default double speedFactor() { return 1.0; }

    /** 얻는 점수에 곱해진다. */
    default double scoreFactor() { return 1.0; }

    /** 화면 위를 덮는 비율(0~1). 0이면 안 덮는다. */
    default double coverRatio() { return 0; }
}

/**
 * 시간이 지나면 저절로 꺼지는 효과의 공통 부분. 아이템으로 거는 효과들이 쓴다.
 *
 * 남은 시간은 1초 타이머로 센다. onActivate 에서 다시 채우므로 같은 아이템을
 * 또 맞으면 시간이 새로 시작한다.
 */
abstract class TimedEffect implements GimmickEffect {

    private int left;

    /** 몇 초 동안 이어질지. */
    abstract int seconds();

    /** 아직 효과가 살아 있는지. */
    boolean running() { return left > 0; }

    @Override public void onActivate(GameBoard game) { left = seconds(); }
    @Override public void onSecond(GameBoard game) { left--; }
    @Override public boolean expired() { return left <= 0; }
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
    @Override public String icon() { return "images/items/gim_norotate.png"; }
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

/**
 * 블록이 바뀔 때마다 좌우와 회전을 뒤집을지 말지를 새로 뽑는다.
 *
 * ReverseKeys 와 달리 늘 뒤집히지는 않아서, 블록이 나올 때마다 눌러 보고 알아내야 한다.
 */
class RandomKeys implements GimmickEffect {

    private final Random rand = new Random();
    private boolean swapMove, swapRotate;

    @Override public String name() { return "RANDOM KEYS"; }
    @Override public String icon() { return "images/items/gim_randomkey.png"; }

    @Override public void onActivate(GameBoard game) { reroll(); }
    @Override public void onSpawn(GameBoard game) { reroll(); }

    private void reroll() {
        swapMove = rand.nextBoolean();
        swapRotate = rand.nextBoolean();
    }

    @Override
    public Gimmick.Input remap(Gimmick.Input in) {
        switch (in) {
            case LEFT:         return swapMove ? Gimmick.Input.RIGHT : in;
            case RIGHT:        return swapMove ? Gimmick.Input.LEFT : in;
            case ROTATE_LEFT:  return swapRotate ? Gimmick.Input.ROTATE_RIGHT : in;
            case ROTATE_RIGHT: return swapRotate ? Gimmick.Input.ROTATE_LEFT : in;
            default:           return in;
        }
    }
}

/**
 * 블록이 저절로 돌아간다. 하드 드롭으로 떨어뜨리면 그 순간의 방향 그대로 굳는다.
 *
 * ponytail: 1초 타이머에 얹어서 1초에 한 번 돌린다. 더 빙글빙글 돌려야 하면
 * Board 에 짧은 타이머를 하나 더 두고 forceRotate() 를 부르면 된다.
 */
class SpinBlock implements GimmickEffect {
    @Override public String name() { return "SPIN"; }
    @Override public String icon() { return "images/items/gim_rotfix.png"; }
    @Override public void onSecond(GameBoard game) { game.forceRotate(); }
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
    @Override public String icon() { return "images/items/gim_floor.png"; }

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

/** [아이템] 잠시 새 블록이 나오지 않는다. 상대를 묶어 두는 자물쇠. */
class NoSpawn extends TimedEffect {
    @Override int seconds() { return 6; }
    @Override public String name() { return "LOCKED"; }
    @Override public String icon() { return "images/items/atk_block.png"; }
    @Override public boolean blocksSpawn() { return running(); }
}

/** [아이템] 블록을 멈추는 것은 같고, 내가 한숨 돌리려고 쓴다. 이름과 그림만 다르다. */
class RestSpawn extends NoSpawn {
    @Override public String name() { return "REST"; }
    @Override public String icon() { return "images/items/atk_nopiece.png"; }
}

/** [아이템] 잠시 얻는 점수가 두 배가 된다. */
class ScoreBoost extends TimedEffect {
    @Override int seconds() { return 15; }
    @Override public String name() { return "SCORE x2"; }
    @Override public String icon() { return "images/items/atk_double.png"; }
    @Override public double scoreFactor() { return running() ? 2.0 : 1.0; }
}

/** [아이템] 잠시 블록이 두 개씩 한 덩어리로 떨어진다. */
class TwinPieces extends TimedEffect {
    @Override int seconds() { return 12; }
    @Override public String name() { return "TWIN"; }
    @Override public String icon() { return "images/items/atk_twin.png"; }
    @Override public boolean doublesPiece() { return running(); }
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
