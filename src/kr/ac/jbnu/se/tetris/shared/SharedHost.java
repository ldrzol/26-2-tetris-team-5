package kr.ac.jbnu.se.tetris.shared;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

/**
 * 땅따먹기 방장. 게임 규칙(SharedGame)은 방장만 돌리고, 판 상태를 참가자에게 보낸다.
 *
 * 시작 순서 [버그 14]:
 * 1) 화면이 열려도 바로 시작하지 않고 "상대가 준비하기를 기다리는 중..."을 띄운다.
 * 2) 참가자 화면이 열리면 참가자가 READY 를 보낸다.
 * 3) READY 를 받으면 3, 2, 1 카운트다운을 양쪽 화면에 띄운다.
 * 4) 카운트다운이 끝나면 블록이 떨어지기 시작하고 5분 시계가 시작된다.
 */
class SharedHost extends SharedSession {

    /** 한 판 제한 시간. */
    private static final long TIME_LIMIT_MS = 5 * 60 * 1000L;

    /** 블록이 한 칸 내려오는 간격. */
    private static final int FALL_MS = 400;

    /** [버그 10] 블록이 한 칸 내려오는 동안(FALL_MS) 참가자 입력을 받는 최대 횟수. 키를 꾹 눌러도 이 정도면 충분하다. */
    private static final int MAX_INPUTS_PER_TICK = 10;

    /** [버그 14] 시작 전 카운트다운 초. */
    private static final int COUNTDOWN_SEC = 3;

    private final SharedGame game = new SharedGame();

    /**
     * [버그 14] 제한 시간이 끝나는 시각. 카운트다운이 끝나 실제로 시작할 때 정한다.
     * (예전에는 화면이 열리는 순간 정해서, 참가자가 늦게 들어오면 그만큼 시간이 줄어 있었다)
     */
    private long endTime;

    /** [버그 14] 카운트다운이 끝나 게임이 시작됐는지. 시작 전 입력은 무시한다. */
    private boolean started = false;

    /** [버그 14] 남은 카운트다운 초. */
    private int countdown = COUNTDOWN_SEC;

    /** [버그 9] 블록을 떨어뜨리는 타이머. 창이 닫히면 멈출 수 있게 필드로 들고 있는다. */
    private final Timer fallTimer = new Timer(FALL_MS, e -> tick());

    /** [버그 14] 1초마다 카운트다운을 하나씩 줄이는 타이머. */
    private final Timer countdownTimer = new Timer(1000, e -> countDown());

    /** [버그 10] 이번 칸 동안 받은 참가자 입력 수. 블록이 한 칸 내려올 때마다 0으로 돌아간다. */
    private int inputsThisTick = 0;

    SharedHost(PeerConnection connection, int fee, View view) {
        super(connection, fee, view);
        view.showState(game);
        view.showStatus("상대가 준비하기를 기다리는 중...");
        // [버그 14] 예전에는 여기서 바로 fallTimer.start() 로 시작했다.
        // 이제는 참가자가 READY 를 보내면 카운트다운 뒤에 시작한다.
    }

    /** [버그 14] 참가자가 준비되면 카운트다운을 시작한다. 두 번 시작하지 않게 막는다. */
    private void startCountdown() {
        if (started || countdownTimer.isRunning()) {
            return;
        }
        showCountdown();
        countdownTimer.start();
    }

    /** [버그 14] 1초마다 카운트다운을 줄이고, 0이 되면 게임을 시작한다. */
    private void countDown() {
        countdown--;
        if (countdown > 0) {
            showCountdown();
            return;
        }
        countdownTimer.stop();
        started = true;
        endTime = System.currentTimeMillis() + TIME_LIMIT_MS;   // 5분은 지금부터 센다
        refresh();
        fallTimer.start();
    }

    /** [버그 14] 양쪽 화면에 남은 초를 띄운다. 판 상태를 먼저 보내고 그다음 카운트를 보낸다 (참가자 글자가 덮이지 않게). */
    private void showCountdown() {
        view.showStatus(countdown + "초 뒤에 시작합니다");
        connection.send(new SharedStateMessage(game));
        connection.send(COUNT + countdown);
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

    /** [버그 9] 화면을 떠나거나 연결이 끊기면 타이머를 모두 멈춘다. */
    @Override
    protected void onClosed() {
        fallTimer.stop();
        countdownTimer.stop();   // [버그 14] 카운트다운 중에 닫혀도 멈춘다
    }

    /** 화면을 다시 그리고, 끝났으면 결과를 띄우고, 판 상태를 참가자에게 보낸다. */
    private void refresh() {
        view.showState(game);
        long left = Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        view.showStatus("남은 시간 " + left / 60 + ":" + String.format("%02d", left % 60));
        connection.send(new SharedStateMessage(game));
        finish(game, 0);
    }

    /** 참가자에게서 받은 메시지: 준비 신호(READY) 또는 키 입력(InputMessage). */
    @Override
    protected void onMessage(Object message) {
        if (READY.equals(message)) {
            SwingUtilities.invokeLater(this::startCountdown);   // [버그 14] 참가자 준비 완료
            return;
        }
        if (!(message instanceof InputMessage)) {
            return;
        }
        InputMessage input = (InputMessage) message;
        SwingUtilities.invokeLater(() -> {
            if (!started || inputsThisTick >= MAX_INPUTS_PER_TICK) {
                return;   // [버그 14] 시작 전이거나 [버그 10] 이번 칸에서 너무 많이 보냈으면 무시한다
            }
            inputsThisTick++;
            applyInput(1, input.action);
            refresh();
        });
    }

    /** 방장이 직접 누른 키. */
    @Override
    public void onKey(InputMessage.Action action) {
        if (!started) {
            return;   // [버그 14] 시작 전에는 움직이지 않는다
        }
        applyInput(0, action);
        refresh();
    }

    /** p번 사람(0 = 방장, 1 = 참가자)의 블록을 움직인다. */
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
            case ROTATE_LEFT:
                game.rotateLeft(p);
                break;
            case DOWN:
                game.moveDown(p);
                break;
            case DROP:
                game.hardDrop(p);
                break;
            case HOLD:
                game.hold(p);
                break;
        }
    }
}