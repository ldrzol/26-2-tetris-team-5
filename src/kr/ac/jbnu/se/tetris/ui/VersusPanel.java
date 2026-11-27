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
import kr.ac.jbnu.se.tetris.network.MatchResult;
import kr.ac.jbnu.se.tetris.network.NetworkMatch;

/**
 * 대전 화면. Figma "리팩 - 2인 모드(두 화면)".
 *
 * 왼쪽이 플레이어, 오른쪽이 상대다. 상대는 셋 중 하나다.
 *   - AI 가 조종하는 보드 (AI 대전)
 *   - 같은 PC 의 두 번째 보드
 *   - 다른 PC 에서 보내온 화면을 그대로 비추는 보드 (네트워크 2인 대전)
 *
 * 아이템은 네트워크 대전과 한 화면 2인 대전에서 쓴다. 손패는 플레이어마다 따로 있고,
 * 상점에서 산 가방(Wallet)에서 올라온다. 자리를 고른 뒤 사용 키를 누르면 가방에서 빠진다.
 */
class VersusPanel extends DesignPanel implements NetworkMatch.Listener {

	private static final int TIME_LIMIT = 300;   // 5분

	/** 손에 들 수 있는 아이템 수. 키 1~3 과 같아야 한다. */
	private static final int HAND_SIZE = 3;

	/** 이만큼 점수를 올릴 때마다 아이템을 하나 받는다. */
	private static final int ITEM_SCORE = 500;

	// Figma "리팩 - 2인 모드(두 화면)" 수치 (디자인 기준 픽셀)
	private static final Rectangle BOARD_1 = new Rectangle(327, 233, 550, 1100);
	private static final Rectangle BOARD_2 = new Rectangle(1123, 233, 550, 1100);

	/** 칠판 점수판: 왼쪽에 점수, 오른쪽 끝에 지운 줄 수 */
	private static final Rectangle[] SCORE_BOX = {
			new Rectangle(410, 38, 384, 121), new Rectangle(1206, 38, 384, 121) };
	private static final Rectangle TIME_BOX = new Rectangle(884, 25, 235, 147);

	private static final Rectangle[] NAME_OVAL = {
			new Rectangle(30, 172, 260, 122), new Rectangle(1712, 194, 260, 122) };
	private static final Rectangle[] NEXT_BOX = {
			new Rectangle(41, 358, 235, 249), new Rectangle(1728, 358, 235, 249) };
	private static final Rectangle[] HOLD_BOX = {
			new Rectangle(41, 678, 235, 249), new Rectangle(1725, 678, 235, 249) };
	private static final Rectangle[] ITEM_BOX = {
			new Rectangle(42, 998, 235, 335), new Rectangle(1728, 998, 235, 335) };

	private static final int VS_TOP = 382;
	private static final float VS_SIZE = 64f, NAME_SIZE = 64f;

	/** 가운데 아이템 키 안내 상자. 한 화면 2인이면 2P 키까지 적느라 길어진다. */
	private static final int KEYS_X = 937, KEYS_TOP = 532, KEYS_W = 129;
	private static final int KEY_ENTRY = 120;

	// ITEM 상자 안: 손패 세 칸과 그 아래 한 줄
	private static final int ITEM_SLOT = 62, ITEM_SLOT_GAP = 8, ITEM_SLOT_DROP = 112;
	private static final int ITEM_ICON = 52, ITEM_LABEL_DROP = 190;
	private static final float ITEM_SIZE = 26f;

	/** 보드 안 오른쪽 위에 켜진 기믹 아이콘, 위 가운데에 콤보를 띄운다. (디자인에 자리가 없어서) */
	private static final int GIMMICK_ICON = 56, GIMMICK_GAP = 8, OVER_PAD = 26;
	private static final float COMBO_SIZE = 44f;

