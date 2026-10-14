package kr.ac.jbnu.se.tetris.item;

import java.awt.Color;

/**
 * 블록 스킨. 색 7개가 한 벌이고, Board 가 tile.png 를 이 색으로 물들여 쓴다.
 *
 * 색 수는 GameBoard.COLOR_COUNT 와 같아야 한다.
 */
public enum Skin {

	CLASSIC("클래식", 0,
			0xCC6666, 0x66CC66, 0x6666CC, 0xCCCC66, 0xCC66CC, 0x66CCCC, 0xDAAA00),

	PINK("핑크 테마", 300,
			0xFF8FB1, 0xFFB3C7, 0xE75480, 0xFF6FA5, 0xC94F7C, 0xFFC2D4, 0xD96BA0),

	BLUE("파랑 테마", 300,
			0x6FA8FF, 0x4F7BC9, 0x8FD3FF, 0x3F5FB0, 0x5FC9D6, 0x9FB8FF, 0x2E86C1);

	public final String label;
	public final int price;
	public final Color[] colors;

	Skin(String label, int price, int... rgb) {
		this.label = label;
		this.price = price;
		this.colors = new Color[rgb.length];
		for (int i = 0; i < rgb.length; i++)
			this.colors[i] = new Color(rgb[i]);
	}
}
