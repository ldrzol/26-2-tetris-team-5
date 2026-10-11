package kr.ac.jbnu.se.tetris;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/** 1인 모드 화면. Figma "Desktop - 6". */
class SinglePanel extends DesignPanel {

	private static final int TIME_LIMIT = 300;   // 5분

	// ── Figma "Desktop - 6" 수치 (디자인 기준 픽셀) ─────────────────────────
	private static final Rectangle BOARD = new Rectangle(700, 144, 600, 1200);

	private static final Rectangle HOLD_TAB = new Rectangle(311, 147, 186, 62);
	private static final Rectangle HOLD_BOX = new Rectangle(311, 209, 322, 181);
	private static final Rectangle NEXT_TAB = new Rectangle(1367, 147, 186, 62);
	private static final Rectangle NEXT_BOX = new Rectangle(1367, 209, 322, 181);
	private static final Rectangle SCORE_TAB = new Rectangle(1367, 591, 190, 62);
	private static final Rectangle SCORE_BOX = new Rectangle(1367, 653, 322, 100);
	private static final Rectangle TIME_TAB = new Rectangle(1367, 808, 190, 62);
	private static final Rectangle TIME_BOX = new Rectangle(1367, 870, 322, 100);

	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;

	/**
	 * 보드 한 칸의 크기(디자인 기준 px). 디자인 원본은 60 으로, 그때 보드가 600x1200 자리를
	 * 정확히 채운다. 줄이면 보드 전체가 같은 자리 가운데에서 작아진다.
	 */
	private static final int CELL = 60;

	private final MainFrame frame;
	private final Board board;
	private String scoreText = "000000";
	private String timeText = "05:00";
	private int remain = TIME_LIMIT;

	// ponytail: 점수·시계를 1초마다 폴링한다. 보드가 이벤트를 안 쏘니 이게 제일 짧다.
	private final Timer clock = new Timer(1000, e -> tick());

	SinglePanel(MainFrame frame) {
		this.frame = frame;

		board = new Board(new JLabel());   // Board 가 상태바를 요구한다. 화면엔 안 붙인다.
		board.setBackground(Ui.BG);
		board.onRepaint(this::repaint);    // Hold/Next 는 패널이 그리므로 같이 갱신해야 한다
		add(board);

		Ui.bind(this, KeyEvent.VK_LEFT, "left", board::moveLeft);
		Ui.bind(this, KeyEvent.VK_RIGHT, "right", board::moveRight);
		Ui.bind(this, KeyEvent.VK_UP, "rotate", board::rotateRight);
		Ui.bind(this, KeyEvent.VK_DOWN, "soft", board::softDrop);
		Ui.bind(this, KeyEvent.VK_SPACE, "hard", board::hardDrop);
		Ui.bind(this, KeyEvent.VK_C, "hold", board::hold);
		Ui.bind(this, KeyEvent.VK_SHIFT, "hold2", board::hold);
		Ui.bind(this, KeyEvent.VK_ESCAPE, "pause", this::pause);
		onGear(this::pause);

		board.start();
		clock.start();
	}

	/** 톱니(또는 Esc)를 누르면 판을 멈추고 Figma "리플레이 화면" 을 덮는다. */
	private void pause() {
		if (!board.isRunning())
			return;
		clock.stop();
		board.setPaused(true);
		frame.showPause(MainFrame.SINGLE, this::resume);
	}

	private void resume() {
		board.setPaused(false);
		clock.start();
	}

	/**
	 * 이름을 물어보고 랭킹 파일에 한 줄 남긴다. 취소하면 남기지 않는다.
	 *
	 * ponytail: 전용 입력 화면 대신 JOptionPane 을 쓴다. 판마다 한 번 뜨는 창이다.
	 */
	private void saveRanking() {
		String name = JOptionPane.showInputDialog(this, "랭킹에 남길 이름", Settings.playerName);
		if (name == null)
			return;
		Settings.playerName = Ranking.clean(name);
		Ranking.add(Settings.playerName, board.score());
	}

	@Override
	void layoutDesign() {
		board.setBounds(boardBounds(BOARD, CELL));
	}

	@Override
	void paintDesign(Graphics2D g) {
		boardFrame(g, boardBounds(BOARD, CELL));

		outline(g, HOLD_BOX);
		outline(g, NEXT_BOX);
		outline(g, SCORE_BOX);
		outline(g, TIME_BOX);

		tab(g, HOLD_TAB, "Hold", TAB_SIZE);
		tab(g, NEXT_TAB, "Next", TAB_SIZE);
		tab(g, SCORE_TAB, "Score", TAB_SIZE);
		tab(g, TIME_TAB, "Time", TAB_SIZE);

		GameBoard game = board.getGame();
		preview(g, HOLD_BOX, game.holdType(), game.getHoldColor());
		preview(g, NEXT_BOX, game.nextType(), game.getNextColor());

		valueRight(g, SCORE_BOX, scoreText, VALUE_SIZE, VALUE_PAD);
		valueRight(g, TIME_BOX, timeText, VALUE_SIZE, VALUE_PAD);

		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);

		gear(g);
	}

	private void tick() {
		remain--;
		timeText = mmss(remain);
		scoreText = String.format("%06d", board.score());
		repaint();

		if (remain <= 0 || board.getGame().isGameOver()) {
			clock.stop();
			board.setPaused(true);
			saveRanking();
			frame.showGameOver(board.score(), MainFrame.SINGLE);
		}
	}
}
