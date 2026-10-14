package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.ai.AIplayer;
import kr.ac.jbnu.se.tetris.network.BoardStateMessage;
import kr.ac.jbnu.se.tetris.network.ItemMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

/**
 * 대전 화면. Figma "Desktop - 9".
 *
 * 왼쪽이 플레이어, 오른쪽이 상대다. 상대는 셋 중 하나다.
 *   - AI 가 조종하는 보드 (AI 대전)
 *   - 같은 PC 의 두 번째 보드
 *   - 다른 PC 에서 보내온 화면을 그대로 비추는 보드 (네트워크 2인 대전)
 *
 * 네트워크 대전에서만 아이템을 쓴다. 손에 세 개까지 들고 1~3 키로 고른다.
 */
class VersusPanel extends DesignPanel {

	private static final int TIME_LIMIT = 300;   // 5분

	/** 손에 들 수 있는 아이템 수. 키 1~3 과 같아야 한다. */
	private static final int HAND_SIZE = 3;

	/** 이만큼 점수를 올릴 때마다 아이템을 하나 받는다. */
	private static final int ITEM_SCORE = 500;

	/** 상대에게 내 화면을 보내는 간격(ms). */
	private static final int SNAPSHOT_MS = 100;

	// Figma "Desktop - 9" 수치 (디자인 기준 픽셀)
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

	// 양쪽 아래: Hold 상자 밑에 Level 과 켜진 기믹 목록
	private static final Rectangle LEVEL_TAB_1 = new Rectangle(18, 1005, 186, 62);
	private static final Rectangle LEVEL_BOX_1 = new Rectangle(18, 1067, 232, 90);
	private static final Rectangle LEVEL_TAB_2 = new Rectangle(1751, 1005, 186, 62);
	private static final Rectangle LEVEL_BOX_2 = new Rectangle(1751, 1067, 232, 90);

	/** Level 상자 아래에 켜진 기믹 아이콘을 한 줄로 둔다. (동시에 최대 3개) */
	private static final int GIMMICK_DROP = 16, GIMMICK_ICON = 64, GIMMICK_GAP = 10;

	private static final Rectangle TIME_TAB = new Rectangle(906, 711, 190, 62);
	private static final Rectangle TIME_BOX = new Rectangle(864, 773, 273, 100);

	// 두 보드 사이 빈 자리에 아이템 칸을 둔다. (네트워크 대전에서만 그린다)
	private static final Rectangle ITEM_TAB = new Rectangle(864, 900, 190, 62);
	private static final Rectangle ITEM_BOX = new Rectangle(864, 962, 273, 300);
	private static final int ITEM_ROW_TOP = 20, ITEM_ROW_H = 90;
	private static final int ITEM_ICON = 64, ITEM_PAD = 24, ITEM_TEXT_GAP = 14;
	private static final float ITEM_SIZE = 30f;

	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;
	private static final int VS_TOP = 561, VS_HEIGHT = 80;
	private static final int LABEL_TOP = 304, LABEL_HEIGHT = 80;
	private static final int LABEL_1_X = 84, LABEL_2_X = 1803;
	private static final float BIG_SIZE = 64f;

	private final MainFrame frame;
	private final String kind;
	private final Board playerBoard = newBoard();
	private final Board rivalBoard = newBoard();

	/** 네트워크 대전일 때만 연결이 있다. AI·로컬 대전은 null. */
	private final PeerConnection connection;
	private final int fee;

	private final Attack[] hand = new Attack[HAND_SIZE];
	private final Random random = new Random();

	/** 상대 점수. 네트워크 대전에서는 받아 온 값을 쓴다. */
	private int rivalScore = 0;
	private boolean rivalOver = false;
	private int nextItemScore = ITEM_SCORE;
	private boolean finished = false;

	private String timeText = "05:00";
	private int remain = TIME_LIMIT;

	private final Timer clock = new Timer(1000, e -> tick());

