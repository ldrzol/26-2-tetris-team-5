package kr.ac.jbnu.se.tetris.game;

import kr.ac.jbnu.se.tetris.item.Attack;
import kr.ac.jbnu.se.tetris.item.Skin;
import kr.ac.jbnu.se.tetris.save.Settings;
import kr.ac.jbnu.se.tetris.save.Wallet;

import kr.ac.jbnu.se.tetris.Resources;
import java.awt.AlphaComposite;
import java.awt.Color;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class Board extends JPanel {

	private final GameBoard game = new GameBoard();  // 게임 규칙 담당
	private final JLabel statusbar;                  // 이 보드 전용 상태바
	private boolean isStarted = false;               // 게임 진행 중 여부
	private boolean isPaused = false;   // 일시정지 여부

	// 낙하, 기믹 1초, 숨김 기믹 다시 그리기 타이머
	private final GameClock clock = new GameClock(this::onFallTick, this::onGimmickSecond, this::onVisibilityTick);

	private final LineClearEffect flash;             // [flash] 줄 삭제 깜빡임 효과

	/** 레벨이 올랐을 때 알릴 곳. (화면이 '레벨 클리어' 덮개를 띄운다) */
	private java.util.function.IntConsumer onLevelUp = null;
	private Runnable onFever = null;

	// [소리] 직전 상태와 비교해서 줄 삭제 / 레벨업 / 게임 오버에 효과음을 낸다
	private int lastLines = 0, lastLevel = 1;
	private boolean lastOver = false;

	/** 보드가 다시 그려질 때 같이 갱신할 곳. (Hold/Next 를 그리는 바깥 패널) */
	private Runnable onRepaint = null;

	// [다방향] 그릴 때 돌리는 횟수(90도 단위). 0 이면 보통 테트리스와 같다
	private int turns = 0;

	private final LockDelay lockDelay = new LockDelay(this::onLockTimeout);

	public Board(JLabel statusbar) {
		this.statusbar = statusbar;
		flash = new LineClearEffect(game, () -> isPaused, this::repaint, this::afterAction);   // [flash]
	}

	/** 타이머가 호출. 게임 규칙의 한 틱을 진행한 뒤 화면을 갱신한다. */
	private void onFallTick() {
		if (!isStarted || isPaused) return;
		game.tick();
		afterAction();
	}

	/** [기믹] INVISIBLE 이 첫 1초 뒤 정확히 사라지도록 짧은 간격으로 다시 그린다. 기믹이 켜져 있을 때만 그린다. */
	private void onVisibilityTick() {
		if (isStarted && !isPaused && !game.getGimmick().activeIcons().isEmpty()) repaint();
	}

	/** [기믹] 1초마다 호출. 일시정지 중이거나 게임이 안 돌면 무시한다. */
	private void onGimmickSecond() {
		if (!isStarted || isPaused) return;
		game.getGimmick().onSecond();
		afterAction();   // 칸이 사라지면 락 상태와 화면도 다시 맞춘다
	}

	/** 게임 시작(또는 재시작). */
	public void start() {
		if (isPaused)
			return;

		isStarted = true;
		flash.stop();                // [flash]
		clearLockState();
		lastLines = 0; lastLevel = 1; lastOver = false;   // [소리]
		game.reset();                // reset() 안에서 기믹도 초기화된다
		game.spawn();
		clock.startAll();
		afterAction();
	}

	/** 일시정지 상태를 지정한다. 대전 화면은 두 보드에 같은 값을 넘긴다. */
	public void setPaused(boolean paused) {
		if (!isStarted || isPaused == paused)
			return;

		isPaused = paused;
		if (isPaused) {
			clock.pauseFall();
			lockDelay.stop();
			statusbar.setText("paused");
		} else {
			clock.resumeFall();
			syncLock();
			statusbar.setText(String.valueOf(game.getLinesRemoved()));
		}
		repaint();
	}

	public void moveLeft() {
		if (canControl()) {
			if (game.moveLeft()) {
				resetLock();
				Sfx.play(Sfx.Sound.MOVE);
			}
			afterAction();
		}
	}

	public void moveRight() {
		if (canControl()) {
			if (game.moveRight()) {
				resetLock();
				Sfx.play(Sfx.Sound.MOVE);
			}
			afterAction();
		}
	}

	public void rotateRight() {
		if (canControl()) {
			if (game.rotateRight()) {
				resetLock();
				Sfx.play(Sfx.Sound.ROTATE);
			}
			afterAction();
		}
	}

	public void rotateLeft() {
		if (canControl()) {
			if (game.rotateLeft()) {
				resetLock();
				Sfx.play(Sfx.Sound.ROTATE);
			}
			afterAction();
		}
	}

	public void hardDrop() {
		if (canControl()) {
			clearLockState();
			game.hardDrop();
			Sfx.play(Sfx.Sound.DROP);
			afterAction();
		}
	}

	public void softDrop() {
		if (canControl()) {
			if (game.moveDown())
				game.addScore(1);   // 소프트 드롭: 칸마다 1점 (자동 낙하는 점수 없음)
			afterAction();
		}
	}

	public void hold(){
		if(canControl()){
			if (game.hold()) {
				clearLockState();
			}
			afterAction();
		}
	}

	/** 게임이 끝났거나, 일시정지 중이거나, 조작할 블록이 없으면 입력을 무시한다. */
	private boolean canControl() {
		return isStarted && !isPaused && !game.isGameOver()
				&& !game.isClearing() && game.currentType() != Tetrominoes.NoShape;
	}

	/** 게임 상태를 상태바와 타이머에 반영하고 화면을 다시 그린다. */
	private void afterAction() {
		playStateSounds();
		if (game.isGameOver()) {
			clock.stopAll();
			flash.stop();            // [flash]
			clearLockState();
			isStarted = false;
			statusbar.setText("game over");
		} else {
			applyGimmickState();     // [기믹] 레벨에 맞는 기믹과 낙하 속도 반영
			statusbar.setText(isPaused ? "paused" : String.valueOf(game.getLinesRemoved()));
			flash.startIfNeeded();   // [flash]
			syncLock();
		}
		if (onRepaint != null)
			onRepaint.run();
		repaint();
	}

	/** [소리] 직전 상태와 달라진 것만 골라 효과음을 낸다. */
	private void playStateSounds() {
		if (game.getLinesRemoved() > lastLines) {
			lastLines = game.getLinesRemoved();
			Sfx.play(Sfx.Sound.CLEAR);
		}
		if (game.getLevel() > lastLevel) {
			int cleared = lastLevel;          // 방금 끝낸 레벨
			lastLevel = game.getLevel();
			Sfx.play(Sfx.Sound.LEVEL_UP);
			if (onLevelUp != null)
				onLevelUp.accept(cleared);
		}
		boolean fever = game.takeFever();   // [피버] 등록한 화면이 없으면 그냥 흘려보낸다
		if (fever && onFever != null)
			onFever.run();
		if (game.isGameOver() && !lastOver) {
			lastOver = true;
			Sfx.play(Sfx.Sound.GAME_OVER);
		}
	}

	/** 레벨이 올랐을 때 할 일을 등록한다. (끝낸 레벨 번호를 넘긴다) */
	public void onLevelUp(java.util.function.IntConsumer action) {
		this.onLevelUp = action;
	}

	/** [피버] 피버가 터졌을 때 할 일을 등록한다. 등록하지 않으면 피버가 없는 판이다. */
	public void onFever(Runnable action) {
		this.onFever = action;
	}

	/**
	 * [피버] 고른 블록을 다음 블록으로 넣고 점수 배수를 건다.
	 *
	 * 가방을 쓰는 useItem 과 달리 아이템을 소모하지 않는다.
	 */
	public void startFever(Tetrominoes shape) {
		game.setNext(shape);
		game.getGimmick().apply(Attack.SCORE_X2);
		afterAction();
	}

	/** 레벨 클리어 뒤 이어서 하기. 쌓인 블록만 비우고 점수·레벨은 유지한다. */
	public void clearStack() {
		flash.stop();
		clearLockState();
		game.clearStack();
		afterAction();
	}

	/** 가방에 있으면 아이템을 하나 써서 효과를 낸다. 1인 모드에서는 내 보드에 건다. */
	public boolean useItem(Attack item) {
		if (!isRunning() || game.isGameOver() || !Wallet.use(item))
			return false;

		switch (item) {
			case BOMB:
				game.explode();             // 쌓인 칸 하나를 무작위 모양으로 터뜨린다
				break;
			case SWAP:
				game.replaceCurrentPiece(game.nextType());   // 걸 기믹이 없어 그 자리에서 바꾼다
				break;
			default:
				game.getGimmick().apply(item);
				break;
		}
		Sfx.play(Sfx.Sound.ITEM);
		afterAction();
		return true;
	}

	/** [기믹] 레벨에 맞춰 기믹을 바꾸고, 낙하 간격(timer delay)을 다시 정한다. */
	private void applyGimmickState() {
		Gimmick gm = game.getGimmick();
		gm.applyLevel(game.getLevel());

		int delay = FallSpeed.delayFor(game.getLevel(), gm.speedFactor());
		clock.setFallDelay(delay);
	}

	// 그리드는 칸 크기가 바뀔 때만 한 번 그려 두고, 매 프레임에는 그림 한 장만 찍는다.
	private BufferedImage gridCache;
	private int gridCacheW = -1, gridCacheH = -1;

	private void drawGrid(Graphics g, int boardTop) {
		int sw = squareWidth(), sh = squareHeight();
		int w = GameBoard.WIDTH * sw;
		int h = GameBoard.VISIBLE_HEIGHT * sh;
		if (w <= 0 || h <= 0) return;

		if (gridCache == null || gridCacheW != w || gridCacheH != h) {
			gridCache = new BufferedImage(w + 1, h + 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gc = gridCache.createGraphics();
			gc.setColor(new Color(0xF1, 0xE7, 0xD3, 40));   // 어두운 판 위에 보이도록 연한 크림색
			for (int j = 0; j <= GameBoard.WIDTH; j++)
				gc.drawLine(j * sw, 0, j * sw, h);
			for (int i = 0; i <= GameBoard.VISIBLE_HEIGHT; i++)
				gc.drawLine(0, i * sh, w, i * sh);
			gc.dispose();
			gridCacheW = w;
			gridCacheH = h;
		}
		g.drawImage(gridCache, 0, boardTop, null);
	}

	/**
	 * [다방향] 판을 90도씩 돌려 그린다. 규칙은 그대로고 보이는 방향만 바뀐다.
	 * 0 = 아래로, 1 = 왼쪽으로, 2 = 위로, 3 = 오른쪽으로 떨어져 보인다.
	 */
	public void setTurns(int turns) {
		this.turns = ((turns % 4) + 4) % 4;
		repaint();
	}

	// 칸 크기는 패널의 실제 크기에서 계산하므로, 보드가 작아지면 칸도 같이 작아진다
	// (90도 돌려 그릴 때는 패널의 가로/세로가 뒤바뀐다)
	int squareWidth() {
		return (int) (sideways() ? getSize().getHeight() : getSize().getWidth()) / GameBoard.WIDTH;
	}

	int squareHeight() {
		return (int) (sideways() ? getSize().getWidth() : getSize().getHeight()) / GameBoard.VISIBLE_HEIGHT;
	}

	private boolean sideways() {
		return turns % 2 != 0;
	}

	/** 화면 그리기. 쌓인 블록 -> 고스트 -> 현재 떨어지는 블록 순서로 그린다. */
	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);

		int boardW = GameBoard.WIDTH * squareWidth();
		int boardH = GameBoard.VISIBLE_HEIGHT * squareHeight();

		// 판을 패널 한가운데에 놓는다. [다방향] 이면 거기서 90도씩 돌린다
		Graphics2D g = (Graphics2D) g0.create();
		// 이동량을 정수로 맞춘다. 소수 좌표가 끼면 선이 반 픽셀씩 흔들려 번쩍거린다.
		g.translate(getWidth() / 2, getHeight() / 2);
		if (turns != 0)
			g.rotate(Math.PI / 2 * turns);
		g.translate(-(boardW / 2), -(boardH / 2));
		int boardTop = 0;

		// [기믹] FLIP: 판 영역의 중심을 기준으로 180도 돌려서 그린다
		if (game.getGimmick().flipsBoard())
			g.rotate(Math.PI, boardW / 2.0, boardH / 2.0);   // 이때만 회전 변환을 쓴다

		if (Settings.grid)                 // [설정] 그리드 표시
			drawGrid(g, boardTop);

		// 1) 이미 바닥에 쌓인 블록
		for (int i = 0; i < GameBoard.VISIBLE_HEIGHT; ++i) {
			for (int j = 0; j < GameBoard.WIDTH; ++j) {
				Tetrominoes shape = game.shapeAt(j, GameBoard.VISIBLE_HEIGHT - i - 1);
				if (shape != Tetrominoes.NoShape)
					drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape,
							game.colorAt(j, GameBoard.VISIBLE_HEIGHT - i - 1));
			}
		}

		// [기믹] INVISIBLE 이면 떨어지는 블록과 고스트를 둘 다 그리지 않는다
		boolean invisible = game.getGimmick().hidesPiece();

		if (!invisible && Settings.ghost) {   // [설정] 고스트 표시
			drawGhost(g, boardTop);
		}

		// 2) 현재 떨어지는 블록
		if (!invisible && game.currentType() != Tetrominoes.NoShape) {
			for (int i = 0; i < game.pieceCount(); ++i) {
				int x = game.pieceX(i);
				int y = game.pieceY(i);
				// 숨김 영역(y >= VISIBLE_HEIGHT)의 칸은 위로 벗어나 저절로 잘린다
				drawSquare(g, x * squareWidth(), boardTop + (GameBoard.VISIBLE_HEIGHT - y - 1) * squareHeight(),
						game.currentType(), game.getCurColor());
			}
		}

		flash.draw(g, boardTop, squareWidth(), squareHeight());   // [flash]
		g.dispose();
	}

	private static final Color GARBAGE_COLOR = new Color(153, 153, 153);

	private static final BufferedImage SRC_TILE = loadTile();
	private static final BufferedImage GARBAGE_TILE = tint(SRC_TILE, GARBAGE_COLOR);

	/** 스킨별로 물들인 타일. 한 번 만들어 두고 다시 쓴다. */
	private static final Map<Skin, BufferedImage[]> TILES = new EnumMap<>(Skin.class);

	/** 지금 장착한 스킨의 타일. (흰 부분만 색이 되고 검은 테두리는 남는다) */
	private static BufferedImage[] tiles() {
		return TILES.computeIfAbsent(Wallet.skin(), Board::makeTiles);
	}

	private static BufferedImage loadTile() {
		try {
			return ImageIO.read(Resources.find("blocks/tile.png"));
		} catch (IOException | IllegalArgumentException e) {
			throw new RuntimeException("blocks/tile.png 를 찾을 수 없습니다", e);
		}
	}

	private static BufferedImage[] makeTiles(Skin skin) {
		BufferedImage[] tiles = new BufferedImage[GameBoard.COLOR_COUNT];
		for (int i = 0; i < tiles.length; i++) {
			tiles[i] = tint(SRC_TILE, skin.colors[i]);
		}
		return tiles;
	}

	/** 흰 부분에만 색이 입혀지도록 픽셀을 곱한다. (검은 테두리는 검은 채로 남는다) */
	private static BufferedImage tint(BufferedImage src, Color c) {
		BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < src.getHeight(); y++) {
			for (int x = 0; x < src.getWidth(); x++) {
				int p = src.getRGB(x, y);
				int r = ((p >> 16) & 255) * c.getRed() / 255;
				int g = ((p >> 8) & 255) * c.getGreen() / 255;
				int b = (p & 255) * c.getBlue() / 255;
				out.setRGB(x, y, (p & 0xFF000000) | (r << 16) | (g << 8) | b);
			}
		}
		return out;
	}

	/** tile.png 안에서 테두리가 차지하는 비율 (8px / 176px) */
	private static final double BORDER_RATIO = 8.0 / 176.0;

	/**
	 * 한 칸(정사각형)을 그린다. 쓰레기 블록만 회색 고정, 나머지는 색 번호를 따른다.
	 *
	 * 칸 크기보다 테두리 두께만큼 크게, 그 절반만큼 왼쪽/위로 밀어서 그린다.
	 * 그래야 테두리가 칸 경계선 위에 걸쳐져서, 블록끼리 붙어도 선이 겹쳐 그려질 뿐
	 * 두꺼워지지 않는다.
	 */
	private void drawSquare(Graphics g, int x, int y, Tetrominoes shape, int colorId) {
		BufferedImage tile = (shape == Tetrominoes.GarbageShape) ? GARBAGE_TILE : tiles()[colorId];
		int border = Math.max(1, (int) Math.round(squareWidth() * BORDER_RATIO));
		g.drawImage(tile, x - border / 2, y - border / 2,
				squareWidth() + border, squareHeight() + border, this);
	}
	/**
	 * Hold / Next 상자 안에 블록 모양 하나를 가운데 맞춰 그린다.
	 * 칸 크기는 상자에 맞춰 정하고, 사방에 반 칸씩 여백을 둔다.
	 */
	public static void drawPreview(Graphics g, Tetrominoes shape, int colorId, Rectangle area) {
		if (shape == Tetrominoes.NoShape || shape == Tetrominoes.GarbageShape)
			return;

		Shape piece = new Shape();
		piece.setShape(shape);

		int minX = piece.x(0), maxX = piece.x(0), minY = piece.y(0), maxY = piece.y(0);
		for (int i = 1; i < piece.size(); i++) {
			minX = Math.min(minX, piece.x(i));
			maxX = Math.max(maxX, piece.x(i));
			minY = Math.min(minY, piece.y(i));
			maxY = Math.max(maxY, piece.y(i));
		}
		int cols = maxX - minX + 1, rows = maxY - minY + 1;
		int cell = Math.max(1, Math.min(area.width / (cols + 1), area.height / (rows + 1)));

		int originX = area.x + (area.width - cols * cell) / 2;
		int originY = area.y + (area.height - rows * cell) / 2;

		BufferedImage tile = tiles()[colorId];
		int border = Math.max(1, (int) Math.round(cell * BORDER_RATIO));
		for (int i = 0; i < piece.size(); i++) {
			// 보드와 같은 규칙: y 가 클수록 아래쪽이다
			int x = originX + (piece.x(i) - minX) * cell;
			int y = originY + (piece.y(i) - minY) * cell;
			g.drawImage(tile, x - border / 2, y - border / 2, cell + border, cell + border, null);
		}
	}

	private void drawGhost(Graphics g, int boardTop) {
		Tetrominoes type = game.currentType();
		if (type == Tetrominoes.NoShape) {
			return;
		}
		int drop = 0;
		while (canDrop(drop + 1)) {
			drop++;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
		for (int i = 0; i < game.pieceCount(); i++) {
			int y = game.pieceY(i) - drop;
			drawSquare(g2, game.pieceX(i) * squareWidth(),
					boardTop + (GameBoard.VISIBLE_HEIGHT - y - 1) * squareHeight(),
					type, game.getCurColor());
		}
		g2.dispose();
	}

	private boolean canDrop(int d) {
		for (int i = 0; i < game.pieceCount(); i++) {
			int y = game.pieceY(i) - d;
			if (y < 0 || game.shapeAt(game.pieceX(i), y) != Tetrominoes.NoShape) {
				return false;
			}
		}
		return true;
	}

	private boolean isGrounded() {
		return game.currentType() != Tetrominoes.NoShape && !canDrop(1);
	}

	private void clearLockState() {
		lockDelay.clear();
	}

	private void onLockTimeout() {
		if (!canControl() || !isGrounded()) return;
		clearLockState();
		game.lockPiece();
		Sfx.play(Sfx.Sound.LOCK);
		afterAction();
	}

	/** 바닥에 닿아 있으면 락 타이머를 돌리고, 떠 있으면 멈춘다. */
	private void syncLock() {
		if (!canControl()) {
			lockDelay.stop();
			return;
		}
		if (isGrounded()) {
			lockDelay.start();
		} else {
			lockDelay.stop();
		}
	}

	/** 바닥에서 움직이면 락딜레이를 다시 준다. 단 LockDelay.reset 한도까지만. */
	private void resetLock() {
		lockDelay.reset();
	}

	public void onRepaint(Runnable action) {
		this.onRepaint = action;
	}

	public GameBoard getGame() {
		return game;
	}

	/** 현재 점수. (줄 삭제, 드롭, 콤보, 퍼펙트 클리어는 GameBoard 가 계산한다) */
	public int score() {
		return game.getScore();
	}

	public boolean isRunning() {
		return isStarted && !isPaused;
	}
}