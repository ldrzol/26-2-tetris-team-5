package kr.ac.jbnu.se.tetris.save;

import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.item.Box;
import kr.ac.jbnu.se.tetris.item.Skin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * 코인 지갑 + 보유 스킨 + 아이템 가방.
 *
 * 상점에서 산 것이 다음 실행에도 남아야 하므로 Preferences 에 적어 둔다.
 * (파일 경로를 정할 필요가 없어서 가장 짧다)
 */
public final class Wallet {

	/** 이 점수를 넘기면 클리어로 치고 보너스 코인을 준다. */
	static final int CLEAR_SCORE = 3000;
	static final int CLEAR_BONUS = 500;

	/** 점수 이만큼마다 코인 1개. */
	static final int SCORE_PER_COIN = 100;

	private static final Preferences PREFS = Preferences.userNodeForPackage(Wallet.class);

	private static int coins = PREFS.getInt("coins", 0);
	private static Skin skin = skinOf(PREFS.get("skin", ""));
	private static final Set<Skin> owned = loadOwned();
	private static final int[] bag = loadBag();

	private Wallet() {
	}

	// 조회
	public static int coins() {
		return coins;
	}

	/** 화면에 적을 코인 수. (1,234 처럼 천 단위로 끊는다) */
	public static String coinText() {
		return String.format("%,d", coins);
	}

	public static Skin skin() {
		return skin;
	}

	public static boolean owns(Skin s) {
		return owned.contains(s);
	}

	public static int count(Attack item) {
		return bag[item.ordinal()];
	}

	// 상점
	/** 스킨 구매. 코인이 모자라면 false. 산 스킨은 바로 장착한다. */
	public static boolean buy(Skin s) {
		if (owned.contains(s) || coins < s.price)
			return false;
		coins -= s.price;
		owned.add(s);
		skin = s;
		save();
		return true;
	}

	/** 이미 가진 스킨으로 갈아 끼운다. */
	public static boolean equip(Skin s) {
		if (!owned.contains(s))
			return false;
		skin = s;
		save();
		return true;
	}

	/**
	 * 상자를 사서 바로 연다. 나온 아이템을 가방에 넣고 돌려준다. 코인이 모자라면 빈 목록.
	 *
	 * 안 연 상자를 들고 다니는 상태는 두지 않는다. 상자를 모았다가 여는 연출이 필요하면
	 * 상자 개수도 가방에 넣고 ShopScreen 에 '열기' 줄을 늘려야 한다.
	 */
	public static List<Attack> buy(Box box) {
		if (coins < box.price)
			return java.util.Collections.emptyList();
		coins -= box.price;
		List<Attack> got = box.open();
		for (Attack item : got)
			add(item);
		return got;
	}

	/** 피버 타임 보상이나 대전 중 점수 보상처럼 공짜로 받는 경우. */
	public static Attack add(Attack item) {
		bag[item.ordinal()]++;
		save();
		return item;
	}

	/** 아이템 하나를 쓴다. 없으면 false. */
	public static boolean use(Attack item) {
		if (bag[item.ordinal()] <= 0)
			return false;
		bag[item.ordinal()]--;
		save();
		return true;
	}
	/**
	 * 땅따먹기 참가비 정산. 결과 문구를 돌려준다.
	 * winner: 0=P1, 1=P2, -1=무승부. me: 나는 P1(0) 또는 P2(1).
	 */
	public static String settle(int winner, int me, int fee) {
		if (winner == -1) {
			addCoins(fee);
			return "무승부 - 참가비를 돌려받았습니다";
		}
		if (winner == me) {
			addCoins(fee * 2);
			return "승리! 참가비 " + fee + " 코인을 받았습니다";
		}
		return "패배 - 참가비 " + fee + " 코인을 잃었습니다";
	}

	/** 코인을 더하거나 뺀다(음수). 참가비 정산에 쓴다. 0 아래로는 내려가지 않는다. */
	public static void addCoins(int amount) {
		coins = Math.max(0, coins + amount);
		save();
	}

	// 보상
	/** 레벨 하나를 깰 때마다 주는 코인. */
	static final int COIN_PER_LEVEL = 100;

	/** 레벨 클리어 보상. 레벨이 높을수록 많이 준다. */
	public static int awardLevel(int level) {
		int got = COIN_PER_LEVEL * level;
		coins += got;
		save();
		return got;
	}