	/** AI 대전 또는 같은 PC 대전. */
	VersusPanel(MainFrame frame, String kind, boolean aiRival) {
		this(frame, kind, aiRival, null, 0);
	}

	/** 네트워크 2인 대전. 끝나면 참가비를 정산한다. */
	VersusPanel(MainFrame frame, PeerConnection connection, int fee) {
		this(frame, MainFrame.LOBBY, false, connection, fee);
	}

	private VersusPanel(MainFrame frame, String kind, boolean aiRival,
			PeerConnection connection, int fee) {
		this.frame = frame;
		this.kind = kind;
		this.connection = connection;
		this.fee = fee;

		add(playerBoard);
		add(rivalBoard);

		GameKeys.bind(this, 0, playerBoard);   // 설정한 조작키 (+ X, Shift 보조 키)
		Ui.bind(this, KeyEvent.VK_ESCAPE, "pause", this::pause);
		Ui.bind(this, KeyEvent.VK_P, "pause2", this::pause);
		onGear(this::pause);

		playerBoard.start();
		rivalBoard.start();

		if (connection != null)
			startNetworkPlay();
		else if (aiRival)
			new AIplayer(rivalBoard, frame.aiLevel()).start();

		clock.start();
	}

	// 네트워크 대전

	/** 상대 보드는 받아 온 화면을 비추기만 하므로 멈춰 둔다. */
	private void startNetworkPlay() {
		rivalBoard.setPaused(true);

		for (int i = 0; i < HAND_SIZE; i++) {
			final int slot = i;
			Ui.bind(this, KeyEvent.VK_1 + i, "item" + i, () -> useItem(slot));
		}

		connection.setListener(this::onMessage);
		new Timer(SNAPSHOT_MS, e -> sendSnapshot()).start();
	}

	private void sendSnapshot() {
		Tetrominoes[] cells = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
		int[] colors = new int[cells.length];
		GameBoard game = playerBoard.getGame();
		game.writeVisible(cells, colors);
		connection.send(new BoardStateMessage(cells, colors,
				game.getLinesRemoved(), game.getScore(), game.isGameOver()));
	}

	private void onMessage(Object message) {
		if (message instanceof BoardStateMessage)
			SwingUtilities.invokeLater(() -> receiveState((BoardStateMessage) message));
		else if (message instanceof ItemMessage)
			SwingUtilities.invokeLater(() -> receiveItem((ItemMessage) message));
	}

	private void receiveState(BoardStateMessage state) {
		rivalBoard.getGame().applyRemoteState(state.cells, state.colors);
		rivalScore = state.score;
		rivalOver = state.gameOver;
		rivalBoard.repaint();
		repaint();
	}

	private void receiveItem(ItemMessage item) {
		if (item.attack == Attack.SWAP)
			receiveSwap(item);
		else
			playerBoard.getGame().getGimmick().apply(item.attack);
		playerBoard.repaint();
	}

	/**
	 * 블록 교환을 받는다.
	 *
	 * 먼저 받은 쪽은 상대 블록을 받고 자기 것을 답장으로 돌려준다. 답장을 받은 쪽은
	 * 거기 실린 블록으로 바꾸기만 한다. 내가 못 받는 상황이면 빈 모양을 돌려보내서
	 * 상대도 그대로 두게 한다. 그래야 블록이 늘거나 사라지지 않는다.
	 */
	private void receiveSwap(ItemMessage item) {
		if (item.reply) {
			playerBoard.getGame().replaceCurrentPiece(item.piece);
			return;
		}
		Tetrominoes mine = playerBoard.getGame().currentType();
		boolean took = playerBoard.getGame().replaceCurrentPiece(item.piece);
		connection.send(new ItemMessage(Attack.SWAP, took ? mine : Tetrominoes.NoShape, true));
	}

	// 아이템

	/** 빈 자리에 아이템을 하나 넣는다. 손이 가득 차 있으면 그냥 버린다. */
	private void addItem() {
		for (int i = 0; i < hand.length; i++) {
			if (hand[i] == null) {
				hand[i] = Attack.random(random);
				repaint();
				return;
			}
		}
	}

