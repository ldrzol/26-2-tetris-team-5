package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.InputStream;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/** 화면 전체에서 공유하는 색과 폰트. (CSS 대신) */
final class Ui {

	static final Color BG = new Color(0x0B0B0B);
	static final Color CREAM = new Color(0xF1E7D3);

	/** 프로젝트에 같이 넣을 폰트 파일. 이 중에 있는 걸 먼저 쓴다. */
	private static final String[] FONT_FILES = { "font/NeoDunggeunmoPro-Regular.ttf" };

	/** 파일이 없을 때 찾아볼, 이 컴퓨터에 설치된 폰트 이름. (Ui.main 으로 정확한 이름 확인) */
	private static final String SYSTEM_FONT_NAME = "산돌구름체";

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
	 *   3) 기본 고정폭 폰트             - 최후의 수단
	 */
	private static Font loadFont() {
		for (String file : FONT_FILES) {
			try (InputStream in = Ui.class.getResourceAsStream(file)) {
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
		return new Font(Font.MONOSPACED, Font.BOLD, 12);
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

	/** 이 컴퓨터에 깔린 폰트 이름을 전부 출력한다. SYSTEM_FONT_NAME 에 적을 값을 여기서 찾는다. */
	public static void main(String[] args) {
		for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
			System.out.println(name);
	}
}
