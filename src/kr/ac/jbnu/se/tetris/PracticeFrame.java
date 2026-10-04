package kr.ac.jbnu.se.tetris;

/**
 * AI 대전 화면. 보이는 모양은 2인 대전(Figma "Desktop - 9")과 같고,
 * 오른쪽 보드를 사람 대신 AI 가 조종한다.
 */
class PracticeFrame extends VersusPanel {

	PracticeFrame(MainFrame frame) {
		super(frame, MainFrame.PRACTICE, true);
	}
}
