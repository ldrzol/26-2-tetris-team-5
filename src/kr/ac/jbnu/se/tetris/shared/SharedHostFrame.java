package kr.ac.jbnu.se.tetris.shared;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedHostFrame extends SharedFrame {

    /** 한 판 제한 시간. */
    private static final long TIME_LIMIT_MS = 5 * 60 * 1000L;

    /** 블록이 한 칸 내려오는 간격. */
    private static final int FALL_MS = 400;

    /** [버그 10] 블록이 한 칸 내려오는 동안(FALL_MS) 참가자 입력을 받는 최대 횟수. 키를 꾹 눌러도 이 정도면 충분하다. */
    private static final int MAX_INPUTS_PER_TICK = 10;

    private final transient SharedGame game = new SharedGame();
    private final long endTime = System.currentTimeMillis() + TIME_LIMIT_MS;

    /** [버그 9] 블록을 떨어뜨리는 타이머. 창이 닫히면 멈출 수 있게 필드로 들고 있는다. */
    private final Timer fallTimer = new Timer(FALL_MS, e -> tick());

    /** [버그 10] 이번 칸 동안 받은 참가자 입력 수. 블록이 한 칸 내려올 때마다 0으로 돌아간다. */
    private int inputsThisTick = 0;

    public SharedHostFrame(PeerConnection connection, int fee, Runnable onExit) {
        super(connection, fee, onExit, "땅따먹기 - 방장 (내 블록: 흰 테두리)");
        refresh();
        pack();
        fallTimer.start();
    }

    /** 0.4초마다: 시간이 다 됐으면 끝내고, 블록을 한 칸 내리고, 참가자 입력 수를 다시 센다. */
    private void tick() {
        if (System.currentTimeMillis() >= endTime) {
            game.end();
        }
        inputsThisTick = 0;
        game.tick();
        refresh();
    }

    /** [버그 9] 창이 닫히거나 연결이 끊기면 블록 떨어뜨리기를 멈춘다. */
    @Override
    protected void onClosed() {
        fallTimer.stop();
    }

    private void refresh() {
        view.setState(game);
        if (game.isGameOver() && !settled) {
            settled = true;
            ResultPanel.showShared(this, connection, game.getWinner(), 0, fee, onExit);
        }
        long left = Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        scoreLabel.setText("남은 시간 " + left / 60 + ":" + String.format("%02d", left % 60)
                + "  |  나(P1): " + game.getScore(0) + "  상대(P2): " + game.getScore(1));
        connection.send(new SharedStateMessage(game));
    }

    @Override
    protected void onMessage(Object message) {
        if (!(message instanceof InputMessage)) {
            return;
        }
        InputMessage input = (InputMessage) message;
        SwingUtilities.invokeLater(() -> {
            if (inputsThisTick >= MAX_INPUTS_PER_TICK) {
                return;   // [버그 10] 이번 칸에서 너무 많이 보냈으면 무시한다
            }
            inputsThisTick++;
            applyInput(1, input.action);
            refresh();
        });
    }

    @Override
    protected void onKey(InputMessage.Action action) {
        applyInput(0, action);
        refresh();
    }

    private void applyInput(int p, InputMessage.Action action) {
        switch (action) {
            case LEFT:
                game.moveLeft(p);
                break;
            case RIGHT:
                game.moveRight(p);
                break;
            case ROTATE:
                game.rotateRight(p);
                break;
            case DOWN:
                game.moveDown(p);
                break;
            case DROP:
                game.hardDrop(p);
                break;
        }
    }
}