package kr.ac.jbnu.se.tetris;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Board extends JPanel implements ActionListener {

	private final GameBoard game = new GameBoard();  // 게임 규칙 담당
	private final Timer timer;                       // 블록을 주기적으로 떨어뜨리는 타이머
	private final JLabel statusbar;                  // 이 보드 전용 상태바
	private boolean isStarted = false;               // 게임 진행 중 여부
	private boolean isPaused = false;// 일시정지 여부

	private static final int SCORE_PER_LINE = 100;

	// [기믹] 낙하 속도: 레벨이 오를수록 간격이 줄고, SPEED_UP 기믹이 켜지면 추가로 빨라진다
	private static final int BASE_DELAY = 400;
	private static final int MIN_DELAY = 100;
	private static final int DELAY_STEP_PER_LEVEL = 30;

	// [기믹] 1초마다 기믹(블록 사라짐 등)을 갱신하는 타이머
	private final Timer gimmickTimer = new Timer(1000, e -> onGimmickSecond());

	private final LineClearEffect flash;             // [flash] 줄 삭제 깜빡임 효과

	/** 보드가 다시 그려질 때 같이 갱신할 곳. (Hold/Next 를 그리는 바깥 패널) */
	private Runnable onRepaint = null;

	private Runnable onUpdate;

	private static final int LOCK_DELAY = 500;
	private static final int MAX_LOCK_RESETS = 15;
	private final Timer lockTimer = new Timer(LOCK_DELAY, e -> onLockTimeout());
	private int lockResets = 0;

	public void setOnUpdate(Runnable onUpdate){
		this.onUpdate = onUpdate;
	}



	public Board(JLabel statusbar) {
		this.statusbar = statusbar;
		timer = new Timer(BASE_DELAY, this);   // 틱 간격은 afterAction()에서 레벨에 맞춰 조정
		lockTimer.setRepeats(false);
		flash = new LineClearEffect(game, () -> isPaused, this::repaint, this::afterAction);   // [flash]
	}

	/** 타이머가 호출. 게임 규칙의 한 틱을 진행한 뒤 화면을 갱신한다. */
	public void actionPerformed(ActionEvent e) {
		if (!isStarted || isPaused) return;
		game.tick();
		afterAction();
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
		game.reset();                // reset() 안에서 기믹도 초기화된다
		game.spawn();
		timer.start();
		gimmickTimer.restart();      // [기믹]
		afterAction();
	}

	/** 일시정지 상태를 지정한다. (Tetris가 모든 보드에 같은 값을 전달한다) */
	public void setPaused(boolean paused) {
		if (!isStarted || isPaused == paused)
			return;

		isPaused = paused;
		if (isPaused) {
			timer.stop();
			lockTimer.stop();
			statusbar.setText("paused");
		} else {
			timer.start();
			syncLock();
			statusbar.setText(String.valueOf(game.getLinesRemoved()));
		}
		repaint();
	}


	public void moveLeft() {
		if (canControl()) {
			if (game.moveLeft()) {
				resetLock();
			}
			afterAction();
		}
	}

	public void moveRight() {
		if (canControl()) {
			if (game.moveRight()) {
				resetLock();
			}
			afterAction();
		}
	}

	public void rotateRight() {
		if (canControl()) {
			if (game.rotateRight()) {
				resetLock();
			}
			afterAction();
		}
	}

	public void rotateLeft() {
		if (canControl()) {
			if (game.rotateLeft()) {
				resetLock();
			}
			afterAction();
		}
	}

	public void hardDrop() {
		if (canControl()) {
			clearLockState();
			game.hardDrop();
			afterAction();
		}
	}

	public void softDrop() {
		if (canControl()) {
			game.moveDown();
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
		if (game.isGameOver()) {
			timer.stop();
			gimmickTimer.stop();     // [기믹]
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
		if(onUpdate!=null){
			onUpdate.run();
		}
	}

	/** [기믹] 레벨에 맞춰 기믹을 바꾸고, 낙하 간격(timer delay)을 다시 정한다. */
	/** 레벨에 맞춰 기믹을 바꾸고, 낙하 간격(timer delay)을 다시 정한다. */
	private void applyGimmickState() {
		Gimmick gm = game.getGimmick();
		gm.applyLevel(game.getLevel());

		int delay = Math.max(MIN_DELAY, BASE_DELAY - (game.getLevel() - 1) * DELAY_STEP_PER_LEVEL);
		delay = Math.max(MIN_DELAY / 2, (int) (delay * gm.speedFactor()));
		if (timer.getDelay() != delay) {
			timer.setDelay(delay);
		}
	}


	private void drawGrid(Graphics g, int boardTop) {
		int w = GameBoard.WIDTH * squareWidth();
		int h = GameBoard.HEIGHT * squareHeight();

		g.setColor(new Color(0, 0, 0, 28));   // 연한 반투명 검정
		for (int j = 0; j <= GameBoard.WIDTH; j++) {
			int x = j * squareWidth();
			g.drawLine(x, boardTop, x, boardTop + h);
		}
		for (int i = 0; i <= GameBoard.HEIGHT; i++) {
			int y = boardTop + i * squareHeight();
			g.drawLine(0, y, w, y);
		}
	}

	// 칸 크기는 패널의 실제 크기에서 계산하므로, 보드가 작아지면 칸도 같이 작아진다
	int squareWidth() {
		return (int) getSize().getWidth() / GameBoard.WIDTH;
	}

	int squareHeight() {
		return (int) getSize().getHeight() / GameBoard.VISIBLE_HEIGHT;
	}

	/** 화면 그리기. 쌓인 블록 -> 고스트 -> 현재 떨어지는 블록 순서로 그린다. */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Dimension size = getSize();
		int boardTop = (int) size.getHeight() - GameBoard.VISIBLE_HEIGHT * squareHeight();

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


		if (!invisible) {
			drawGhost(g, boardTop);
		}

		// 2) 현재 떨어지는 블록
		if (!invisible && game.currentType() != Tetrominoes.NoShape) {
			for (int i = 0; i < 4; ++i) {
				int x = game.pieceX(i);
				int y = game.pieceY(i);
				// 숨김 영역(y >= VISIBLE_HEIGHT)의 칸은 위로 벗어나 저절로 잘린다
				drawSquare(g, x * squareWidth(), boardTop + (GameBoard.VISIBLE_HEIGHT - y - 1) * squareHeight(),
						game.currentType(), game.getCurColor());
			}
		}
		flash.draw(g, boardTop, squareWidth(), squareHeight());   // [flash]
	}

	/** 블록이 고를 수 있는 색. 원래 테트로미노 7색 그대로다. */
	private static final Color[] PALETTE = { new Color(204, 102, 102), new Color(102, 204, 102),
			new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204), new Color(102, 204, 204),
			new Color(218, 170, 0) };

	private static final Color GARBAGE_COLOR = new Color(153, 153, 153);

	private static final BufferedImage SRC_TILE = loadTile();

	/** tile.png 를 색 번호별로 미리 물들여 둔다. (흰 부분만 색이 되고 검은 테두리는 남는다) */
	private static final BufferedImage[] TILES = makeTiles();
	private static final BufferedImage GARBAGE_TILE = tint(SRC_TILE, GARBAGE_COLOR);

	private static BufferedImage loadTile() {
		try {
			return ImageIO.read(Board.class.getResource("blocks/tile.png"));
		} catch (IOException | IllegalArgumentException e) {
			throw new RuntimeException("blocks/tile.png 를 찾을 수 없습니다", e);
		}
	}

	private static BufferedImage[] makeTiles() {
		BufferedImage[] tiles = new BufferedImage[GameBoard.COLOR_COUNT];
		for (int i = 0; i < tiles.length; i++) {
			tiles[i] = tint(SRC_TILE, PALETTE[i]);
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
		BufferedImage tile = (shape == Tetrominoes.GarbageShape) ? GARBAGE_TILE : TILES[colorId];
		int border = Math.max(1, (int) Math.round(squareWidth() * BORDER_RATIO));
		g.drawImage(tile, x - border / 2, y - border / 2,
				squareWidth() + border, squareHeight() + border, this);
	}
	/**
	 * Hold / Next 상자 안에 블록 모양 하나를 가운데 맞춰 그린다.
	 * 칸 크기는 상자에 맞춰 정하고, 사방에 반 칸씩 여백을 둔다.
	 */
	static void drawPreview(Graphics g, Tetrominoes shape, int colorId, Rectangle area) {
		if (shape == Tetrominoes.NoShape || shape == Tetrominoes.GarbageShape)
			return;

		Shape piece = new Shape();
		piece.setShape(shape);

		int minX = piece.x(0), maxX = piece.x(0), minY = piece.y(0), maxY = piece.y(0);
		for (int i = 1; i < 4; i++) {
			minX = Math.min(minX, piece.x(i));
			maxX = Math.max(maxX, piece.x(i));
			minY = Math.min(minY, piece.y(i));
			maxY = Math.max(maxY, piece.y(i));
		}
		int cols = maxX - minX + 1, rows = maxY - minY + 1;
		int cell = Math.max(1, Math.min(area.width / (cols + 1), area.height / (rows + 1)));

		int originX = area.x + (area.width - cols * cell) / 2;
		int originY = area.y + (area.height - rows * cell) / 2;

		BufferedImage tile = TILES[colorId];
		int border = Math.max(1, (int) Math.round(cell * BORDER_RATIO));
		for (int i = 0; i < 4; i++) {
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
		for (int i = 0; i < 4; i++) {
			int y = game.pieceY(i) - drop;
			drawSquare(g2, game.pieceX(i) * squareWidth(),
					boardTop + (GameBoard.VISIBLE_HEIGHT - y - 1) * squareHeight(),
					type, game.getCurColor());
		}
		g2.dispose();
	}

	private boolean canDrop(int d) {
		for (int i = 0; i < 4; i++) {
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
		lockTimer.stop();
		lockResets = 0;
	}

	private void onLockTimeout() {
		if (!canControl() || !isGrounded()) return;
		clearLockState();
		game.lockPiece();
		afterAction();
	}

	/** 바닥에 닿아 있으면 락 타이머를 돌리고, 떠 있으면 멈춘다. */
	private void syncLock() {
		if (!canControl()) {
			lockTimer.stop();
			return;
		}
		if (isGrounded()) {
			if (!lockTimer.isRunning()) lockTimer.start();
		} else {
			lockTimer.stop();
		}
	}

	/** 바닥에서 움직이면 락딜레이를 다시 준다. 단 MAX_LOCK_RESETS 번까지만. */
	private void resetLock() {
		if (lockTimer.isRunning() && lockResets < MAX_LOCK_RESETS) {
			lockResets++;
			lockTimer.restart();
		}
	}

	void onRepaint(Runnable action) {
		this.onRepaint = action;
	}

	public GameBoard getGame() {
		return game;
	}

	/** 지운 줄로 매기는 점수. 화면들이 같은 식을 쓰므로 여기 한 곳에 둔다. */
	public int score() {
		return game.getLinesRemoved() * SCORE_PER_LINE;
	}

	public boolean isRunning() {
		return isStarted && !isPaused;
	}
}
