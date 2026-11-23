package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.Resources;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.InputStream;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/** 화면 전체에서 공유하는 색과 폰트. Figma "리팩" 화면들의 색을 그대로 옮겼다. (CSS 대신) */
final class Ui {

	/** 창 바탕 (그림이 덮지 못한 틈) */
	static final Color BG = new Color(0xBFE6F7);

	/** 상자 테두리 갈색 */
	static final Color BROWN = new Color(0xA8693F);
	/** 진한 갈색 (토글, 버튼 테두리) */
	static final Color DARK_BROWN = new Color(0x5A3424);
	/** 기본 글자 색 */
	static final Color TEXT = new Color(0x4B2E1F);
	/** 흐린 글자 색 (표 머리, 설명) */
	static final Color SUB_TEXT = new Color(0x8A6A55);
	/** 메뉴 제목 / 타원 버튼 글자 */
	static final Color TITLE_TEXT = new Color(0x3D3D3D);
	/** 점선 구분선 */
	static final Color DASH = new Color(0xD9C2A8);

	/** 크림색 바탕 (코인 알약, 제목 알약) */
	static final Color CREAM = new Color(0xFFF9EC);
	/** 반투명 크림 (Hold / Next / Score 상자 안) */
	static final Color PANEL_FILL = new Color(255, 249, 236, 204);
	/** 보드 안쪽 하늘색 */
	static final Color BOARD_FILL = new Color(0xE4F8FF);

	/** 빨강 (1P, 시작하기, 고른 줄 테두리) */
	static final Color RED = new Color(0xC9452F);
	/** 파랑 (2P) */
	static final Color BLUE = new Color(0x2E6BC4);
	/** 노랑 (음량 눈금, 내 기록 줄) */
	static final Color YELLOW = new Color(0xFFCF5A);
	/** 연한 노랑 (안내 줄, 고른 스킨) */
	static final Color LIGHT_YELLOW = new Color(0xFCE9BD);
	/** 선택 화살표 빨강 */
	static final Color ARROW_RED = new Color(0xEA0000);

	/** 어닝(가게 차양) 빨강과 그 아래 그림자 */
	static final Color AWNING = new Color(0xF97860);
	static final Color AWNING_SHADOW = new Color(0xDDC590);

	/** 일시정지 / 결과 카드 */
	static final Color DARK_CARD = new Color(0x3A3934);
	static final Color DARK_CARD_BORDER = new Color(0xA8641A);

	/** 대전 점수판 칠판 초록 */
	static final Color CHALK = new Color(0x004701);

	/** 그림자 */
	static final Color SHADOW = new Color(0, 0, 0, 64);

	/** 프로젝트에 같이 넣은 글꼴. 동글동글한 주아체 (OFL, font/OFL.txt). */
	private static final String[] FONT_FILES = { "font/Jua-Regular.ttf" };

	/** 파일이 없을 때 찾아볼, 이 컴퓨터에 설치된 폰트 이름. */
	private static final String SYSTEM_FONT_NAME = "배달의민족 주아";

	private static final Font BASE = loadFont();

	private Ui() {
	}

	static Font font(float size) {
		return BASE.deriveFont(size);
	}

	/**
	 * 폰트를 구하는 순서:
	 *   1) 프로젝트 안의 폰트 파일       - 팀원 모두에게 똑같이 보인다
	 *   2) 설치된 SYSTEM_FONT_NAME     - 내 화면에서만 보인다
	 *   3) 기본 폰트                   - 최후의 수단
	 */
	private static Font loadFont() {
		for (String file : FONT_FILES) {
			try (InputStream in = Resources.open(file)) {
				if (in != null)
					return Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (Exception ignored) {
				// 다음 후보로 넘어간다
			}
		}
		Font installed = new Font(SYSTEM_FONT_NAME, Font.PLAIN, 12);
		if (installed.getFamily().equals(SYSTEM_FONT_NAME))
			return installed;

		System.err.println("폰트를 찾지 못했습니다. 기본 폰트로 실행합니다.");
		return new Font(Font.SANS_SERIF, Font.BOLD, 12);
	}

	/**
	 * 키 하나를 동작에 연결한다. WHEN_IN_FOCUSED_WINDOW 라서 포커스를 신경 쓸 필요가 없고,
	 * CardLayout 으로 가려진 화면의 바인딩은 동작하지 않는다.
	 */
	static void bind(JComponent c, int keyCode, String name, Runnable action) {
		bind(c, KeyStroke.getKeyStroke(keyCode, 0), name, action);
	}

	/** 수정키(Shift 등)가 섞인 키처럼 KeyStroke 를 직접 만들어 연결할 때. */
	static void bind(JComponent c, KeyStroke stroke, String name, Runnable action) {
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
		c.getActionMap().put(name, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}
}
