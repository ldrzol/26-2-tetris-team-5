package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * 게임 하나(GameBoard 하나)를 화면에 보여 주는 패널.
 * 게임 규칙은 GameBoard에 맡기고, 여기서는 그리기, 타이머, 상태바, 일시정지만 처리한다.
 *
 * 키 입력은 이 클래스가 직접 받지 않는다. Tetris가 창 단위로 키를 받아서
 * 아래의 moveLeft(), rotateLeft() 같은 메서드를 모든 보드에 호출해 준다.
 */
public class Board extends JPanel implements ActionListener {

	private final GameBoard game = new GameBoard();  // 게임 규칙 담당
	private final Timer timer;                       // 블록을 주기적으로 떨어뜨리는 타이머
	private final JLabel statusbar;                  // 이 보드 전용 상태바
	private boolean isStarted = false;               // 게임 진행 중 여부
	private boolean isPaused = false;                // 일시정지 여부

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

	// ---- 조작 메서드 (Tetris의 키 바인딩이 호출) ----

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

		// 2) 현재 떨어지는 블록
		if (game.currentType() != Tetrominoes.NoShape) {
			for (int i = 0; i < 4; ++i) {
				int x = game.pieceX(i);
				int y = game.pieceY(i);
				drawSquare(g, x * squareWidth(), boardTop + (GameBoard.HEIGHT - y - 1) * squareHeight(),
						game.currentType());
			}
		}
	}

	/** 한 칸(정사각형)을 색상과 입체감을 넣어 그린다. */
	private void drawSquare(Graphics g, int x, int y, Tetrominoes shape) {
		Color colors[] = { new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
				new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204), new Color(102, 204, 204),
				new Color(218, 170, 0) };

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
}