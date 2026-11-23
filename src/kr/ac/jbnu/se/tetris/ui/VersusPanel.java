package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Sfx;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
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
 * 아이템은 네트워크 대전과 한 화면 2인 대전에서 쓴다. 손패는 플레이어마다 따로 있고,
 * 상점에서 산 가방(Wallet)에서 올라온다. 자리를 고른 뒤 사용 키를 누르면 가방에서 빠진다.
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

	/** 기믹 아이콘 줄 아래에 콤보 표시. (두 번째 연속 삭제부터) */
	private static final int COMBO_DROP = 102;
	private static final float COMBO_SIZE = 40f;

	private static final Rectangle TIME_TAB = new Rectangle(906, 711, 190, 62);
	private static final Rectangle TIME_BOX = new Rectangle(864, 773, 273, 100);

	// 두 보드 사이 빈 자리에 아이템 칸을 둔다. 위가 1P, 아래가 2P 것이다.
	private static final Rectangle[] ITEM_TABS = {
			new Rectangle(864, 900, 190, 62), new Rectangle(864, 1150, 190, 62) };
	private static final Rectangle[] ITEM_BOXES = {
			new Rectangle(864, 962, 273, 170), new Rectangle(864, 1212, 273, 170) };

	private static final int ITEM_SLOT = 74, ITEM_SLOT_GAP = 10, ITEM_SLOT_TOP = 14;
	private static final int ITEM_SLOT_BORDER = 4, ITEM_ICON = 64;
	private static final int ITEM_LABEL_DROP = 94, ITEM_HINT_DROP = 128;
	private static final float ITEM_SIZE = 28f, ITEM_HINT_SIZE = 22f;

	/** 아이템을 쓰거나 받았을 때 그 사람 아이템 칸에 글자를 띄우고 칸을 반짝이는 시간(ms). */
	private static final int NOTICE_MS = 1500;

	// 왼쪽 위 뒤로 버튼 (환경 설정 화면의 뒤로와 같은 모양)
	private static final Rectangle BACK = new Rectangle(18, 66, 232, 81);
	private static final Rectangle BACK_ARROW = new Rectangle(77, 88, 20, 37);
	private static final int BACK_TEXT_X = 124;
	private static final int BACK_TEXT_TOP = 79;
	private static final String LEAVE = "나가기";
	private static final String STAY = "계속하기";

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

	/** 손패. [0] = 왼쪽(나), [1] = 오른쪽(상대). 플레이어마다 따로 든다. */
	private final Attack[][] hands = new Attack[2][HAND_SIZE];

	/** 지금 고른 손패 자리. 사용 키를 누르면 이 자리를 쓴다. */
	private final int[] picked = new int[2];

	/** 다음 아이템을 받을 점수. 플레이어마다 따로 센다. */
	private final int[] nextItemScore = { ITEM_SCORE, ITEM_SCORE };

	/** 이번 판에 쓸 아이템 더미. 가방에서 옮겨 와 손패에 나눠 준다. */
	private final List<Attack> stock = new ArrayList<>();

	private final Random random = new Random();

	/** 한 화면에서 둘이 잡는 판인지. 오른쪽 보드도 사람이 조종한다. */
	private final boolean local;

	/** 아이템을 쓰는 판인지. AI 대전에서는 쓰지 않는다. */
	private final boolean useItems;

	/** 상대 점수. 네트워크 대전에서는 받아 온 값을 쓴다. */
	private int rivalScore = 0;
	private int rivalCombo = -1;
	private boolean rivalOver = false;
	private boolean finished = false;

	private String timeText = "05:00";
	private int remain = TIME_LIMIT;

	private final Timer clock = new Timer(1000, e -> tick());

	/** 아이템 칸 안내 글자와 반짝일 칸. [0] = 왼쪽, [1] = 오른쪽. 칸이 없으면 -1. */
	private final String[] notice = new String[2];
	private final int[] noticeSlot = { -1, -1 };
	private final long[] noticeUntil = new long[2];

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
		this.local = connection == null && !aiRival;
		this.useItems = connection != null || local;

		add(playerBoard);
		add(rivalBoard);

		bindGameKeys();
		Ui.bind(this, KeyEvent.VK_ESCAPE, "pause", this::pause);
		Ui.bind(this, KeyEvent.VK_P, "pause2", this::pause);
		onGear(this::pause);
		onClick(BACK, this::askLeave);

		playerBoard.onFever(() -> fever(0));
		if (local)
			rivalBoard.onFever(() -> fever(1));

		playerBoard.start();
		rivalBoard.start();

		if (connection != null)
			startNetworkPlay();
		else if (aiRival)
			new AIplayer(rivalBoard, frame.aiLevel()).start();

		if (useItems) {
			bindItemKeys(0);
			if (local)
				bindItemKeys(1);
			loadStock();
			// 덮개는 화면이 올라온 뒤에 띄워야 한다. 지금 띄우면 화면을 갈아 끼울 때 치워진다.
			SwingUtilities.invokeLater(this::openingPick);
		}

		clock.start();
	}

	// 네트워크 대전

	/** 상대 보드는 받아 온 화면을 비추기만 하므로 멈춰 둔다. */
	private void startNetworkPlay() {
		rivalBoard.setPaused(true);

		connection.setListener(this::onMessage);
		new Timer(SNAPSHOT_MS, e -> sendSnapshot()).start();
	}

	private void sendSnapshot() {
		Tetrominoes[] cells = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
		int[] colors = new int[cells.length];
		GameBoard game = playerBoard.getGame();
		game.writeVisible(cells, colors);
		connection.send(new BoardStateMessage(cells, colors,
				game.getLinesRemoved(), game.getScore(), game.isGameOver(), hands[0].clone(),
				game.getCombo()));
	}

	private void onMessage(Object message) {
		if (message instanceof BoardStateMessage)
			SwingUtilities.invokeLater(() -> receiveState((BoardStateMessage) message));
		else if (message instanceof ItemMessage)
			SwingUtilities.invokeLater(() -> receiveItem((ItemMessage) message));
	}

	private void receiveState(BoardStateMessage state) {
		rivalBoard.getGame().applyRemoteState(state.cells, state.colors);
		if (state.hand != null)
			hands[1] = state.hand;          // 상대 가방을 오른쪽에 그대로 보여 준다
		rivalCombo = state.combo;
		rivalScore = state.score;
		rivalOver = state.gameOver;
		rivalBoard.repaint();
		repaint();
	}

	private void receiveItem(ItemMessage item) {
		notice(0, "상대 " + item.attack.label + "!", -1);
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

	/** 손패를 고르는 키·사용 키를 연결한다. 칸을 눌러도 골라진다. */
	private void bindItemKeys(int player) {
		for (int i = 0; i < HAND_SIZE; i++) {
			final int slot = i;
			Ui.bind(this, GameKeys.ITEM_SELECT[hand(player)][i], "pick" + player + slot, () -> select(player, slot));
			onClick(itemSlot(ITEM_BOXES[player], slot), () -> select(player, slot));
		}
		Ui.bind(this, GameKeys.ITEM_USE[hand(player)], "use" + player, () -> useItem(player, picked[player]));
	}

	/** 한 화면 2인의 2P 는 방향키 쪽 손이라 오른손 아이템 키를, 나머지는 왼손 키(1 2 3 · F)를 쓴다. */
	private int hand(int player) {
		return local && player == 1 ? GameKeys.RIGHT_HAND : GameKeys.LEFT_HAND;
	}

	/** 아이템 칸에 글자를 잠깐 띄운다. 시간이 지나면 다시 그려서 지운다. */
	private void notice(int player, String text, int slot) {
		notice[player] = text;
		noticeSlot[player] = slot;
		noticeUntil[player] = System.currentTimeMillis() + NOTICE_MS;
		Timer off = new Timer(NOTICE_MS, e -> repaint());
		off.setRepeats(false);
		off.start();
		repaint();
	}

	private boolean noticeOn(int player) {
		return notice[player] != null && System.currentTimeMillis() < noticeUntil[player];
	}

	private void select(int player, int slot) {
		picked[player] = slot;
		repaint();
	}

	/** 가방에 있는 것을 이번 판에 쓸 더미로 옮긴다. 실제로 쓸 때 가방에서 뺀다. */
	private void loadStock() {
		for (Attack item : Attack.values())
			for (int n = Wallet.count(item); n > 0; n--)
				stock.add(item);
	}

	/**
	 * 더미에 남은 것을 손패 빈 자리에 번갈아 나눠 준다.
	 *
	 * 한 개는 한 사람에게만 간다. 그래서 1P 가 쓴다고 2P 손패가 비지 않는다.
	 */
	private void dealFromStock() {
		deal(hands, stock, local ? 2 : 1);
		repaint();
	}

	/** 나눠 주기만 하는 부분. 자가 점검에서도 쓴다. */
	private static void deal(Attack[][] hands, List<Attack> stock, int players) {
		for (int slot = 0; slot < HAND_SIZE; slot++)
			for (int p = 0; p < players; p++)
				if (hands[p][slot] == null && !stock.isEmpty())
					hands[p][slot] = stock.remove(0);
	}

	/** 판을 멈추고 들고 시작할 아이템을 고르게 한다. 한 화면 2인은 1P, 2P 차례로 고른다. */
	private void openingPick() {
		if (stock.isEmpty())
			return;                     // 가방이 비어 있으면 고를 것도 나눠 줄 것도 없다
		pauseBoards();
		pickFor(0);
	}

	private void pickFor(int player) {
		Attack[] choices = stockChoices();
		if (choices.length == 0) {      // 앞 사람이 마지막 하나를 가져갔다
			afterPick(player);
			return;
		}
		frame.showPickItem((player + 1) + "P - 들고 시작할 아이템을 고르세요", choices, item -> {
			stock.remove(item);         // 고른 한 개는 이 사람 것이다
			hands[player][0] = item;
			picked[player] = 0;
			afterPick(player);
		});
	}

	private void afterPick(int player) {
		if (player == 0 && local) {
			pickFor(1);
			return;
		}
		dealFromStock();
		resume();
	}

	/**
	 * 더미에 남아 있는 아이템 종류.
	 *
	 * ponytail: 덮개에 들어가는 앞의 PickOverlay.MAX 종까지만 보여 준다.
	 * 더 많이 가진 사람이 뒤쪽 아이템도 고르고 싶어지면 쪽 넘기기를 붙인다.
	 */
	private Attack[] stockChoices() {
		List<Attack> kinds = new ArrayList<>();
		for (Attack item : stock)
			if (!kinds.contains(item) && kinds.size() < PickOverlay.MAX)
				kinds.add(item);
		return kinds.toArray(new Attack[0]);
	}

	/** 점수 보상으로 받는 아이템. 가방에 넣고 손패의 빈 자리에도 올려 둔다. */
	private void addItem(int player) {
		Attack item = Wallet.add(Attack.random(random));
		for (int i = 0; i < HAND_SIZE; i++)
			if (hands[player][i] == null) {
				hands[player][i] = item;
				notice(player, "+ " + item.label, i);
				return;
			}
		notice(player, "가방으로: " + item.label, -1);   // 손패가 차서 가방에만 들어갔다
	}

	/** 고른 자리의 아이템을 쓴다. 가방에 없으면 자리만 비운다. */
	private void useItem(int player, int slot) {
		Attack attack = hands[player][slot];
		if (finished || attack == null)
			return;

		hands[player][slot] = null;
		// 손패의 한 개는 한 사람 것이라 가방에 반드시 남아 있다. 그래도 없으면 그냥 넘어간다.
		if (!Wallet.use(attack)) {
			repaint();
			return;
		}
		Sfx.play(Sfx.Sound.ITEM);
		notice(player, attack.label + " 사용!", slot);

		if (attack.self)
			board(player).getGame().getGimmick().apply(attack);
		else if (connection != null)
			sendItem(attack);
		else
			hitRival(player, attack);
		repaint();
	}

	private void sendItem(Attack attack) {
		if (attack == Attack.SWAP) {
			// 내 블록을 보내기만 하고, 상대가 쥔 것은 답장으로 받는다. 받아 둔 화면에서
			// 꺼내 쓰면 최대 SNAPSHOT_MS 만큼 옛날 것이라 교환이 어긋난다.
			connection.send(new ItemMessage(Attack.SWAP, playerBoard.getGame().currentType(), false));
		} else {
			connection.send(new ItemMessage(attack));
		}
	}

	/** 한 화면 2인: 상대 보드에 바로 건다. 주고받을 것이 없으므로 교환도 그 자리에서 한다. */
	private void hitRival(int player, Attack attack) {
		GameBoard mine = board(player).getGame();
		GameBoard theirs = board(1 - player).getGame();

		if (attack == Attack.SWAP) {
			Tetrominoes keep = theirs.currentType();
			if (theirs.replaceCurrentPiece(mine.currentType()))
				mine.replaceCurrentPiece(keep);
		} else {
			theirs.getGimmick().apply(attack);
		}
		notice(1 - player, attack.label + " 공격 받음!", -1);
		board(1 - player).repaint();
	}

	private Board board(int player) {
		return player == 0 ? playerBoard : rivalBoard;
	}

	/** 그 손패를 이 화면에서 조종하는지. (네트워크 대전의 오른쪽은 비추기만 한다) */
	private boolean ours(int player) {
		return player == 0 || local;
	}

	// 진행

	/**
	 * [피버] 테트리스(4줄)나 콤보로 터진다. 판을 멈추고 터뜨린 쪽이 블록을 고르면,
	 * 그 블록을 다음 블록으로 넣고 잠깐 점수가 두 배가 된다.
	 *
	 * 네트워크 대전에서는 상대 판을 멈출 수 없으므로 내 판만 멈춘다. (pauseBoards 와 같다)
	 */
	private void fever(int player) {
		if (finished)
			return;
		pauseBoards();
		frame.showPick((player + 1) + "P FEVER!  원하는 블록을 고르세요", shape -> {
			board(player).startFever(shape);
			resume();
		});
	}

	/** 톱니(또는 Esc)를 누르면 두 판을 멈추고 Figma "리플레이 화면" 을 덮는다. */
	private void pause() {
		if (!playerBoard.isRunning())
			return;
		pauseBoards();
		frame.showPause(kind, this::resume);
	}

	/** 두 판과 시계를 멈춘다. 덮개를 띄우기 전에 부른다. */
	private void pauseBoards() {
		clock.stop();
		playerBoard.setPaused(true);
		if (connection == null)
			rivalBoard.setPaused(true);
	}

	/** 뒤로: 판을 멈추고 정말 나갈지 묻는다. */
	private void askLeave() {
		if (!isEnabled())
			return;          // 덮개가 떠 있다. (마우스는 덮개를 지나 이 화면까지 온다)
		pauseBoards();
		frame.showChoice("게임을 그만두고 나갈까요?", LEAVE, STAY, answer -> {
			if (LEAVE.equals(answer))
				frame.showScreen(MainFrame.MULTI);
			else
				resume();
		});
	}

	/** 한 화면 2인은 1P WASD 쪽 / 2P 방향키 쪽으로 나눠 건다. 혼자 조종하면 보조 키·방향키도 1P 것이다. */
	private void bindGameKeys() {
		if (local)
			GameKeys.bindPair(this, playerBoard, rivalBoard);
		else
			GameKeys.bind(this, 0, playerBoard);
	}

	private void resume() {
		bindGameKeys();     // 일시정지 중에 설정을 바꿨을 수 있다
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

		if (useItems) {
			award(0);
			if (local)
				award(1);
		}
		repaint();

		if (remain <= 0 || playerBoard.getGame().isGameOver() || rivalLost())
			finish();
	}

	/** 점수가 ITEM_SCORE 만큼 오를 때마다 아이템을 하나 준다. */
	private void award(int player) {
		if (board(player).score() < nextItemScore[player])
			return;
		nextItemScore[player] += ITEM_SCORE;
		addItem(player);
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
		frame.showVersusResult(sideTitles(title), new int[] { playerBoard.score(), rivalScore() }, coins, kind);
	}

	/** 결과를 보드마다 적는다. 예) 한 화면 2인에서 2P 가 이기면 "1P LOSE" / "2P WIN". */
	private String[] sideTitles(String title) {
		String[] names = { "나", connection != null ? "상대" : "AI" };
		if (local)
			names = new String[] { "1P", "2P" };
		String left = title.startsWith("YOU ") ? title.substring(4) : title;   // WIN / LOSE / DRAW
		String right = left;
		if (left.equals("WIN"))
			right = "LOSE";
		else if (left.equals("LOSE"))
			right = "WIN";
		return new String[] { names[0] + " " + left, names[1] + " " + right };
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

		if (useItems)
			items(g);

		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);
		textCenteredIn(g, "VS", VS_TOP, VS_HEIGHT, BIG_SIZE, Color.WHITE);

		outline(g, BACK);
		drawImageFlipped(g, ARROW_ICON, BACK_ARROW);
		text(g, "뒤로", BACK_TEXT_X, BACK_TEXT_TOP, TAB_SIZE, Ui.CREAM);

		g.setColor(Color.WHITE);
		g.setFont(font(BIG_SIZE));
		// 디자인에는 "!st" / "2st" 로 적혀 있으나 오타라 1st / 2nd 로 쓴다
		g.drawString("1st", px(LABEL_1_X), centeredBaseline(g, LABEL_TOP, LABEL_HEIGHT));
		g.drawString("2nd", px(LABEL_2_X), centeredBaseline(g, LABEL_TOP, LABEL_HEIGHT));

		gear(g);
	}

	/** 플레이어마다 손에 든 아이템. 고른 자리에 흰 테두리를 둘러 준다. */
	private void items(Graphics2D g) {
		for (int player = 0; player < 2; player++)
			itemBox(g, player);
	}

	private void itemBox(Graphics2D g, int player) {
		Rectangle box = ITEM_BOXES[player];
		tab(g, ITEM_TABS[player], (player + 1) + "P Item", TAB_SIZE);
		outline(g, box);

		for (int i = 0; i < HAND_SIZE; i++)
			drawItemSlot(g, player, i, itemSlot(box, i));

		// 상자 가로 가운데가 디자인 화면 가운데(1000)와 같아서 textCentered 로 맞는다.
		itemLabel(g, player, box.y + ITEM_LABEL_DROP);
		textCentered(g, ours(player) ? GameKeys.itemHint(hand(player)) : "상대 가방",
				box.y + ITEM_HINT_DROP, ITEM_HINT_SIZE, DIM_CREAM, 0);
	}

	/** 손패 한 칸. 방금 쓰거나 받은 칸은 반짝이고, 고른 칸은 흰 테두리다. */
	private void drawItemSlot(Graphics2D g, int player, int index, Rectangle slot) {
		if (noticeOn(player) && index == noticeSlot[player])
			fill(g, slot, DIM_CREAM);
		boolean chosen = ours(player) && index == picked[player];
		outline(g, slot, chosen ? Color.WHITE : DIM_CREAM, ITEM_SLOT_BORDER);
		Attack attack = hands[player][index];
		if (attack != null)
			drawImage(g, image(attack.icon), centered(slot, ITEM_ICON));
	}

	/** 칸 아래 한 줄: 알림이 떠 있으면 알림, 아니면 고른 아이템 이름. (상대 가방은 이름을 숨긴다) */
	private void itemLabel(Graphics2D g, int player, int top) {
		if (noticeOn(player)) {
			textCentered(g, notice[player], top, ITEM_SIZE, Ui.CREAM, 0);
			return;
		}
		if (!ours(player))
			return;
		Attack chosen = hands[player][picked[player]];
		if (chosen == null)
			textCentered(g, "-", top, ITEM_SIZE, DIM_CREAM, 0);
		else
			textCentered(g, chosen.label, top, ITEM_SIZE, Ui.CREAM, 0);
	}

	/** 아이템 칸 세 개를 상자 가운데에 한 줄로 늘어놓는다. */
	private static Rectangle itemSlot(Rectangle box, int index) {
		int total = HAND_SIZE * ITEM_SLOT + (HAND_SIZE - 1) * ITEM_SLOT_GAP;
		int x = box.x + (box.width - total) / 2 + index * (ITEM_SLOT + ITEM_SLOT_GAP);
		return new Rectangle(x, box.y + ITEM_SLOT_TOP, ITEM_SLOT, ITEM_SLOT);
	}

	private static Rectangle centered(Rectangle slot, int size) {
		return new Rectangle(slot.x + (slot.width - size) / 2, slot.y + (slot.height - size) / 2,
				size, size);
	}

	/** 한쪽 보드의 레벨과, 그 아래에 켜진 기믹 아이콘과 콤보. */
	private void sideInfo(Graphics2D g, Board board, Rectangle levelBox) {
		valueRight(g, levelBox, String.valueOf(board.getGame().getLevel()), VALUE_SIZE, VALUE_PAD);
		gimmickIcons(g, board.getGame().getGimmick(),
				levelBox.x, levelBox.y + levelBox.height + GIMMICK_DROP, GIMMICK_ICON, GIMMICK_GAP);

		int combo = combo(board);
		if (combo > 0)                      // 두 번째 연속 삭제부터 보여 준다
			text(g, combo + " COMBO", levelBox.x, levelBox.y + levelBox.height + COMBO_DROP,
					COMBO_SIZE, Ui.CREAM);
	}

	/** 상대 보드는 비추기만 하므로 콤보도 받아 온 값을 쓴다. */
	private int combo(Board board) {
		if (board == rivalBoard && connection != null)
			return rivalCombo;
		return board.getGame().getCombo();
	}

	private void previews(Graphics2D g, Board board, Rectangle nextBox, Rectangle holdBox) {
		GameBoard game = board.getGame();
		preview(g, nextBox, game.nextType(), game.getNextColor());
		preview(g, holdBox, game.holdType(), game.getHoldColor());
	}

	/**
	 * 자가 점검. java -ea 로 실행한다.
	 *
	 * 가방의 한 개가 두 사람에게 가지 않는지 본다. (1P 가 쓴다고 2P 손패가 비면 안 된다)
	 */
	public static void main(String[] args) {
		List<Attack> stock = new ArrayList<>();
		stock.add(Attack.BOMB);
		stock.add(Attack.BOMB);
		stock.add(Attack.TINY);
		stock.add(Attack.SPEED);

		Attack[][] hands = new Attack[2][HAND_SIZE];
		hands[0][0] = stock.remove(stock.indexOf(Attack.BOMB));   // 1P 가 시작 아이템으로 고른 것
		deal(hands, stock, 2);

		int[] dealt = new int[Attack.values().length];
		for (Attack[] hand : hands)
			for (Attack item : hand)
				if (item != null)
					dealt[item.ordinal()]++;

		assert dealt[Attack.BOMB.ordinal()] == 2 : "폭탄 2개를 가졌는데 나눠 준 수가 다르다";
		assert dealt[Attack.TINY.ordinal()] == 1 && dealt[Attack.SPEED.ordinal()] == 1
				: "한 개짜리가 두 번 올라가거나 빠졌다";
		assert stock.isEmpty() : "자리가 남는데 안 나눠 준 것이 있다";

		System.out.println("ok");
	}
}
