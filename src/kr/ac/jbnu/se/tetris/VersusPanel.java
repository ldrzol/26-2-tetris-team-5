package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.ai.AIplayer;

/**
 * 대전 화면. Figma "Desktop - 9".
 *
 * 왼쪽이 플레이어, 오른쪽이 상대다. 상대를 AI 가 조종하면 AI 대전,
 * 아니면 2인 대전 화면이 된다.
 */
class VersusPanel extends DesignPanel {

	private static final int TIME_LIMIT = 300;   // 5분

	// ── Figma "Desktop - 9" 수치 (디자인 기준 픽셀) ─────────────────────────
	private static final Rectangle BOARD_1 = new Rectangle(268, 239, 550, 1100);
	private static final Rectangle BOARD_2 = new Rectangle(1183, 239, 550, 1100);
	private static final int CELL = 55;   // 550/10 = 1100/20 = 55, 자리에 딱 맞는다

	private static final Rectangle SCORE_TAB_1 = new Rectangle(382, 39, 190, 62);
	private static final Rectangle SCORE_BOX_1 = new Rectangle(382, 101, 322, 100);
	private static final Rectangle SCORE_TAB_2 = new Rectangle(1297, 39, 190, 62);
	private static final Rectangle SCORE_BOX_2 = new Rectangle(1297, 101, 322, 100);

	private static final Rectangle NEXT_TAB_1 = new Rectangle(18, 462, 186, 62);
	private static final Rectangle NEXT_BOX_1 = new Rectangle(18, 524, 232, 165);
	private static final Rectangle HOLD_TAB_1 = new Rectangle(18, 734, 186, 62);
	private static final Rectangle HOLD_BOX_1 = new Rectangle(18, 796, 232, 165);
	private static final Rectangle NEXT_TAB_2 = new Rectangle(1751, 462, 186, 62);
	private static final Rectangle NEXT_BOX_2 = new Rectangle(1751, 524, 232, 165);
	private static final Rectangle HOLD_TAB_2 = new Rectangle(1751, 734, 186, 62);
	private static final Rectangle HOLD_BOX_2 = new Rectangle(1751, 796, 232, 165);

	private static final Rectangle TIME_TAB = new Rectangle(906, 711, 190, 62);
	private static final Rectangle TIME_BOX = new Rectangle(864, 773, 273, 100);

	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;
	private static final int VS_TOP = 561, VS_HEIGHT = 80;
	private static final int LABEL_TOP = 304, LABEL_HEIGHT = 80;
	private static final int LABEL_1_X = 84, LABEL_2_X = 1803;
	private static final float BIG_SIZE = 64f;

	private final MainFrame frame;
	private final String kind;
	private final Board playerBoard = newBoard();
	private final Board rivalBoard = newBoard();

	private String score1 = "000000";
	private String score2 = "000000";
	private String timeText = "05:00";
	private int remain = TIME_LIMIT;

	private final Timer clock = new Timer(1000, e -> tick());

	VersusPanel(MainFrame frame, String kind, boolean aiRival) {
		this.frame = frame;
		this.kind = kind;

		add(playerBoard);
		add(rivalBoard);

		Ui.bind(this, KeyEvent.VK_LEFT, "left", playerBoard::moveLeft);
		Ui.bind(this, KeyEvent.VK_RIGHT, "right", playerBoard::moveRight);
		Ui.bind(this, KeyEvent.VK_UP, "rotate", playerBoard::rotateRight);
		Ui.bind(this, KeyEvent.VK_DOWN, "soft", playerBoard::softDrop);
		Ui.bind(this, KeyEvent.VK_SPACE, "hard", playerBoard::hardDrop);
		Ui.bind(this, KeyEvent.VK_C, "hold", playerBoard::hold);
		Ui.bind(this, KeyEvent.VK_SHIFT, "hold2", playerBoard::hold);

		playerBoard.start();
		rivalBoard.start();
		if (aiRival)
			new AIplayer(rivalBoard).start();

		clock.start();
	}

	private Board newBoard() {
		Board b = new Board(new JLabel());   // Board 가 상태바를 요구한다. 화면엔 안 붙인다.
		b.setBackground(Ui.BG);
		b.onRepaint(this::repaint);          // Hold/Next 는 패널이 그리므로 같이 갱신해야 한다
		return b;
	}

	@Override
	void layoutDesign() {
		playerBoard.setBounds(boardBounds(BOARD_1, CELL));
		rivalBoard.setBounds(boardBounds(BOARD_2, CELL));
	}

	@Override
	void paintDesign(Graphics2D g) {
		boardFrame(g, boardBounds(BOARD_1, CELL));
		boardFrame(g, boardBounds(BOARD_2, CELL));

		outline(g, SCORE_BOX_1);
		outline(g, SCORE_BOX_2);
		outline(g, NEXT_BOX_1);
		outline(g, HOLD_BOX_1);
		outline(g, NEXT_BOX_2);
		outline(g, HOLD_BOX_2);
		outline(g, TIME_BOX);

		tab(g, SCORE_TAB_1, "Score", TAB_SIZE);
		tab(g, SCORE_TAB_2, "Score", TAB_SIZE);
		tab(g, NEXT_TAB_1, "Next", TAB_SIZE);
		tab(g, HOLD_TAB_1, "Hold", TAB_SIZE);
		tab(g, NEXT_TAB_2, "Next", TAB_SIZE);
		tab(g, HOLD_TAB_2, "Hold", TAB_SIZE);
		tab(g, TIME_TAB, "Time", TAB_SIZE);

		previews(g, playerBoard, NEXT_BOX_1, HOLD_BOX_1);
		previews(g, rivalBoard, NEXT_BOX_2, HOLD_BOX_2);

		valueRight(g, SCORE_BOX_1, score1, VALUE_SIZE, VALUE_PAD);
		valueRight(g, SCORE_BOX_2, score2, VALUE_SIZE, VALUE_PAD);
		valueCentered(g, TIME_BOX, timeText, VALUE_SIZE);

		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);
		textCenteredIn(g, "VS", VS_TOP, VS_HEIGHT, BIG_SIZE, Color.WHITE);

		g.setColor(Color.WHITE);
		g.setFont(font(BIG_SIZE));
		// 디자인에는 "!st" / "2st" 로 적혀 있으나 오타라 1st / 2nd 로 쓴다
		g.drawString("1st", px(LABEL_1_X), centeredBaseline(g, LABEL_TOP, LABEL_HEIGHT));
		g.drawString("2nd", px(LABEL_2_X), centeredBaseline(g, LABEL_TOP, LABEL_HEIGHT));

		gear(g);
	}

	private void previews(Graphics2D g, Board board, Rectangle nextBox, Rectangle holdBox) {
		GameBoard game = board.getGame();
		preview(g, nextBox, game.nextType(), game.getNextColor());
		preview(g, holdBox, game.holdType(), game.getHoldColor());
	}

	private void tick() {
		remain--;
		timeText = mmss(remain);
		score1 = String.format("%06d", playerBoard.score());
		score2 = String.format("%06d", rivalBoard.score());
		repaint();

		if (remain <= 0 || playerBoard.getGame().isGameOver() || rivalBoard.getGame().isGameOver()) {
			clock.stop();
			playerBoard.setPaused(true);
			rivalBoard.setPaused(true);
			frame.showGameOver(playerBoard.score(), kind);
		}
	}
}