	/** 아이템을 쓰거나 받았을 때 그 사람 아이템 칸에 글자를 띄우고 칸을 반짝이는 시간(ms). */
	private static final int NOTICE_MS = 1500;

	private static final String LEAVE = "나가기";
	private static final String STAY = "계속하기";

	private final MainFrame frame;
	private final String kind;
	private final Board playerBoard = newBoard();
	private final Board rivalBoard = newBoard();

	/** 네트워크 대전일 때만 있다. 통신·결과·정산은 NetworkMatch 가 맡는다. AI·로컬 대전은 null. */
	private final transient NetworkMatch match;

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

	/** 이번 판에 쓴 아이템 수. [0] = 왼쪽, [1] = 오른쪽. (네트워크 상대는 나에게 건 수만 센다) */
	private final int[] itemsUsed = new int[2];

	/** 상대 점수·줄 수. 네트워크 대전에서는 받아 온 값을 쓴다. */
	private int rivalScore = 0;
	private int rivalLines = 0;
	private int rivalCombo = -1;
	private boolean rivalOver = false;
	private boolean finished = false;

	private String timeText = "05:00";
	private int remain = TIME_LIMIT;

	private final Timer clock = new Timer(1000, e -> tick());

	/** [버그 23] AI 대전일 때 오른쪽 판을 두는 AI. 끝나면 멈추려고 저장해 둔다. */
	private transient AIplayer ai;

	/** 아이템 칸 안내 글자와 반짝일 칸. [0] = 왼쪽, [1] = 오른쪽. 칸이 없으면 -1. */
	private final String[] notice = new String[2];
	private final int[] noticeSlot = { -1, -1 };
	private final long[] noticeUntil = new long[2];

	/** AI 대전 또는 같은 PC 대전. */
	VersusPanel(MainFrame frame, String kind, boolean aiRival) {
		this(frame, kind, aiRival, null, false, 0);
	}

	/** 네트워크 2인 대전. 끝나면 참가비를 정산한다. [버그 17] host = 내가 방장인지 (승패는 방장이 정한다) */
	VersusPanel(MainFrame frame, PeerConnection connection, boolean host, int fee) {
		this(frame, MainFrame.LOBBY, false, connection, host, fee);
	}

	private VersusPanel(MainFrame frame, String kind, boolean aiRival,
						PeerConnection connection, boolean host, int fee) {
		this.frame = frame;
		this.kind = kind;
		this.match = connection == null ? null : new NetworkMatch(connection, host, fee, this::snapshot);
		this.local = match == null && !aiRival;
		this.useItems = match != null || local;

		add(playerBoard);
		add(rivalBoard);

		bindGameKeys();
		Ui.bind(this, KeyEvent.VK_ESCAPE, "pause", this::pause);
		Ui.bind(this, KeyEvent.VK_P, "pause2", this::pause);
		onGear(this::pause);
		onBack(this::askLeave);

		playerBoard.onFever(() -> fever(0));
		if (local)
			rivalBoard.onFever(() -> fever(1));

		playerBoard.start();
		rivalBoard.start();

		if (match != null)
			startNetworkPlay();
		else if (aiRival){
			ai = new AIplayer(rivalBoard, frame.aiLevel());   // [버그 23] 멈출 수 있게 저장
		ai.start();}
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

		match.start(TIME_LIMIT, this);   // 받기·끊김 알림·판 보내기·시계는 NetworkMatch 가 (버그 20·21·22)
	}

	/**
	 * [버그 21] 상대가 나갔거나 연결이 끊겼을 때. 판을 멈추고 안내한다.
	 * 정산은 하지 않는다. (끊겼을 때 참가비 규칙은 팀과 정하기 - 땅따먹기와 같게 둠)
	 */
	@Override
	public void onRivalLeft() {
		if (finished)
			return;          // 이미 결과가 나왔으면 할 것이 없다
		finished = true;
		clock.stop();
		match.rivalLeft();   // 상대가 없으니 더 보내지 않는다
		playerBoard.setPaused(true);
		frame.showChoice("상대와 연결이 끊어졌습니다", "2인 모드로", "메인으로",
				answer -> frame.showScreen("메인으로".equals(answer) ? MainFrame.MENU : MainFrame.MULTI));
	}

