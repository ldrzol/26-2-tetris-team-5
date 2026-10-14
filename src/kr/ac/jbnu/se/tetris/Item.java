package kr.ac.jbnu.se.tetris;

/** 게임 중 쓰는 아이템. 실제 효과는 Board.useItem 에 있다. */
enum Item {

	BOMB("폭탄", "맨 아래 줄 삭제"),
	SLOW("감속", "10초간 천천히");

	final String label, desc;

	Item(String label, String desc) {
		this.label = label;
		this.desc = desc;
	}
}
