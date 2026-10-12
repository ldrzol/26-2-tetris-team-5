package kr.ac.jbnu.se.tetris;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.network.InputMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;
import kr.ac.jbnu.se.tetris.network.SharedStateMessage;

public class SharedClientFrame extends JFrame {
    private final SharedBoardView view = new SharedBoardView();
    private final JLabel scoreLabel = new JLabel(" ", JLabel.CENTER);
    private final PeerConnection connection;

    public SharedClientFrame(PeerConnection connection) {
        this.connection = connection;

        view.setPreferredSize(new Dimension(280, 440));
        add(scoreLabel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);

        setupKeys();
        connection.setListener(message -> onMessage(message));

        setTitle("땅따먹기 - 상대 (내 블록: 검은 테두리)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
    }

    private void onMessage(Object message) {
        if (!(message instanceof SharedStateMessage)) {
            return;
        }
        SharedStateMessage state = (SharedStateMessage) message;
        SwingUtilities.invokeLater(() -> {
            view.setState(state);
            scoreLabel.setText("나(P2): " + state.getScore(1) + "    상대(P1): " + state.getScore(0));
        });
    }

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
            public void actionPerformed(ActionEvent e) {
                connection.send(new InputMessage(action));
            }
        });
    }
}