	/** 지금 내 판을 메시지로 만든다. 보내는 일은 NetworkMatch 가 0.1초마다 한다. */
	private BoardStateMessage snapshot() {
		Tetrominoes[] cells = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
		int[] colors = new int[cells.length];
		GameBoard game = playerBoard.getGame();
		game.writeVisible(cells, colors);
		return new BoardStateMessage(cells, colors,
				game.getLinesRemoved(), game.getScore(), game.isGameOver(), hands[0].clone(),
				game.getCombo());
	}

	/** [버그 17] 방장이 정한 결과가 왔다. NetworkMatch 가 내 쪽에서 본 결과로 뒤집어서 준다. */
	@Override
	public void onResult(MatchResult result) {
		finish(result);
	}

	@Override
	public void onRivalState(BoardStateMessage state) {   // 상대 판이 왔다 (NetworkMatch 가 알려 줌)
		rivalBoard.getGame().applyRemoteState(state.cells, state.colors);
		if (state.hand != null)
			hands[1] = state.hand;          // 상대 가방을 오른쪽에 그대로 보여 준다
		rivalCombo = state.combo;
		rivalScore = state.score;
		rivalLines = state.linesRemoved;
		rivalOver = state.gameOver;
		rivalBoard.repaint();
		repaint();
	}

	@Override
	public void onRivalItem(ItemMessage item) {   // 상대가 아이템을 썼다 (NetworkMatch 가 알려 줌)
		if (!item.reply)
			itemsUsed[1]++;
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
		match.replySwap(took ? mine : Tetrominoes.NoShape);
	}

	// 아이템

