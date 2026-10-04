package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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

	private static final int FLASH_TOTAL = 6;//블럭 제거 이펙트 깜빡이는 횟수, 짝수로 둘 것
	private final Timer flashTimer = new Timer(100, e -> onFlashTick());//깜빡이는 빈도

	private boolean flashOn = false;
	private int flashCount = 0;

	public Board(JLabel statusbar) {
		this.statusbar = statusbar;
		timer = new Timer(400, this);   // 400ms마다 actionPerformed 호출
		timer.start();
	}

	/** 타이머가 호출. 게임 규칙의 한 틱을 진행한 뒤 화면을 갱신한다. */
	public void actionPerformed(ActionEvent e) {
		game.tick();
		afterAction();
	}

	/** 게임 시작(또는 재시작). */
	public void start() {
		if (isPaused)
			return;

		isStarted = true;
		flashTimer.stop();
		flashOn = false;
		game.reset();
		game.spawn();
		timer.start();
		afterAction();
	}

	/** 일시정지 상태를 지정한다. (Tetris가 모든 보드에 같은 값을 전달한다) */
	public void setPaused(boolean paused) {
		if (!isStarted || isPaused == paused)
			return;

		isPaused = paused;
		if (isPaused) {
			timer.stop();
			statusbar.setText("paused");
		} else {
			timer.start();
			statusbar.setText(String.valueOf(game.getLinesRemoved()));
		}
		repaint();
	}


	public void moveLeft() {
		if (canControl()) {
			game.moveLeft();
			afterAction();
		}
	}

	public void moveRight() {
		if (canControl()) {
			game.moveRight();
			afterAction();
		}
	}

	public void rotateRight() {
		if (canControl()) {
			game.rotateRight();
			afterAction();
		}
	}

	public void rotateLeft() {
		if (canControl()) {
			game.rotateLeft();
			afterAction();
		}
	}

	public void hardDrop() {
		if (canControl()) {
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

	/** 게임이 끝났거나, 일시정지 중이거나, 조작할 블록이 없으면 입력을 무시한다. */
	private boolean canControl() {
		return isStarted && !isPaused && game.currentType() != Tetrominoes.NoShape;
	}

	/** 게임 상태를 상태바와 타이머에 반영하고 화면을 다시 그린다. */
	private void afterAction() {
		if (game.isGameOver()) {
			timer.stop();
			isStarted = false;
			statusbar.setText("game over");
		} else {
			statusbar.setText(String.valueOf(game.getLinesRemoved()));
			startFlash();
		}
		repaint();
	}

	private void startFlash(){
		if(game.isClearing() && !flashTimer.isRunning()){
			flashCount = 0;
			flashOn = false;
			flashTimer.start();
		}
	}

	private void drawFlash(Graphics g, int boardTop){
		if(!flashOn){
			return;
		}
		g.setColor(Color.WHITE);
		for(int row = 0;row<GameBoard.HEIGHT;++row){
			if(game.isFlashRow(row)){
				g.fillRect(0, boardTop+(GameBoard.HEIGHT-row-1)*squareHeight(),
						GameBoard.WIDTH*squareWidth(), squareHeight());
			}
		}
	}

	private void onFlashTick(){
		if(isPaused){
			return;
		}
		flashOn=!flashOn;
		flashCount++;

		if(flashCount>=FLASH_TOTAL){
			flashTimer.stop();
			flashOn=false;
			game.finishClear();
			return;
		}
		repaint();
	}

	// 칸 크기는 패널의 실제 크기에서 계산하므로, 보드가 작아지면 칸도 같이 작아진다
	int squareWidth() {
		return (int) getSize().getWidth() / GameBoard.WIDTH;
	}

	int squareHeight() {
		return (int) getSize().getHeight() / GameBoard.HEIGHT;
	}

	/** 화면 그리기. 쌓인 블록 -> 현재 떨어지는 블록 순서로 그린다. */
	public void paint(Graphics g) {
		super.paint(g);

		Dimension size = getSize();
		int boardTop = (int) size.getHeight() - GameBoard.HEIGHT * squareHeight();

		// 1) 이미 바닥에 쌓인 블록
		for (int i = 0; i < GameBoard.HEIGHT; ++i) {
			for (int j = 0; j < GameBoard.WIDTH; ++j) {
				Tetrominoes shape = game.shapeAt(j, GameBoard.HEIGHT - i - 1);
				if (shape != Tetrominoes.NoShape)
					drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape,
							game.colorAt(j, GameBoard.HEIGHT - i - 1));
			}
		}

		// 2) 현재 떨어지는 블록
		if (game.currentType() != Tetrominoes.NoShape) {
			for (int i = 0; i < 4; ++i) {
				int x = game.pieceX(i);
				int y = game.pieceY(i);
				drawSquare(g, x * squareWidth(), boardTop + (GameBoard.HEIGHT - y - 1) * squareHeight(),
						game.currentType(), game.getCurColor());
			}
		}
		drawFlash(g, boardTop);
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

	/** 한 칸(정사각형)을 그린다. 쓰레기 블록만 회색 고정, 나머지는 색 번호를 따른다. */
	private void drawSquare(Graphics g, int x, int y, Tetrominoes shape, int colorId) {
		BufferedImage tile = (shape == Tetrominoes.GarbageShape) ? GARBAGE_TILE : TILES[colorId];
		g.drawImage(tile, x, y, squareWidth(), squareHeight(), this);
	}
	public GameBoard getGame() {
		return game;
	}

	public boolean isRunning() {
		return isStarted && !isPaused;
	}
}