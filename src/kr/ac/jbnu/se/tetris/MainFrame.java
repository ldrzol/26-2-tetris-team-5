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
	static final String SETTINGS = "settings"; // Figma 환경 설정 화면
	static final String RANKING = "ranking";   // Figma 랭킹 화면
	static final String SHOP = "shop";         // 상점 화면

	private final CardLayout cards = new CardLayout();
	private final JPanel root = new JPanel(cards);
	private JPanel current;
	private JPanel overlay;

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
		show(createScreen(name), name);
	}

	private void show(JPanel panel, String name) {
		clearOverlay();
		if (current != null)
			root.remove(current);
		current = panel;
		root.add(current, name);
		cards.show(root, name);
		root.revalidate();
		root.repaint();

		// 게임이 아닌 화면의 톱니는 환경 설정으로 간다. 뒤로 가면 이 화면으로 돌아온다.
		// (게임 화면은 톱니에 일시정지를 걸어 두므로 건드리지 않는다)
		if (panel instanceof MenuScreen || panel instanceof RankingScreen || panel instanceof ShopScreen)
			((DesignPanel) panel).onGear(() -> show(new SettingsScreen(() -> showScreen(name)), SETTINGS));
	}

	private JPanel createScreen(String name) {
		switch (name) {
			case MULTI:
				return new MenuScreen(new String[] { "멀티 모드" }, new float[] { 128f }, 252,
						new String[] { "2인 대전", "AI 대전" },
						i -> showScreen(i == 0 ? LOBBY : PRACTICE));
			case LOBBY:
				return new LobbyFrame(this);
			case RANKING:
				return new RankingScreen(() -> showScreen(SINGLE), () -> showScreen(MENU));
			case SHOP:
				return new ShopScreen(() -> showScreen(MENU));
			case SINGLE:
				return new SinglePanel(this);
			case VERSUS:
				return new VersusPanel(this, VERSUS, false);
			case PRACTICE:
				return new VersusPanel(this, PRACTICE, true);   // 오른쪽 보드를 AI 가 조종한다
			default:
				return new MenuScreen(new String[] { "TETRIS" }, new float[] { 250f }, 274,
						new String[] { "1인 모드", "멀티 모드", "상점" },
						i -> showScreen(i == 0 ? RANKING : i == 1 ? MULTI : SHOP));
		}
	}

	/** 글자 한 줄을 묻는 덮개. 취소하면 onDone 에 null 이 간다. */
	void showInput(String title, String initial, java.util.function.Consumer<String> onDone) {
		showOverlay(new InputOverlay(title, initial, value -> {
			clearOverlay();
			onDone.accept(value);
		}));
	}

	/** 게임 위에 반투명 GAME OVER 를 덮는다. kind 는 '다시 하기' 로 돌아갈 화면. */
	void showGameOver(int score, String kind) {
		showGameOver(score, 0, kind);
	}

	/** coins 는 이번 판으로 받은 코인. 0이면 줄을 그리지 않는다. */
	void showGameOver(int score, int coins, String kind) {
		showOverlay(new GameOverOverlay(score, coins, () -> showScreen(kind), () -> showScreen(MENU)));
	}

	/**
	 * 게임 중 톱니를 눌렀을 때. Figma "리플레이 화면".
	 *
	 * 게임 화면은 그대로 두고 덮개만 바꿔 끼우므로, 환경 설정에 다녀와도 판이 남아 있다.
	 */
	void showPause(String kind, Runnable onResume) {
		showOverlay(new PauseOverlay(
				() -> {
					clearOverlay();
					onResume.run();
				},
				() -> showScreen(kind),
				() -> showScreen(MENU),
				() -> showOverlay(new SettingsScreen(() -> showPause(kind, onResume)))));
	}

	/** 지금 화면 위에 덮개를 올린다. 먼저 올려 둔 덮개는 치운다. */
	private void showOverlay(JPanel panel) {
		clearOverlay();
		overlay = panel;
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
