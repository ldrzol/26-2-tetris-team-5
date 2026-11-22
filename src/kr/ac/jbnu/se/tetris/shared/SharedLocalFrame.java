package kr.ac.jbnu.se.tetris.shared;

import java.awt.event.KeyEvent;

import javax.swing.Timer;

public class SharedLocalFrame extends AbstractSharedFrame {

    /** 블록이 한 칸 내려오는 간격. */
    private static final int FALL_MS = 400;
    private final SharedGame game = new SharedGame();

    public SharedLocalFrame() {
        super("땅따먹기 (한 PC 테스트)");
        setupKeys();
        refresh();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();

        new Timer(FALL_MS, e -> {
            game.tick();
            refresh();
        }).start();
    }

    private void refresh() {
        view.setState(game);
        scoreLabel.setText("P1: " + game.getScore(0) + "    P2: " + game.getScore(1));
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "p1left", () -> game.moveLeft(0));
        bind(KeyEvent.VK_RIGHT, "p1right", () -> game.moveRight(0));
        bind(KeyEvent.VK_UP, "p1rotate", () -> game.rotateRight(0));
        bind(KeyEvent.VK_DOWN, "p1down", () -> game.moveDown(0));
        bind(KeyEvent.VK_SPACE, "p1drop", () -> game.hardDrop(0));

        bind(KeyEvent.VK_A, "p2left", () -> game.moveLeft(1));
        bind(KeyEvent.VK_D, "p2right", () -> game.moveRight(1));
        bind(KeyEvent.VK_W, "p2rotate", () -> game.rotateRight(1));
        bind(KeyEvent.VK_S, "p2down", () -> game.moveDown(1));
        bind(KeyEvent.VK_Q, "p2drop", () -> game.hardDrop(1));
    }

    @Override
    protected void afterKey() {
        refresh();
    }

    public static void main(String[] args) {
        SharedLocalFrame frame = new SharedLocalFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}