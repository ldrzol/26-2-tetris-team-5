package kr.ac.jbnu.se.tetris;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * 프로그램의 진입점이 되는 메인 창(JFrame).
 * 게임 보드를 count개 만들어 격자로 배치하고, 키 입력을 모든 보드에 전달한다.
 *
 * 보드가 많을수록 각 보드의 크기가 작아진다. (화면 크기 안에 전부 들어오도록 계산)
 * 사용법: java kr.ac.jbnu.se.tetris.Tetris [보드 개수]   (생략하면 2개)
 */
public class Tetris extends JFrame {

	private static final int BASE_CELL = 20;   // 보드가 1개일 때 한 칸의 최대 픽셀 크기
	private static final int MIN_CELL = 4;     // 한 칸의 최소 픽셀 크기
	private static final int STATUS_HEIGHT = 20; // 상태바 높이 (칸 크기 계산용 추정값)

	private final List<Board> boards = new ArrayList<>();
	private boolean paused = false;            // 전체 일시정지 상태

	public Tetris(int count) {
		if (count < 1)
			count = 1;

		// 격자 크기: 가능한 한 정사각형에 가깝게 (예: 2개 -> 1x2, 4개 -> 2x2, 5개 -> 2x3)
		int cols = (int) Math.ceil(Math.sqrt(count));
		int rows = (int) Math.ceil((double) count / cols);

		// 화면의 90% x 85% 안에 들어오도록 한 칸의 픽셀 크기 결정
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int maxByWidth = (screen.width * 9 / 10 / cols) / GameBoard.WIDTH;
		int maxByHeight = (screen.height * 85 / 100 / rows - STATUS_HEIGHT) / GameBoard.VISIBLE_HEIGHT;
		int cell = Math.max(MIN_CELL, Math.min(BASE_CELL, Math.min(maxByWidth, maxByHeight)));

		JPanel root = new JPanel(new GridLayout(rows, cols));
		for (int i = 0; i < count; i++)
			root.add(makePlayer(cell));
		add(root);

		setupKeys();

		setTitle("Tetris");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();   // 보드들의 preferredSize 합에 맞춰 창 크기 결정
	}

	/** 보드 하나 + 전용 상태바를 묶은 패널을 만들고 게임을 시작한다. */
	private JPanel makePlayer(int cell) {
		JLabel status = new JLabel(" 0");
		Board board = new Board(status);
		board.setPreferredSize(new Dimension(cell * GameBoard.WIDTH, cell * GameBoard.VISIBLE_HEIGHT));
		boards.add(board);

		JPanel p = new JPanel(new BorderLayout());
		p.add(board, BorderLayout.CENTER);
		p.add(status, BorderLayout.SOUTH);

		board.start();
		return p;
	}

	/**
	 * 창 단위 키 바인딩. 포커스가 어느 컴포넌트에 있든 눌린 키가 모든 보드에 전달된다.
	 *  ←/→ : 좌우 이동   ↑ : 반시계 회전   ↓ : 시계 회전
	 *  Space : 즉시 낙하  D : 한 칸 아래로  P : 전체 일시정지/재개
	 */
	private void setupKeys() {
		bind(KeyEvent.VK_LEFT, "left", Board::moveLeft);
		bind(KeyEvent.VK_RIGHT, "right", Board::moveRight);
		bind(KeyEvent.VK_DOWN, "rotateRight", Board::rotateRight);
		bind(KeyEvent.VK_UP, "rotateLeft", Board::rotateLeft);
		bind(KeyEvent.VK_SPACE, "drop", Board::hardDrop);
		bind(KeyEvent.VK_D, "softDrop", Board::softDrop);
		bind(KeyEvent.VK_R, "Restart", Board::start);

		// 일시정지는 보드 하나가 아니라 전체 상태를 한 곳에서 관리한다
		Runnable togglePause = () -> {
			paused = !paused;
			for (Board b : boards)
				b.setPaused(paused);
		};
		bind(KeyEvent.VK_P, "pause", togglePause);
	}

	/** 키를 모든 보드에 적용하는 동작에 연결한다. (Shift가 눌려도 동작) */
	private void bind(int keyCode, String name, Consumer<Board> action) {
		bind(keyCode, name, () -> {
			for (Board b : boards)
				action.accept(b);
		});
	}

	private void bind(int keyCode, String name, Runnable action) {
		InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
		im.put(KeyStroke.getKeyStroke(keyCode, java.awt.event.InputEvent.SHIFT_DOWN_MASK), name);
		getRootPane().getActionMap().put(name, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	public static void main(String[] args) {
		int count = 1;
		if (args.length > 0) {
			try {
				count = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("보드 개수는 정수로 입력하세요. 기본값 2개로 시작합니다.");
			}
		}

		Tetris game = new Tetris(count);
		game.setLocationRelativeTo(null); // 화면 정중앙에 창 배치
		game.setVisible(true);            // 창 표시
	}
}