package kr.ac.jbnu.se.tetris.item;

/** 게임 중 쓰는 아이템. 실제 효과는 Board.useItem 에 있다. */
public enum Item {

	BOMB("폭탄", "맨 아래 줄 삭제", "images/items/atk_bomb.png"),
	SLOW("감속", "10초간 천천히", null);   // 감속 아이콘은 아직 없다. 생기면 경로만 적으면 된다

	public final String label, desc, icon;

	Item(String label, String desc, String icon) {
		this.label = label;
		this.desc = desc;
		this.icon = icon;
	}
}
