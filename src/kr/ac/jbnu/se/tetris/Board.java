package kr.ac.jbnu.se.tetris;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
		timer = new Timer(400, this);   // 400ms마다 actionPerformed 호출
		lockTimer.setRepeats(false);
	}

	/** 타이머가 호출. 게임 규칙의 한 틱을 진행한 뒤 화면을 갱신한다. */
	public void actionPerformed(ActionEvent e) {
		if (!isStarted || isPaused) return;
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
		clearLockState();
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
			flashTimer.stop();
			clearLockState();
			isStarted = false;
			statusbar.setText("game over");
		} else {
			statusbar.setText(isPaused ? "paused" : String.valueOf(game.getLinesRemoved()));
			startFlash();
			syncLock();
		}
		repaint();
		if(onUpdate!=null){
			onUpdate.run();
		}
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
			afterAction();
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
					drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape);
			}
		}

		drawGhost(g, boardTop);

		// 2) 현재 떨어지는 블록
		if (game.currentType() != Tetrominoes.NoShape) {
			for (int i = 0; i < 4; ++i) {
				int x = game.pieceX(i);
				int y = game.pieceY(i);
				drawSquare(g, x * squareWidth(), boardTop + (GameBoard.HEIGHT - y - 1) * squareHeight(),
						game.currentType());
			}
		}
		drawFlash(g, boardTop);
	}

	private void drawGhost(Graphics g, int boardTop){
		Tetrominoes type = game.currentType();
		if(type == Tetrominoes.NoShape){
			return;
		}
		int drop = 0;
		while(canDrop(drop+1)){
			drop++;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
		for(int i = 0;i<4;i++){
			int y = game.pieceY(i)-drop;
			drawSquare(g2, game.pieceX(i)*squareWidth(), boardTop+(GameBoard.HEIGHT-y-1)*squareHeight(),type);
		}
		g2.dispose();
	}

	private boolean canDrop(int d){
		for(int i = 0; i<4;i++){
			int y = game.pieceY(i)-d;
			if(y<0||game.shapeAt(game.pieceX(i),y)!=Tetrominoes.NoShape){
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

	// Simple policy: landing again grants a fresh 500 ms.
	// The reset count persists until lock, successful hold, or restart.
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

	private void resetLock(){
		if(lockTimer.isRunning()&&lockResets<MAX_LOCK_RESETS){
			lockResets++;
			lockTimer.restart();
		}
	}

	/** 한 칸(정사각형)을 색상과 입체감을 넣어 그린다. */
	private void drawSquare(Graphics g, int x, int y, Tetrominoes shape) {
		Color colors[] = { new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
				new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204), new Color(102, 204, 204),
				new Color(218, 170, 0), new Color(153, 153, 153) };

		Color color = colors[shape.ordinal()];

		g.setColor(color);
		g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);

		g.setColor(color.brighter());
		g.drawLine(x, y + squareHeight() - 1, x, y);
		g.drawLine(x, y, x + squareWidth() - 1, y);

		g.setColor(color.darker());
		g.drawLine(x + 1, y + squareHeight() - 1, x + squareWidth() - 1, y + squareHeight() - 1);
		g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1, x + squareWidth() - 1, y + 1);
	}
	public GameBoard getGame() {
		return game;
	}

	public boolean isRunning() {
		return isStarted && !isPaused;
	}
}