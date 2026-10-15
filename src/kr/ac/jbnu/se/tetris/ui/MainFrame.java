package kr.ac.jbnu.se.tetris.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.ai.AIplayer;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

/** 프로그램의 유일한 창. 모든 화면을 CardLayout 으로 갈아 끼운다. */
public class MainFrame extends JFrame {

	static final String MENU = "menu";         // Figma Desktop - 5
	static final String MULTI = "multi";       // Figma Desktop - 13
	static final String LOBBY = "lobby";       // Figma Desktop - 12
	static final String SINGLE = "single";     // Figma Desktop - 6
	static final String VERSUS = "versus";     // Figma Desktop - 9
	static final String LOCAL = "local";       // Figma Desktop - 9 (한 PC 에서 둘이)
	static final String PRACTICE = "practice"; // Figma Desktop - 9 (상대가 AI)
	static final String SETTINGS = "settings"; // Figma 환경 설정 화면
	static final String RANKING = "ranking";   // Figma 랭킹 화면
	static final String SHOP = "shop";         // 상점 화면
	static final String HIDDEN = "hidden";     // 숨은 레벨 (다방향 테트리스)

	private final CardLayout cards = new CardLayout();
	private final JPanel root = new JPanel(cards);
	private JPanel current;
	private JPanel overlay;
	static final String AI_LEVEL = "ailevel";
	private AIplayer.Level aiLevel = AIplayer.Level.NORMAL;

	AIplayer.Level aiLevel() {
		return aiLevel;
	}
	public MainFrame() {
		root.setBackground(Ui.BG);
		add(root);
		showScreen(MENU);

		setTitle("TETRIS");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(windowSize());
		setLocationRelativeTo(null);
	}

	/** 디자인(2000x1422)을 모니터 작업 영역의 80% 안에 맞춰 줄인 창 크기. */
	private static Dimension windowSize() {
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		double scale = Math.min(screen.width / (double) DesignPanel.DESIGN_W,
				screen.height / (double) DesignPanel.DESIGN_H) * 0.8;
		return new Dimension((int) (DesignPanel.DESIGN_W * scale), (int) (DesignPanel.DESIGN_H * scale));
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
						new String[] { "2인 대전", "한 화면 2인", "AI 대전" },
						i -> showScreen(i == 0 ? LOBBY : i == 1 ? LOCAL : AI_LEVEL));
			case AI_LEVEL:
				return new MenuScreen(new String[] { "AI 대전" }, new float[] { 128f }, 252,
						new String[] { "쉬움", "보통", "어려움" },
						i -> {
							aiLevel = AIplayer.Level.values()[i];
							showScreen(PRACTICE);
						});
			case LOBBY:
				return new LobbyFrame(this);
			case RANKING:
				return new RankingScreen(() -> showScreen(SINGLE), () -> showScreen(MENU));
			case SHOP:
				return new ShopScreen(() -> showScreen(MENU));
			case SINGLE:
				return new SinglePanel(this);
			case HIDDEN:
				return new HiddenPanel(this);
			case PRACTICE:
				return new VersusPanel(this, PRACTICE, true);   // 오른쪽 보드를 AI 가 조종한다
			case LOCAL:
				return new VersusPanel(this, LOCAL, false);     // 두 보드를 한 PC 에서 둘이 잡는다
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

	/** 둘 중 하나를 고르는 덮개. 고른 글자가 그대로 간다. Esc 로 닫으면 null 이 간다. */
	void showChoice(String title, String first, String second,
			java.util.function.Consumer<String> onDone) {
		showOverlay(new InputOverlay(title, new String[] { first, second }, value -> {
			clearOverlay();
			onDone.accept(value);
		}));
	}

	/** [피버] 블록 7종 중 하나를 고르는 덮개. 반드시 하나를 고른다. */
	void showPick(String title, java.util.function.Consumer<kr.ac.jbnu.se.tetris.game.Tetrominoes> onPick) {
		showOverlay(new PickOverlay(title, PickOverlay.pieces(), shape -> {
			clearOverlay();
			onPick.accept(shape);
		}));
	}

	/** 대전 시작에서 가방에 있는 아이템 하나를 고르는 덮개. */
	void showPickItem(String title, kr.ac.jbnu.se.tetris.item.Attack[] choices,
			java.util.function.Consumer<kr.ac.jbnu.se.tetris.item.Attack> onPick) {
		showOverlay(new PickOverlay(title, choices, item -> {
			clearOverlay();
			onPick.accept(item);
		}));
	}

	/** 레벨을 깼을 때 덮는 '레벨 클리어' 화면. '다음 레벨' 을 고르면 onNext 가 돈다. */
	void showLevelClear(int level, int score, int coins, Runnable onNext) {
		showOverlay(new LevelClearOverlay(level, score, coins,
				() -> {
					clearOverlay();
					onNext.run();
				},
				() -> showScreen(MENU)));
	}

	/** 게임 위에 반투명 GAME OVER 를 덮는다. kind 는 '다시 하기' 로 돌아갈 화면. */
	void showGameOver(int score, String kind) {
		showGameOver(score, 0, kind);
	}

	/** coins 는 이번 판으로 받은 코인. 0이면 줄을 그리지 않는다. */
	void showGameOver(int score, int coins, String kind) {
		showOverlay(new GameOverOverlay(score, coins, () -> showScreen(kind), () -> showScreen(MENU)));
	}

	/** 대전 결과. title 은 YOU WIN / YOU LOSE / DRAW. coins 는 정산으로 받은 코인. */
	void showResult(String title, int score, int coins, String kind) {
		showOverlay(new GameOverOverlay(title, score, coins, () -> showScreen(kind), () -> showScreen(MENU)));
	}

	/** 네트워크 2인 대전을 띄운다. 끝나면 참가비를 정산한다. */
	void showVersus(PeerConnection connection, int fee) {
		show(new VersusPanel(this, connection, fee), VERSUS);
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

	/**
	 * 지금 화면 위에 덮개를 올린다. 먼저 올려 둔 덮개는 치운다.
	 *
	 * 뒤 화면의 단축키는 창 전체에 걸려 있어서(WHEN_IN_FOCUSED_WINDOW) 덮개가 떠 있어도
	 * 같이 반응한다. 덮개의 Enter 가 뒤 메뉴의 Enter 와 겹치므로 뒤 화면을 꺼 둔다.
	 */
	private void showOverlay(JPanel panel) {
		clearOverlay();
		if (current != null)
			current.setEnabled(false);
		overlay = panel;
		overlay.setBounds(0, 0, getContentPane().getWidth(), getContentPane().getHeight());
		getLayeredPane().add(overlay, JLayeredPane.PALETTE_LAYER);
		getLayeredPane().revalidate();
		getLayeredPane().repaint();
	}

	private void clearOverlay() {
		if (current != null)
			current.setEnabled(true);
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