	/** 고른 자리의 아이템을 쓴다. 빈 자리면 아무 일도 하지 않는다. */
	private void useItem(int slot) {
		if (finished || hand[slot] == null)
			return;

		Attack attack = hand[slot];
		hand[slot] = null;
		Sfx.play(Sfx.Sound.ITEM);

		if (attack == Attack.SWAP) {
			// 내 블록을 보내기만 하고, 상대가 쥔 것은 답장으로 받는다. 받아 둔 화면에서
			// 꺼내 쓰면 최대 SNAPSHOT_MS 만큼 옛날 것이라 교환이 어긋난다.
			connection.send(new ItemMessage(Attack.SWAP, playerBoard.getGame().currentType(), false));
		} else if (attack.self) {
			playerBoard.getGame().getGimmick().apply(attack);
		} else {
			connection.send(new ItemMessage(attack));
		}
		repaint();
	}

	// 진행

	/** 톱니(또는 Esc)를 누르면 두 판을 멈추고 Figma "리플레이 화면" 을 덮는다. */
	private void pause() {
		if (!playerBoard.isRunning())
			return;
		clock.stop();
		playerBoard.setPaused(true);
		if (connection == null)
			rivalBoard.setPaused(true);
		frame.showPause(kind, this::resume);
	}

	private void resume() {
		GameKeys.bind(this, 0, playerBoard);   // 일시정지 중에 설정을 바꿨을 수 있다
		playerBoard.setPaused(false);
		if (connection == null)
			rivalBoard.setPaused(false);
		clock.start();
	}

	private Board newBoard() {
		Board b = new Board(new JLabel());   // Board 가 상태바를 요구한다. 화면엔 안 붙인다.
		b.getGame().setLevelProgression(false);
		b.setBackground(Ui.BG);
		b.onRepaint(this::repaint);          // Hold/Next 는 패널이 그리므로 같이 갱신해야 한다
		return b;
	}

	private void tick() {
		remain--;
		timeText = mmss(remain);

		if (connection != null && playerBoard.score() >= nextItemScore) {
			nextItemScore += ITEM_SCORE;
			addItem();
		}
		repaint();

		if (remain <= 0 || playerBoard.getGame().isGameOver() || rivalLost())
			finish();
	}

	private boolean rivalLost() {
		return connection != null ? rivalOver : rivalBoard.getGame().isGameOver();
	}

	private int rivalScore() {
		return connection != null ? rivalScore : rivalBoard.score();
	}

	private void finish() {
		if (finished)
			return;
		finished = true;
		clock.stop();
		playerBoard.setPaused(true);
		if (connection == null)
			rivalBoard.setPaused(true);

		String title = resultTitle();
		int coins = connection != null ? settle(title) : 0;
		frame.showResult(title, playerBoard.score(), coins, kind);
	}

	/** 참가비를 정산하고 받은 코인을 돌려준다. */
	private int settle(String title) {
		if (title.equals("YOU WIN")) {
			Wallet.addCoins(fee * 2);
			return fee * 2;
		}
		if (title.equals("DRAW")) {
			Wallet.addCoins(fee);
			return fee;
		}
		return 0;   // 졌으면 걸어 둔 참가비를 그대로 잃는다
	}

	private String resultTitle() {
		boolean playerOver = playerBoard.getGame().isGameOver();
		if (playerOver && !rivalLost())
			return "YOU LOSE";
		if (rivalLost() && !playerOver)
			return "YOU WIN";
		int mine = playerBoard.score();
		int theirs = rivalScore();
		return mine > theirs ? "YOU WIN" : mine < theirs ? "YOU LOSE" : "DRAW";
	}

	// 그리기

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
		outline(g, LEVEL_BOX_1);
		outline(g, LEVEL_BOX_2);

