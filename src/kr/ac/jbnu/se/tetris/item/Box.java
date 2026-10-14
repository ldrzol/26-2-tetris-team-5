package kr.ac.jbnu.se.tetris.item;

import java.util.Random;

/**
 * 랜덤 아이템 박스. 등급이 높을수록 좋은 아이템이 나올 확률이 높다.
 *
 * weights 는 Item 순서(폭탄/감속/피버)대로 적은 뽑기 가중치다.
 */
public enum Box {

	GOLD("금 상자", 300, "images/box_gold.png", 7, 3),
	SILVER("은 상자", 180, "images/box_silver.png", 5, 5),
	BRONZE("동 상자", 80, "images/box_bronze.png", 3, 7);

	private static final Random RANDOM = new Random();

	public final String label;
	public final int price;
	public final String icon;
	private final int[] weights;

	Box(String label, int price, String icon, int... weights) {
		this.label = label;
		this.price = price;
		this.icon = icon;
		this.weights = weights;
	}

	/** 상자를 열어 아이템 하나를 뽑는다. */
	public Item open() {
		int total = 0;
		for (int w : weights)
			total += w;

		int pick = RANDOM.nextInt(total);
		for (int i = 0; i < weights.length; i++) {
			pick -= weights[i];
			if (pick < 0)
				return Item.values()[i];
		}
		return Item.values()[0];   // 여기 올 일은 없다
	}

	/** 보상으로 상자를 하나 줄 때. 금/은/동 중 하나를 고른다. (예: Wallet.add(Box.random().open())) */
	static Box random() {
		return values()[RANDOM.nextInt(values().length)];
	}
}
