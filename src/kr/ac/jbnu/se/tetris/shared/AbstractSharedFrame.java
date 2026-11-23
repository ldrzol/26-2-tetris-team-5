package kr.ac.jbnu.se.tetris.shared;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import kr.ac.jbnu.se.tetris.network.PeerConnection;

/**
 * 땅따먹기 창의 공통 뼈대. 위에 점수 줄, 가운데 판 그림을 놓고
 * 키 연결, 창을 닫을 때 연결 정리, 결과 창을 한 번만 띄우는 일을 맡는다.
 */
abstract class AbstractSharedFrame extends JFrame {

    protected final SharedBoardView view = new SharedBoardView();
    protected final JLabel scoreLabel = new JLabel(" ", JLabel.CENTER);
    private boolean settled = false;

    protected AbstractSharedFrame(String title) {
        add(scoreLabel, BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);
        setTitle(title);
    }

    /** 키 하나를 동작에 연결한다. 동작이 끝나면 afterKey() 를 부른다. */
    protected final void bind(int keyCode, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
                afterKey();
            }
        });
    }

    /** 키 동작 뒤에 할 일. 기본은 아무것도 하지 않는다. */
    protected void afterKey() {
    }

    /** 창을 닫으면 연결을 끊고 onExit 를 부르도록 한다. */
    protected final void releaseConnectionOnClose(PeerConnection connection, Runnable onExit) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                connection.close();
                onExit.run();
            }
        });
    }

    /** 결과 창은 판이 끝난 뒤 처음 한 번만 띄운다. */
    protected final void showResultOnce(PeerConnection connection, int winner, int me, int fee,
            Runnable onExit) {
        if (settled) {
            return;
        }
        settled = true;
        ResultPanel.showShared(this, connection, winner, me, fee, onExit);
    }
}
