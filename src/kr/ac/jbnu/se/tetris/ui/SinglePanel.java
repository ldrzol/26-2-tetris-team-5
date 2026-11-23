package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.save.Ranking;
import kr.ac.jbnu.se.tetris.save.Settings;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.Timer;

/** 1인 모드 화면. Figma "리팩 - 1인 모드". */
class SinglePanel extends DesignPanel {

	/** 이 레벨을 깨면 숨은 레벨(다방향 테트리스)이 열린다. */
	private static final int HIDDEN_LEVEL = 5;

	// Figma "리팩 - 1인 모드" 수치 (디자인 기준 픽셀)
	private static final Rectangle BOARD = new Rectangle(700, 111, 600, 1200);
	private static final Rectangle HOLD = new Rectangle(268, 111, 359, 297);
	private static final Rectangle NEXT = new Rectangle(1373, 111, 359, 297);
	private static final Rectangle SCORE = new Rectangle(268, 529, 359, 452);

	/** SCORE 상자 안: 점수 한 줄, 그 아래 Level / Line / Time 줄. (Time 은 디자인에 없어 한 줄 더했다) */
	private static final int SCORE_TOP = 655, ROW_X = 302, ROW_RIGHT = 593;
	private static final int[] ROW_TOP = { 755, 825, 895 };
	private static final float SCORE_SIZE = 52f, ROW_SIZE = 36f;

	/** 디자인에 없는 것들은 NEXT 상자 아래 빈 자리에 둔다: 켜진 기믹 아이콘, 콤보, 안내문. */
	private static final int SIDE_X = 1373, GIMMICK_TOP = 460, GIMMICK_ICON = 84, GIMMICK_GAP = 14;
	private static final int COMBO_TOP = 570, NOTICE_TOP = 660;
	private static final float COMBO_SIZE = 56f, NOTICE_SIZE = 34f;

	private final MainFrame frame;
	private final Board board;
	private String notice = "";
	private String timeText = "00:00";
	private int elapsed = 0;

	// 보드가 바뀔 때 알려 주지 않아서, 1초마다 점수와 남은 시간을 읽어 간다.
	private final Timer clock = new Timer(1000, e -> tick());

	SinglePanel(MainFrame frame) {
		this.frame = frame;

		board = new Board(new JLabel());   // Board 가 상태바를 요구한다. 화면엔 안 붙인다.
		board.setOpaque(false);            // 보드 상자의 하늘색이 비쳐야 한다
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
		frame.showLevelClear(board.score(), Wallet.awardLevel(level), stats(level), () -> {
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
			frame.showGameOver(score, Wallet.award(score), MainFrame.SINGLE, stats(board.getGame().getLevel()));
		});
	}

	/** 결과 카드의 작은 줄들: 걸린 시간, 레벨, 지운 줄 수. */
	private String[] stats(int level) {
		return new String[] { timeText, "Level " + level, "Line  " + board.getGame().getLinesRemoved() };
	}

	@Override
	void layoutDesign() {
		board.setBounds(boardBounds(BOARD));
	}

	@Override
	void paintDesign(Graphics2D g) {
		background(g, SKY);
		boardFrame(g, board.getBounds(), Ui.BROWN);

		awningPanel(g, HOLD, "HOLD");
		awningPanel(g, NEXT, "NEXT");
		awningPanel(g, SCORE, "SCORE");

		GameBoard game = board.getGame();
		preview(g, below(HOLD, 16), game.holdType(), game.getHoldColor());
		preview(g, below(NEXT, 16), game.nextType(), game.getNextColor());

		// 점수·레벨·기믹은 그릴 때마다 보드에서 바로 읽는다. (1초 시계를 기다리지 않는다)
		textAt(g, String.format("%,d", board.score()), SCORE.x + SCORE.width / 2.0, SCORE_TOP, SCORE_SIZE, Color.BLACK);
		String[] names = { "Level", "Line", "Time" };
		String[] values = { String.valueOf(game.getLevel()), String.valueOf(game.getLinesRemoved()), timeText };
		for (int i = 0; i < names.length; i++) {
			text(g, names[i], ROW_X, ROW_TOP[i], ROW_SIZE, Color.BLACK);
			textRight(g, values[i], ROW_RIGHT, ROW_TOP[i], ROW_SIZE, Color.BLACK);
		}

		gimmickIcons(g, game.getGimmick(), SIDE_X, GIMMICK_TOP, GIMMICK_ICON, GIMMICK_GAP);
		if (game.getCombo() > 0)                                   // 두 번째 연속 삭제부터 보여 준다
			text(g, game.getCombo() + " COMBO", SIDE_X, COMBO_TOP, COMBO_SIZE, Ui.RED);
		text(g, notice, SIDE_X, NOTICE_TOP, NOTICE_SIZE, Ui.TEXT);

		gear(g);
	}

	private void tick() {
		elapsed++;
		timeText = mmss(elapsed);
		repaint();

		if (board.getGame().isGameOver()) {
			clock.stop();
			board.setPaused(true);
			finish();
		}
	}
}
