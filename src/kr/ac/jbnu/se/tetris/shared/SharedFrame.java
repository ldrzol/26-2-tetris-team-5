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

    protected SharedFrame(PeerConnection connection, int fee, Runnable onExit, String title) {
        this.connection = connection;
        this.fee = fee;
        this.onExit = onExit;
        add(scoreLabel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);

        setupKeys();
        connection.setListener(this::onMessage);

        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                connection.close();
                onExit.run();
            }
        });
    }

    /** 상대에게서 메시지를 받았을 때 (방장/참가자가 다름). */
    protected abstract void onMessage(Object message);

    /** 내가 키를 눌렀을 때 (방장은 직접 움직이고, 참가자는 방장에게 보냄). */
    protected abstract void onKey(InputMessage.Action action);

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "left", InputMessage.Action.LEFT);
        bind(KeyEvent.VK_RIGHT, "right", InputMessage.Action.RIGHT);
        bind(KeyEvent.VK_UP, "rotate", InputMessage.Action.ROTATE);
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