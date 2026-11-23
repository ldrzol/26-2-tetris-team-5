package kr.ac.jbnu.se.tetris.shared;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

/** 땅따먹기 창의 공통 부분. 방장/참가자가 다른 부분만 하위 클래스가 채운다. */
abstract class SharedFrame extends JFrame {
    protected final SharedBoardView view = new SharedBoardView();
    protected final JLabel scoreLabel = new JLabel(" ", SwingConstants.CENTER);
    protected final transient PeerConnection connection;
    protected final int fee;
    protected final transient Runnable onExit;
    protected boolean settled = false;
    /** [버그 14] 참가자가 창을 열고 준비됐다는 신호. */
    protected static final String READY = "SHARED_READY";

    /** [버그 14] 카운트다운 신호. 뒤에 남은 초가 붙는다. 예: "COUNT:3" */
    protected static final String COUNT = "COUNT:";

    protected SharedFrame(PeerConnection connection, int fee, Runnable onExit, String title) {
        this.connection = connection;
        this.fee = fee;
        this.onExit = onExit;
        add(scoreLabel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);

        setupKeys();
        connection.setListener(this::onMessage);
        // [버그 11] 상대가 사라지면(창 닫기, 랜선 빠짐, 먹통) 안내한다. 받는 스레드에서 불리므로 화면 스레드로 넘긴다.
        connection.setOnDisconnected(() -> SwingUtilities.invokeLater(this::onDisconnected));

        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onClosed();   // [버그 9] 하위 클래스가 정리할 것 (방장 타이머 멈추기)
                connection.close();
                onExit.run();
            }
        });
    }

    /** 상대에게서 메시지를 받았을 때 (방장/참가자가 다름). */
    protected abstract void onMessage(Object message);

    /** 내가 키를 눌렀을 때 (방장은 직접 움직이고, 참가자는 방장에게 보냄). */
    protected abstract void onKey(InputMessage.Action action);

    /** [버그 9] 창이 닫히거나 게임을 더 진행할 수 없을 때 하위 클래스가 정리할 것. */
    protected void onClosed() {
        // 기본은 정리할 것이 없다. 방장은 블록 떨어뜨리는 타이머를 멈춘다.
    }

    /** [버그 11] 상대와 연결이 끊겼을 때. 아직 결과가 안 났으면 멈추고 안내만 띄운다. */
    private void onDisconnected() {
        if (settled) {
            return;   // 이미 결과가 나왔으면 할 것이 없다
        }
        settled = true;   // 끊긴 뒤에는 결과·정산을 하지 않는다 (끊겼을 때 정산 규칙은 팀과 정하기)
        onClosed();
        scoreLabel.setText("상대와 연결이 끊어졌습니다 - 창을 닫으면 메뉴로 돌아갑니다");
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "left", InputMessage.Action.LEFT);
        bind(KeyEvent.VK_RIGHT, "right", InputMessage.Action.RIGHT);
        bind(KeyEvent.VK_UP, "rotate", InputMessage.Action.ROTATE);
        bind(KeyEvent.VK_Z, "rotateLeft", InputMessage.Action.ROTATE_LEFT);
        bind(KeyEvent.VK_C, "hold", InputMessage.Action.HOLD);
        bind(KeyEvent.VK_DOWN, "down", InputMessage.Action.DOWN);
        bind(KeyEvent.VK_SPACE, "drop", InputMessage.Action.DROP);
    }

    private void bind(int keyCode, String name, InputMessage.Action action) {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onKey(action);
            }
        });
    }
}