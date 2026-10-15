package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Sfx;
import kr.ac.jbnu.se.tetris.save.Settings;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * 숨은 레벨: 다방향 테트리스.
 *
 * 1인 모드에서 레벨 5를 깨면 열린다. 판 네 개가 십자로 놓이고 블록이 위/아래/왼쪽/오른쪽
 * 에서 각각 떨어진다. 규칙은 보통 테트리스 그대로고, 보드를 90도씩 돌려 그려서 방향만 바꾼다
 * (Board.setTurns). 그래서 네 판 모두 같은 Board 를 쓴다.
 *
 * 마우스로 판을 고르면 그 판에 조작키가 붙는다. 고르지 않은 판도 각자 타이머로 계속 떨어진다.
 * 기믹은 일정 시간마다 아무 판에나 무작위로 하나씩 걸린다.
 */
class HiddenPanel extends DesignPanel {

	/**
	 * 네 판의 자리. 한 칸 22px, 가운데 220x220 을 비우고 판 사이를 40px 띄운다.
	 * (붙여 두면 네 판의 테두리가 맞닿아 선이 겹쳐 보인다)
	 */
	private static final Rectangle[] SLOTS = {
			new Rectangle(890, 160, 220, 440),    // 위: 아래로 떨어진다
			new Rectangle(1150, 640, 440, 220),   // 오른쪽: 왼쪽으로 떨어진다
			new Rectangle(890, 900, 220, 440),    // 아래: 위로 떨어진다
			new Rectangle(410, 640, 440, 220),    // 왼쪽: 오른쪽으로 떨어진다
	};

	/** SLOTS 와 같은 순서. Board.setTurns 에 넘기는 값이다. */
	private static final int[] TURNS = { 0, 1, 2, 3 };

	private static final String[] NAMES = { "위", "오른쪽", "아래", "왼쪽" };

	/** 기믹을 하나 더 거는 간격. */
	private static final int GIMMICK_MS = 8000;

	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;

	// 십자의 네 귀퉁이가 비므로 다른 화면과 같은 탭 + 상자를 거기에 둔다
	private static final Rectangle SCORE_TAB = new Rectangle(140, 240, 190, 62);
	private static final Rectangle SCORE_BOX = new Rectangle(140, 302, 322, 100);
	private static final Rectangle PICK_TAB = new Rectangle(1538, 240, 190, 62);
	private static final Rectangle PICK_BOX = new Rectangle(1538, 302, 322, 100);
	private static final Rectangle GIMMICK_TAB = new Rectangle(140, 1000, 240, 62);

	/** 고른 판에 걸린 기믹 아이콘을 탭 아래에 한 줄로 둔다. */
	private static final int GIMMICK_X = 140, GIMMICK_TOP = 1090;
	private static final int GIMMICK_ICON = 84, GIMMICK_GAP = 14;

	/** 오른쪽 아래 안내문 */
	private static final int HINT_X = 1150, HINT_TOP = 1020, HINT_STEP = 60;
	private static final float HINT_SIZE = 30f;

	private final MainFrame frame;
	private final Board[] boards = new Board[SLOTS.length];
	private int selected = 0;
	private boolean finished = false;

	/** 아무 판에나 기믹을 하나 건다. */
	private final Timer gimmicks = new Timer(GIMMICK_MS, e -> addGimmick());

	/** 어느 판이든 끝났는지 지켜본다. */
	private final Timer watch = new Timer(500, e -> checkOver());