	/** 주간 랭킹 보상. 랭킹 화면의 표와 같은 값이다. */
	public static int weeklyReward(int rank) {
		if (rank == 1)
			return 1000;
		if (rank >= 2 && rank <= 3)
			return 500;
		if (rank >= 4 && rank <= Ranking.MAX)
			return 100;
		return 30;      // 참여
	}

	/**
	 * 지난주 보상을 지급한다. 한 주에 한 번만 받을 수 있고, 이미 받았거나
	 * 기록이 없으면 0 을 돌려준다.
	 */
	public static int claimWeekly(String week, int rank) {
		if (rank <= 0 || week.equals(PREFS.get("paidWeek", "")))
			return 0;
		int got = weeklyReward(rank);
		coins += got;
		PREFS.put("paidWeek", week);
		save();
		return got;
	}

	/** 점수로 받는 코인. (클리어하면 보너스가 붙는다) */
	static int reward(int score) {
		return score / SCORE_PER_COIN + (cleared(score) ? CLEAR_BONUS : 0);
	}

	public static boolean cleared(int score) {
		return score >= CLEAR_SCORE;
	}

	/** 판이 끝났을 때 코인을 넣어 주고, 넣은 양을 돌려준다. */
	public static int award(int score) {
		int got = reward(score);
		coins += got;
		save();
		return got;
	}

	// 저장
	private static void save() {
		PREFS.putInt("coins", coins);
		PREFS.put("skin", skin.name());
		StringBuilder names = new StringBuilder();
		for (Skin s : owned)
			names.append(names.length() == 0 ? "" : ",").append(s.name());
		PREFS.put("owned", names.toString());

		StringBuilder counts = new StringBuilder();
		for (int i = 0; i < bag.length; i++)
			counts.append(i == 0 ? "" : ",").append(bag[i]);
		PREFS.put("items", counts.toString());
	}

	private static Set<Skin> loadOwned() {
		Set<Skin> set = EnumSet.of(Skin.CLASSIC);   // 기본 스킨은 늘 가지고 있다
		for (String name : PREFS.get("owned", "").split(","))
			for (Skin s : Skin.values())
				if (s.name().equals(name))
					set.add(s);
		if (!set.contains(skin))
			skin = Skin.CLASSIC;
		return set;
	}

	private static int[] loadBag() {
		int[] out = new int[Attack.values().length];      // 아이템 종류는 대전 아이템과 같다
		String[] parts = PREFS.get("items", "").split(",");
		for (int i = 0; i < out.length && i < parts.length; i++)
			out[i] = number(parts[i]);
		return out;
	}

	private static int number(String s) {
		try {
			return Math.max(0, Integer.parseInt(s.trim()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Skin skinOf(String name) {
		for (Skin s : Skin.values())
			if (s.name().equals(name))
				return s;
		return Skin.CLASSIC;
	}

	/** 자가 점검. java -ea 로 실행한다. (저장소는 건드리지 않는 계산만 본다) */
	public static void main(String[] args) {
		assert reward(0) == 0 : "0점이면 코인도 0";
		assert reward(CLEAR_SCORE - SCORE_PER_COIN) == (CLEAR_SCORE - SCORE_PER_COIN) / SCORE_PER_COIN
				: "클리어 전에는 보너스가 붙으면 안 된다";
		assert reward(CLEAR_SCORE) == CLEAR_SCORE / SCORE_PER_COIN + CLEAR_BONUS : "클리어 보너스가 빠졌다";

		int[] seen = new int[Attack.values().length];
		for (int i = 0; i < 3000; i++)
			for (Box box : Box.values()) {
				List<Attack> got = box.open();
				assert got.size() == box.picks : box.label + " 에서 나온 개수가 다르다: " + got.size();
				for (Attack a : got)
					seen[a.ordinal()]++;
			}
		for (int n : seen)
			assert n > 0 : "어느 상자에서도 안 나오는 아이템이 있다";

		assert weeklyReward(1) == 1000 && weeklyReward(2) == 500 && weeklyReward(3) == 500
				: "1~3위 보상이 표와 다르다";
		assert weeklyReward(4) == 100 && weeklyReward(Ranking.MAX) == 100 : "4~10위 보상이 표와 다르다";
		assert weeklyReward(Ranking.MAX + 1) == 30 && weeklyReward(0) == 30 : "참여 보상이 30이 아니다";

		System.out.println("ok");
	}
}
