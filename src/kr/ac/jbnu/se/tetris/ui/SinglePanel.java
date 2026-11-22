package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.save.Ranking;
import kr.ac.jbnu.se.tetris.save.Settings;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.Timer;

/** 1인 모드 화면. Figma "Desktop - 6". */
class SinglePanel extends DesignPanel {

	private static final int TIME_LIMIT = 300;   // 5분

	/** 이 레벨을 깨면 숨은 레벨(다방향 테트리스)이 열린다. */
	private static final int HIDDEN_LEVEL = 5;

	// Figma "Desktop - 6" 수치 (디자인 기준 픽셀)
	private static final Rectangle BOARD = new Rectangle(700, 144, 600, 1200);

	private static final Rectangle HOLD_TAB = new Rectangle(311, 147, 186, 62);
	private static final Rectangle HOLD_BOX = new Rectangle(311, 209, 322, 181);
	private static final Rectangle NEXT_TAB = new Rectangle(1367, 147, 186, 62);
	private static final Rectangle NEXT_BOX = new Rectangle(1367, 209, 322, 181);
	private static final Rectangle SCORE_TAB = new Rectangle(1367, 591, 190, 62);
	private static final Rectangle SCORE_BOX = new Rectangle(1367, 653, 322, 100);
	private static final Rectangle TIME_TAB = new Rectangle(1367, 808, 190, 62);
	private static final Rectangle TIME_BOX = new Rectangle(1367, 870, 322, 100);
	/** 아이템 안내문 자리 */
	private static final int NOTICE_TOP = 1360;
	private static final float NOTICE_SIZE = 36f;

	// 왼쪽 아래: Score/Time 과 같은 높이에 Level, 그 아래에 켜진 기믹 목록
	private static final Rectangle LEVEL_TAB = new Rectangle(311, 591, 190, 62);
	private static final Rectangle LEVEL_BOX = new Rectangle(311, 653, 322, 100);

	/** Level 상자 바로 아래에 켜진 기믹 아이콘을 한 줄로 둔다. (동시에 최대 3개) */
	private static final int GIMMICK_X = 311, GIMMICK_TOP = 779;
	private static final int GIMMICK_ICON = 84, GIMMICK_GAP = 14;

	/** 콤보 표시 자리. 기믹 아이콘 줄 아래, 보드 왼쪽. */
	private static final int COMBO_X = 311, COMBO_TOP = 900;
	private static final float COMBO_SIZE = 56f;

	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;

	/**
	 * 보드 한 칸의 크기(디자인 기준 px). 디자인 원본은 60 으로, 그때 보드가 600x1200 자리를
	 * 정확히 채운다. 줄이면 보드 전체가 같은 자리 가운데에서 작아진다.
	 */
	private static final int CELL = 60;

	private final MainFrame frame;
	private final Board board;
	private String notice = "";
	private String timeText = "05:00";
	private int remain = TIME_LIMIT;

	// 보드가 바뀔 때 알려 주지 않아서, 1초마다 점수와 남은 시간을 읽어 간다.
	private final Timer clock = new Timer(1000, e -> tick());

	SinglePanel(MainFrame frame) {
		this.frame = frame;

		board = new Board(new JLabel());   // Board 가 상태바를 요구한다. 화면엔 안 붙인다.
		board.setBackground(Ui.BG);
		board.onRepaint(this::repaint);    // Hold/Next 는 패널이 그리므로 같이 갱신해야 한다
		add(board);

		GameKeys.bind(this, 0, board);     // 설정한 조작키 (+ X, Shift 보조 키)
		Ui.bind(this, KeyEvent.VK_ESCAPE, "pause", this::pause);
		Ui.bind(this, KeyEvent.VK_P, "pause2", this::pause);
		onGear(this::pause);

		board.onLevelUp(this::levelClear);
		board.onFever(this::fever);

		board.start();
		clock.start();
	}