	HiddenPanel(MainFrame frame) {
		this.frame = frame;

		for (int i = 0; i < SLOTS.length; i++) {
			final int index = i;
			Board board = new Board(new JLabel());
			board.setBackground(Ui.BG);
			board.setTurns(TURNS[i]);
			board.onRepaint(this::repaint);
			add(board);
			boards[i] = board;
			// 클릭은 판(자식 컴포넌트)이 먼저 받으므로 판에 직접 붙인다
			board.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mousePressed(java.awt.event.MouseEvent e) {
					select(index);
				}
			});
		}

		Ui.bind(this, KeyEvent.VK_ESCAPE, "pause", this::pause);
		Ui.bind(this, KeyEvent.VK_P, "pause2", this::pause);
		onGear(this::pause);

		for (Board board : boards)
			board.start();
		select(0);
		gimmicks.start();
		watch.start();
	}

	/** 조작할 판을 고른다. 나머지 판은 그대로 떨어진다. */
	private void select(int index) {
		if (finished)
			return;
		selected = index;
		GameKeys.bind(this, 0, boards[index]);     // 조작키는 늘 고른 판에만 붙는다
		Sfx.play(Sfx.Sound.MOVE);
		repaint();
	}

	private void addGimmick() {
		boards[(int) (Math.random() * boards.length)].getGame().getGimmick().addRandom();
		repaint();
	}

	/** 한 판이라도 끝나면 전부 멈추고 점수를 합쳐서 마무리한다. */
	private void checkOver() {
		for (Board board : boards)
			if (board.getGame().isGameOver()) {
				finish();
				return;
			}
	}

	private void finish() {
		if (finished)
			return;
		finished = true;
		gimmicks.stop();
		watch.stop();
		for (Board board : boards)
			board.setPaused(true);

		int total = total();
		frame.showGameOver(total, Wallet.award(total), MainFrame.HIDDEN);
	}

	private int total() {
		int sum = 0;
		for (Board board : boards)
			sum += board.score();
		return sum;
	}

	private void pause() {
		if (finished)
			return;
		for (Board board : boards)
			board.setPaused(true);
		frame.showPause(MainFrame.HIDDEN, this::resume);
	}

	private void resume() {
		GameKeys.bind(this, 0, boards[selected]);   // 일시정지 중에 설정을 바꿨을 수 있다
		for (Board board : boards)
			board.setPaused(false);
	}

	@Override
	void layoutDesign() {
		for (int i = 0; i < boards.length; i++)
			boards[i].setBounds(screen(SLOTS[i]));
	}

	@Override
	void paintDesign(Graphics2D g) {
		textCenteredIn(g, "HIDDEN LEVEL", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);

		for (int i = 0; i < SLOTS.length; i++)
			frame(g, SLOTS[i], i == selected);

		tab(g, SCORE_TAB, "Score", TAB_SIZE);
		outline(g, SCORE_BOX);
		valueRight(g, SCORE_BOX, String.format("%06d", total()), VALUE_SIZE, VALUE_PAD);

		tab(g, PICK_TAB, "Board", TAB_SIZE);
		outline(g, PICK_BOX);
		valueRight(g, PICK_BOX, NAMES[selected], VALUE_SIZE, VALUE_PAD);

		tab(g, GIMMICK_TAB, "Gimmick", TAB_SIZE);
		gimmickIcons(g, boards[selected].getGame().getGimmick(),
				GIMMICK_X, GIMMICK_TOP, GIMMICK_ICON, GIMMICK_GAP);

		String[] hints = {
				"마우스로 판을 고르면 그 판을 조작합니다",
				"고르지 않은 판도 계속 떨어집니다",
				"Esc 일시정지",
		};
		for (int i = 0; i < hints.length; i++)
			text(g, hints[i], HINT_X, HINT_TOP + i * HINT_STEP, HINT_SIZE, DIM_CREAM);

		gear(g);
	}

	/**
	 * 판 테두리. 고른 판만 하얗고 굵게 그린다.
	 * (따로 한 겹 더 두르면 옆 판 테두리와 겹친다)
	 */
	private void frame(Graphics2D g, Rectangle slot, boolean on) {
		Rectangle b = screen(slot);
		int w = Math.max(1, sz(on ? BORDER + 4 : BORDER));
		g.setColor(on ? Color.WHITE : Ui.CREAM);
		g.setStroke(new BasicStroke(w));
		g.drawRect(b.x - w / 2, b.y - w / 2, b.width + w, b.height + w);
	}

	/**
	 * 자가 점검. java -ea 로 실행한다.
	 *
	 * (1) 네 판이 서로 다른 방향이고 돌려 놔도 칸이 정사각형인지
	 * (2) 고른 판에만 조작키가 붙고 나머지 판은 안 움직이는지
	 */
	public static void main(String[] args) {
		HiddenPanel p = new HiddenPanel(new MainFrame());
		p.setSize(DESIGN_W, DESIGN_H);
		p.doLayout();

		boolean[] seen = new boolean[4];
		for (int i = 0; i < SLOTS.length; i++) {
			seen[TURNS[i]] = true;
			Rectangle b = p.boards[i].getBounds();
			boolean sideways = TURNS[i] % 2 != 0;
			int cellW = (sideways ? b.height : b.width) / GameBoard.WIDTH;
			int cellH = (sideways ? b.width : b.height) / GameBoard.VISIBLE_HEIGHT;
			assert Math.abs(cellW - cellH) <= 1
					: NAMES[i] + " 판의 칸이 정사각형이 아니다: " + cellW + " x " + cellH;
		}
		for (boolean on : seen)
			assert on : "네 판의 떨어지는 방향이 겹친다";

		Settings.keys = Settings.copy(Settings.DEFAULT_KEYS);
		p.select(2);
		int picked = p.boards[2].getGame().getCurX();
		int other = p.boards[0].getGame().getCurX();
		press(p, Settings.keys[Settings.LEFT][0]);
		assert p.boards[2].getGame().getCurX() == picked - 1 : "고른 판이 안 움직인다";
		assert p.boards[0].getGame().getCurX() == other : "고르지 않은 판이 같이 움직였다";

		System.out.println("ok");
		System.exit(0);          // 판 네 개의 타이머가 돌고 있어서 그냥 두면 안 끝난다
	}

	/** 키 하나를 눌렀을 때와 같은 효과를 낸다. */
	private static void press(HiddenPanel p, int keyCode) {
		Object name = p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(keyCode, 0));
		p.getActionMap().get(name).actionPerformed(new ActionEvent(p, ActionEvent.ACTION_PERFORMED, ""));
	}
}