		tab(g, SCORE_TAB_1, "Score", TAB_SIZE);
		tab(g, SCORE_TAB_2, "Score", TAB_SIZE);
		tab(g, NEXT_TAB_1, "Next", TAB_SIZE);
		tab(g, HOLD_TAB_1, "Hold", TAB_SIZE);
		tab(g, NEXT_TAB_2, "Next", TAB_SIZE);
		tab(g, HOLD_TAB_2, "Hold", TAB_SIZE);
		tab(g, TIME_TAB, "Time", TAB_SIZE);
		tab(g, LEVEL_TAB_1, "Level", TAB_SIZE);
		tab(g, LEVEL_TAB_2, "Level", TAB_SIZE);

		previews(g, playerBoard, NEXT_BOX_1, HOLD_BOX_1);
		if (connection == null)
			previews(g, rivalBoard, NEXT_BOX_2, HOLD_BOX_2);   // 비추기만 할 때는 알 수 없다

		// 점수·레벨은 그릴 때마다 보드에서 바로 읽는다. (1초 시계를 기다리지 않는다)
		valueRight(g, SCORE_BOX_1, String.format("%06d", playerBoard.score()), VALUE_SIZE, VALUE_PAD);
		valueRight(g, SCORE_BOX_2, String.format("%06d", rivalScore()), VALUE_SIZE, VALUE_PAD);
		valueCentered(g, TIME_BOX, timeText, VALUE_SIZE);
		sideInfo(g, playerBoard, LEVEL_BOX_1);
		sideInfo(g, rivalBoard, LEVEL_BOX_2);

		if (connection != null)
			items(g);

		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);
		textCenteredIn(g, "VS", VS_TOP, VS_HEIGHT, BIG_SIZE, Color.WHITE);

		g.setColor(Color.WHITE);
		g.setFont(font(BIG_SIZE));
		// 디자인에는 "!st" / "2st" 로 적혀 있으나 오타라 1st / 2nd 로 쓴다
		g.drawString("1st", px(LABEL_1_X), centeredBaseline(g, LABEL_TOP, LABEL_HEIGHT));
		g.drawString("2nd", px(LABEL_2_X), centeredBaseline(g, LABEL_TOP, LABEL_HEIGHT));

		gear(g);
	}

	/** 손에 든 아이템. 빈 자리는 번호만 흐리게 적는다. */
	private void items(Graphics2D g) {
		tab(g, ITEM_TAB, "Item", TAB_SIZE);
		outline(g, ITEM_BOX);

		for (int i = 0; i < hand.length; i++) {
			int x = ITEM_BOX.x + ITEM_PAD;
			int top = ITEM_BOX.y + ITEM_ROW_TOP + i * ITEM_ROW_H;
			Attack attack = hand[i];

			if (attack != null)
				drawImage(g, image(attack.icon), new Rectangle(x, top, ITEM_ICON, ITEM_ICON));

			text(g, (i + 1) + "  " + (attack == null ? "-" : attack.label),
					x + ITEM_ICON + ITEM_TEXT_GAP, top + ITEM_ICON / 4, ITEM_SIZE,
					attack == null ? DIM_CREAM : Ui.CREAM);
		}
	}

	/** 한쪽 보드의 레벨과, 그 아래에 켜진 기믹 아이콘. */
	private void sideInfo(Graphics2D g, Board board, Rectangle levelBox) {
		valueRight(g, levelBox, String.valueOf(board.getGame().getLevel()), VALUE_SIZE, VALUE_PAD);
		gimmickIcons(g, board.getGame().getGimmick(),
				levelBox.x, levelBox.y + levelBox.height + GIMMICK_DROP, GIMMICK_ICON, GIMMICK_GAP);
	}

	private void previews(Graphics2D g, Board board, Rectangle nextBox, Rectangle holdBox) {
		GameBoard game = board.getGame();
		preview(g, nextBox, game.nextType(), game.getNextColor());
		preview(g, holdBox, game.holdType(), game.getHoldColor());
	}
}