	/** 손패를 고르는 키·사용 키를 연결한다. 칸을 눌러도 골라진다. */
	private void bindItemKeys(int player) {
		for (int i = 0; i < HAND_SIZE; i++) {
			final int slot = i;
			Ui.bind(this, GameKeys.ITEM_SELECT[hand(player)][i], "pick" + player + slot, () -> select(player, slot));
			onClick(itemSlot(player, slot), () -> select(player, slot));
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
		itemsUsed[player]++;
		notice(player, attack.label + " 사용!", slot);

		if (attack.self)
			board(player).getGame().getGimmick().apply(attack);
		else if (match != null)
			match.sendItem(attack, playerBoard.getGame().currentType());   // 보내기는 NetworkMatch 가
		else
			hitRival(player, attack);
		repaint();
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
		if (match == null)
			clock.stop();        // [버그 19] 네트워크 대전은 상대가 계속하므로 시간은 멈추지 않는다
		playerBoard.setPaused(true);
		if (match == null)
			rivalBoard.setPaused(true);
	}

	/** 뒤로: 판을 멈추고 정말 나갈지 묻는다. */
	private void askLeave() {
		if (!isEnabled())
			return;          // 덮개가 떠 있다. (마우스는 덮개를 지나 이 화면까지 온다)
		pauseBoards();
		frame.showChoice("게임을 그만두고 나갈까요?", LEAVE, STAY, answer -> {
			if (LEAVE.equals(answer)) {
				if (ai != null)
					ai.stop();     // [버그 23] 나가면 AI도 멈춘다
				if (match != null) {
					finished = true;
					match.leave();   // [버그 21] 연결을 닫으면 상대 쪽에서 "끊겼다"를 알게 된다
				}
				frame.showScreen(MainFrame.MULTI);
			} else {
				resume();
			}
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
		if (match == null)
			rivalBoard.setPaused(false);
		clock.start();
	}

	private Board newBoard() {
		Board b = new Board(new JLabel());   // Board 가 상태바를 요구한다. 화면엔 안 붙인다.
		b.getGame().setLevelProgression(false);
		b.setOpaque(false);                  // 보드 상자의 하늘색이 비쳐야 한다
		b.onRepaint(this::repaint);          // Hold/Next 는 패널이 그리므로 같이 갱신해야 한다
		return b;
	}

	/** [버그 17] 판이 끝났을 때. 네트워크 대전은 방장만 승패를 정하고, 참가자는 방장 결과를 기다린다. */
	private void end() {
		if (match != null && !match.isHost())
			waitForHost();
		else
			finish(result());
	}

	/** [버그 17] 참가자: 내 판이 끝나도 승패는 방장이 정한다. 판을 멈추고 마지막 판을 보낸 뒤 기다린다. */
	private void waitForHost() {
		clock.stop();
		playerBoard.setPaused(true);
		match.stopSending();   // 내 마지막 점수·게임 오버를 방장에게 알리고 보내기를 멈춘다
	}
	private void tick() {
		if (match != null)
			remain = match.secondsLeft();   // [버그 20] 네트워크는 실제 시계로 센다 (NetworkMatch)
		else
			remain--;
		timeText = mmss(remain);

		if (useItems) {
			award(0);
			if (local)
				award(1);
		}
		repaint();

		if (remain <= 0 || playerBoard.getGame().isGameOver() || rivalLost())
			end();   // [버그 17]
	}

	/** 점수가 ITEM_SCORE 만큼 오를 때마다 아이템을 하나 준다. */
	private void award(int player) {
		if (board(player).score() < nextItemScore[player])
			return;
		nextItemScore[player] += ITEM_SCORE;
		addItem(player);
	}

	private boolean rivalLost() {
		return match != null ? rivalOver : rivalBoard.getGame().isGameOver();
	}

	private int rivalScore() {
		return match != null ? rivalScore : rivalBoard.score();
	}

	private void finish(MatchResult result) {   // [버그 17] 결과는 밖에서 정해서 넘긴다
		if (finished)
			return;
		finished = true;
		clock.stop();
		playerBoard.setPaused(true);
		if (match == null)
			rivalBoard.setPaused(true);

		if (ai != null)
			ai.stop();                         // [버그 23] AI도 멈춘다
		// [버그 17·22] 마지막 판 보내기, 결과 보내기, 연결 닫기, 정산은 NetworkMatch 가 한다
		int coins = match != null ? match.finish(result) : 0;
		String rival = match != null ? "상대" : "AI";   // 네트워크면 상대, 아니면 AI
		String[] names = local ? new String[] { "1P", "2P" } : new String[] { "나", rival };
		frame.showVersusResult(names, sideResults(result),
				new int[] { playerBoard.score(), rivalScore() },
				new int[] { playerBoard.getGame().getLinesRemoved(), rivalLines() },
				itemsUsed.clone(), coins, kind);
	}

	/** 결과를 보드마다 적는다. 예) 왼쪽이 지면 "LOSE" / "WIN". */
	private static String[] sideResults(MatchResult result) {
		return new String[] { result.name(), result.opposite().name() };   // 예) "LOSE" / "WIN"
	}

	private int rivalLines() {
		return match != null ? rivalLines : rivalBoard.getGame().getLinesRemoved();
	}

	/** [버그 18] 승패를 글자 대신 enum 으로 정한다. */
	private MatchResult result() {
		boolean playerOver = playerBoard.getGame().isGameOver();
		if (playerOver && !rivalLost())
			return MatchResult.LOSE;
		if (rivalLost() && !playerOver)
			return MatchResult.WIN;
		int mine = playerBoard.score();
		int theirs = rivalScore();
		if (mine > theirs)
			return MatchResult.WIN;
		if (mine < theirs)
			return MatchResult.LOSE;
		return MatchResult.DRAW;
	}
	// 그리기

	@Override
	void layoutDesign() {
		playerBoard.setBounds(boardBounds(BOARD_1));
		rivalBoard.setBounds(boardBounds(BOARD_2));
	}

	@Override
	void paintDesign(Graphics2D g) {
		background(g, SKY);
		boardFrame(g, playerBoard.getBounds(), Ui.BROWN);
		boardFrame(g, rivalBoard.getBounds(), Ui.BROWN);

		int[] scores = { playerBoard.score(), rivalScore() };
		int[] lines = { playerBoard.getGame().getLinesRemoved(), rivalLines() };
		for (int p = 0; p < 2; p++) {
			Rectangle sb = SCORE_BOX[p];
			box(g, sb, 15, Ui.CHALK, Ui.BROWN, 10, 19, Ui.SHADOW);
			text(g, String.format("%,d", scores[p]), sb.x + 22, sb.y + 32, 48, Color.WHITE);
			textRight(g, String.valueOf(lines[p]), sb.x + sb.width - 22, sb.y + 51, 32, Color.WHITE);

			smallOval(g, NAME_OVAL[p], (p + 1) + "P", NAME_SIZE, false);
			awningPanel(g, NEXT_BOX[p], "NEXT");
			awningPanel(g, HOLD_BOX[p], "HOLD");
			if (useItems)                  // AI 대전은 아이템을 쓰지 않는다
				awningPanel(g, ITEM_BOX[p], "ITEM");
		}

		previews(g, playerBoard, 0);
		if (match == null)
			previews(g, rivalBoard, 1);   // 비추기만 할 때는 알 수 없다

		panel(g, TIME_BOX);
		textInBox(g, timeText, new Rectangle(TIME_BOX.x, TIME_BOX.y + 10, TIME_BOX.width, TIME_BOX.height - 20),
				48, Color.BLACK);
		textCentered(g, "VS", VS_TOP, VS_SIZE, Color.BLACK);

		if (useItems) {
			items(g);
			keyGuide(g);
		}

		boardStatus(g, playerBoard, BOARD_1);
		boardStatus(g, rivalBoard, BOARD_2);

		back(g);
		gear(g);
	}

	private void previews(Graphics2D g, Board board, int p) {
		GameBoard game = board.getGame();
		preview(g, below(NEXT_BOX[p], 16), game.nextType(), game.getNextColor());
		preview(g, below(HOLD_BOX[p], 16), game.holdType(), game.getHoldColor());
	}

	/** 가운데 상자: 아이템 고르기 / 사용 키. 디자인의 "Q 사용 / T 바꾸기" 자리다. */
	private void keyGuide(Graphics2D g) {
		int players = local ? 2 : 1;
		Rectangle r = new Rectangle(KEYS_X, KEYS_TOP, KEYS_W, 30 + players * 2 * KEY_ENTRY);
		panel(g, r);
		int top = r.y + 19;
		for (int p = 0; p < players; p++) {
			int[] select = GameKeys.ITEM_SELECT[hand(p)];
			String pick = GameKeys.keyName(select[0]) + GameKeys.keyName(select[1]) + GameKeys.keyName(select[2]);
			String who = local ? (p + 1) + "P " : "";
			keyEntry(g, top, pick, who + "고르기");
			keyEntry(g, top + KEY_ENTRY, GameKeys.keyName(GameKeys.ITEM_USE[hand(p)]), who + "사용");
			top += KEY_ENTRY * 2;
		}
	}

	private void keyEntry(Graphics2D g, int top, String key, String label) {
		Rectangle cap = new Rectangle(KEYS_X + 16, top, 98, 54);
		box(g, cap, 11, new Color(0xF8D257), null, 0, 4, Ui.SHADOW);
		textInBox(g, key, cap, 30, Color.BLACK);
		textAt(g, label, KEYS_X + KEYS_W / 2.0, top + 62, 26, Color.BLACK);
	}

	/** 플레이어마다 손에 든 아이템. 고른 자리에 빨간 테두리를 둘러 준다. */
	private void items(Graphics2D g) {
		for (int player = 0; player < 2; player++) {
			for (int i = 0; i < HAND_SIZE; i++)
				drawItemSlot(g, player, i, itemSlot(player, i));
			Rectangle box = ITEM_BOX[player];
			itemLabel(g, player, box.x + box.width / 2.0, box.y + ITEM_LABEL_DROP);
		}
	}

	/** 손패 한 칸. 방금 쓰거나 받은 칸은 노랗게 반짝이고, 고른 칸은 빨간 테두리다. */
	private void drawItemSlot(Graphics2D g, int player, int index, Rectangle slot) {
		boolean flash = noticeOn(player) && index == noticeSlot[player];
		boolean chosen = ours(player) && index == picked[player];
		box(g, slot, 12, flash ? Ui.YELLOW : Color.WHITE, chosen ? Ui.RED : Ui.DASH, chosen ? 5 : 3, 0, null);
		Attack attack = hands[player][index];
		if (attack != null)
			drawImage(g, image(attack.icon), centered(slot, ITEM_ICON));
	}

	/** 칸 아래 한 줄: 알림이 떠 있으면 알림, 아니면 고른 아이템 이름. (상대 가방은 이름을 숨긴다) */
	private void itemLabel(Graphics2D g, int player, double centerX, int top) {
		if (noticeOn(player)) {
			textAt(g, notice[player], centerX, top, ITEM_SIZE, Ui.RED);
			return;
		}
		if (!ours(player)) {
			textAt(g, "상대 가방", centerX, top, ITEM_SIZE, Ui.SUB_TEXT);
			return;
		}
		Attack chosen = hands[player][picked[player]];
		textAt(g, chosen == null ? "-" : chosen.label, centerX, top, ITEM_SIZE,
				chosen == null ? Ui.SUB_TEXT : Ui.TEXT);
	}

	/** 아이템 칸 세 개를 ITEM 상자 가운데에 한 줄로 늘어놓는다. */
	private static Rectangle itemSlot(int player, int index) {
		Rectangle box = ITEM_BOX[player];
		int total = HAND_SIZE * ITEM_SLOT + (HAND_SIZE - 1) * ITEM_SLOT_GAP;
		int x = box.x + (box.width - total) / 2 + index * (ITEM_SLOT + ITEM_SLOT_GAP);
		return new Rectangle(x, box.y + ITEM_SLOT_DROP, ITEM_SLOT, ITEM_SLOT);
	}

	private static Rectangle centered(Rectangle slot, int size) {
		return new Rectangle(slot.x + (slot.width - size) / 2, slot.y + (slot.height - size) / 2,
				size, size);
	}

	/** 보드 위에 겹쳐 그리는 상태: 오른쪽 위 기믹 아이콘, 위 가운데 콤보. */
	private void boardStatus(Graphics2D g, Board board, Rectangle box) {
		int n = board.getGame().getGimmick().activeIcons().size();
		int x = box.x + box.width - OVER_PAD - n * GIMMICK_ICON - Math.max(0, n - 1) * GIMMICK_GAP;
		gimmickIcons(g, board.getGame().getGimmick(), x, box.y + OVER_PAD, GIMMICK_ICON, GIMMICK_GAP);

		int combo = combo(board);
		if (combo > 0)                      // 두 번째 연속 삭제부터 보여 준다
			textAt(g, combo + " COMBO", box.x + box.width / 2.0, box.y + OVER_PAD, COMBO_SIZE, Ui.RED);
	}

	/** 상대 보드는 비추기만 하므로 콤보도 받아 온 값을 쓴다. */
	private int combo(Board board) {
		if (board == rivalBoard && match != null)
			return rivalCombo;
		return board.getGame().getCombo();
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
