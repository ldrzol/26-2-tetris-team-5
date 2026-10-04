package kr.ac.jbnu.se.tetris;

import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 프로그램의 유일한 창. 모든 화면을 CardLayout 으로 갈아 끼운다. */
public class MainFrame extends JFrame {

	static final String MENU = "menu";         // Figma Desktop - 5
	static final String MULTI = "multi";       // Figma Desktop - 13
	static final String LOBBY = "lobby";       // Figma Desktop - 12
	static final String SINGLE = "single";     // Figma Desktop - 6
	static final String VERSUS = "versus";     // Figma Desktop - 9
	static final String PRACTICE = "practice"; // Figma Desktop - 9 (상대가 AI)

	private final CardLayout cards = new CardLayout();
	private final JPanel root = new JPanel(cards);
	private JPanel current;
	private GameOverOverlay overlay;

	public MainFrame() {
		root.setBackground(Ui.BG);
		add(root);
		showScreen(MENU);

		setTitle("TETRIS");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(DesignPanel.DESIGN_W, DesignPanel.DESIGN_H);
		setLocationRelativeTo(null);
	}

	/** 화면을 새로 만들어서 띄운다. 매번 새로 만들므로 다시 들어가면 늘 처음 상태다. */
	void showScreen(String name) {
		clearOverlay();
		if (current != null)
			root.remove(current);
		current = createScreen(name);
		root.add(current, name);
		cards.show(root, name);
		root.revalidate();
		root.repaint();
	}

	private JPanel createScreen(String name) {
		switch (name) {
			case MULTI:
				return new MenuScreen(new String[] { "멀티 모드" }, new float[] { 128f }, 252,
						new String[] { "2인 대전", "AI 대전" },
						i -> showScreen(i == 0 ? LOBBY : PRACTICE));
			case LOBBY:
				return new LobbyFrame(this);
			case SINGLE:
				return new SinglePanel(this);
			case VERSUS:
				return new VersusPanel(this, VERSUS, false);
			case PRACTICE:
				return new VersusPanel(this, PRACTICE, true);   // 오른쪽 보드를 AI 가 조종한다
			default:
				return new MenuScreen(new String[] { "TETRIS" }, new float[] { 250f }, 274,
						new String[] { "1인 모드", "멀티 모드" },
						i -> showScreen(i == 0 ? SINGLE : MULTI));
		}
	}

	/** 게임 위에 반투명 GAME OVER 를 덮는다. kind 는 '다시 하기' 로 돌아갈 화면. */
	void showGameOver(int score, String kind) {
		clearOverlay();
		overlay = new GameOverOverlay(score, () -> showScreen(kind), () -> showScreen(MENU));
		overlay.setBounds(0, 0, getContentPane().getWidth(), getContentPane().getHeight());
		getLayeredPane().add(overlay, JLayeredPane.PALETTE_LAYER);
		getLayeredPane().revalidate();
		getLayeredPane().repaint();
	}

	private void clearOverlay() {
		if (overlay != null) {
			getLayeredPane().remove(overlay);
			getLayeredPane().repaint();
			overlay = null;
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
	}
}
