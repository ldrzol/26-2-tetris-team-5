package kr.ac.jbnu.se.tetris.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 랜덤 아이템 박스. 등급이 높을수록 한 번에 많이 나온다.
 *
 * 나오는 아이템은 Attack 전부에서 고르게 뽑는다.
 */
public enum Box {

	GOLD("금 상자", 300, "images/box_gold.png", 3),
	SILVER("은 상자", 180, "images/box_silver.png", 2),
	BRONZE("동 상자", 80, "images/box_bronze.png", 1);

	private static final Random RANDOM = new Random();

	public final String label;
	public final int price;
	public final String icon;

	/** 한 번 열 때 나오는 아이템 수. */
	public final int picks;

	Box(String label, int price, String icon, int picks) {
		this.label = label;
		this.price = price;
		this.icon = icon;
		this.picks = picks;
	}

	/** 상자를 열어 아이템을 뽑는다. */
	public List<Attack> open() {
		List<Attack> got = new ArrayList<>();
		for (int i = 0; i < picks; i++)
			got.add(Attack.random(RANDOM));
		return got;
	}

	/** 보상으로 상자를 하나 줄 때. 금/은/동 중 하나를 고른다. */
	static Box random() {
		return values()[RANDOM.nextInt(values().length)];
	}
}