	/**
	 * 레벨을 깼을 때. 판을 멈추고 덮개를 띄운 뒤, '다음 레벨' 을 고르면
	 * 쌓인 블록만 비우고 이어서 한다. (점수·레벨은 그대로)
	 */
	private void levelClear(int level) {
		clock.stop();
		board.setPaused(true);
		frame.showLevelClear(level, board.score(), Wallet.awardLevel(level), () -> {
			if (level >= HIDDEN_LEVEL) {            // 숨은 레벨: 다방향 테트리스로 넘어간다
				frame.showScreen(MainFrame.HIDDEN);
				return;
			}
			board.clearStack();
			notice = "LEVEL " + (level + 1) + " 시작!";
			resume();
		});
	}

	/**
	 * [피버] 테트리스(4줄)나 콤보로 터진다. 판을 멈추고 원하는 블록을 고르게 한 뒤,
	 * 그 블록을 다음 블록으로 넣고 잠깐 점수가 두 배가 된다.
	 */
	private void fever() {
		clock.stop();
		board.setPaused(true);
		frame.showPick("FEVER!  원하는 블록을 고르세요", shape -> {
			board.startFever(shape);
			say("FEVER! 잠시 점수 2배");
			resume();
		});
	}

	private void say(String message) {
		notice = message;
		repaint();
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
		GameKeys.bind(this, 0, board);     // 일시정지 중에 설정을 바꿨을 수 있다
		board.setPaused(false);
		clock.start();
	}

	/**
	 * 이름을 물어보고 랭킹에 한 줄 남긴 뒤 GAME OVER 를 덮는다. 취소하면 남기지 않는다.
	 */
	private void finish() {
		int score = board.score();
		frame.showInput("랭킹에 남길 이름", Settings.playerName, name -> {
			if (name != null) {
				Settings.playerName = Ranking.clean(name);
				Ranking.add(Settings.playerName, score);
			}
			frame.showGameOver(score, Wallet.award(score), MainFrame.SINGLE);
		});
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
		outline(g, LEVEL_BOX);

		tab(g, HOLD_TAB, "Hold", TAB_SIZE);
		tab(g, NEXT_TAB, "Next", TAB_SIZE);
		tab(g, SCORE_TAB, "Score", TAB_SIZE);
		tab(g, TIME_TAB, "Time", TAB_SIZE);
		tab(g, LEVEL_TAB, "Level", TAB_SIZE);

		GameBoard game = board.getGame();
		preview(g, HOLD_BOX, game.holdType(), game.getHoldColor());
		preview(g, NEXT_BOX, game.nextType(), game.getNextColor());

		// 점수·레벨·기믹은 그릴 때마다 보드에서 바로 읽는다. (1초 시계를 기다리지 않는다)
		valueRight(g, SCORE_BOX, String.format("%06d", board.score()), VALUE_SIZE, VALUE_PAD);
		valueRight(g, TIME_BOX, timeText, VALUE_SIZE, VALUE_PAD);
		valueRight(g, LEVEL_BOX, String.valueOf(game.getLevel()), VALUE_SIZE, VALUE_PAD);
		gimmickIcons(g, game.getGimmick(), GIMMICK_X, GIMMICK_TOP, GIMMICK_ICON, GIMMICK_GAP);
		if (game.getCombo() > 0)                                   // 두 번째 연속 삭제부터 보여 준다
			text(g, game.getCombo() + " COMBO", COMBO_X, COMBO_TOP, COMBO_SIZE, Ui.CREAM);

		textCentered(g, notice, NOTICE_TOP, NOTICE_SIZE, Ui.CREAM, 0);

		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);

		gear(g);
	}

	private void tick() {
		remain--;
		timeText = mmss(remain);
		repaint();

		if (remain <= 0 || board.getGame().isGameOver()) {
			clock.stop();
			board.setPaused(true);
			finish();
		}
	}
}
