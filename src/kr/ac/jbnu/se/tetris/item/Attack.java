package kr.ac.jbnu.se.tetris.item;

/**
 * 2인 대전에서 쓰는 아이템.
 *
 * 대부분은 상대 보드에 기믹을 하나 걸고, self 가 true 인 것은 내 보드에 건다.
 * 어떤 기믹이 걸리는지는 Gimmick.effectFor() 에 적혀 있다.
 */
public enum Attack {

	// 방해 아이템 (상대에게)
	BOMB("폭탄", "atk_bomb", false),
	TINY("1X1 폭탄", "atk_tiny", false),
	INK("먹물", "atk_ink", false),
	SPEED("가속", "atk_speed", false),
	REVERSE("조작 반전", "atk_reverse", false),
	LOCK("블록 잠그기", "atk_block", false),
	TWIN("블록 2개", "atk_twin", false),
	SWAP("블록 교환", "atk_swap", false),

	// 내게 거는 아이템
	SCORE_X2("점수 2배", "atk_double", true),
	REST("한숨 돌리기", "atk_nopiece", true),

	// 기믹 아이템 (상대에게)
	RANDOM_KEYS("랜덤 방향키", "gim_randomkey", false),
	SPIN("회전 고정", "gim_rotfix", false),
	FADE("소멸 블록", "gim_fade", false),
	GRAVITY("중력 뒤집기", "gim_gravity", false),
	FLOOR("바닥 상승", "gim_floor", false),
	INVISIBLE("투명 블록", "gim_invisible", false),
	NO_ROTATE("회전 불가", "gim_norotate", false);

	public final String label;
	public final String icon;

	/** true 면 상대가 아니라 내 보드에 건다. */
	public final boolean self;

	Attack(String label, String icon, boolean self) {
		this.label = label;
		this.icon = "images/items/" + icon + ".png";
		this.self = self;
	}

	/** 무작위로 하나 고른다. (대전 중 아이템을 줄 때 쓴다) */
	public static Attack random(java.util.Random rand) {
		return values()[rand.nextInt(values().length)];
	}
}
