package kr.ac.jbnu.se.tetris;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.network.BoardStateMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

public class DuelFrame extends JFrame {
    private final Board myBoard;
    private final OpponentView opponentView = new OpponentView();
    private final JLabel opponentStatus = new JLabel(" 0");
    private final PeerConnection connection;

    public DuelFrame(PeerConnection connection) {
        this.connection = connection;

        JLabel myStatus = new JLabel(" 0");
        myBoard = new Board(myStatus);
        myBoard.setPreferredSize(new Dimension(200, 440));
        opponentView.setPreferredSize(new Dimension(200, 440));

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(makePanel("나", myBoard, myStatus));
        root.add(makePanel("상대", opponentView, opponentStatus));
        add(root);

        setupKeys();
        connection.setListener(message -> onMessage(message));

        setTitle("아이템 대전");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();

        myBoard.start();
        new Timer(100, e -> sendSnapshot()).start();
    }

    private JPanel makePanel(String name, JPanel view, JLabel status) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(" " + name), BorderLayout.NORTH);
        panel.add(view, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private void sendSnapshot() {
        connection.send(takeSnapshot());
    }

    private BoardStateMessage takeSnapshot() {
        GameBoard game = myBoard.getGame();

        Tetrominoes[] cells = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
        for (int y = 0; y < GameBoard.HEIGHT; y++) {
            for (int x = 0; x < GameBoard.WIDTH; x++) {
                cells[y * GameBoard.WIDTH + x] = game.shapeAt(x, y);
            }
        }

        int[] pieceX = new int[4];
        int[] pieceY = new int[4];
        for (int i = 0; i < 4; i++) {
            pieceX[i] = game.pieceX(i);
            pieceY[i] = game.pieceY(i);
        }

        return new BoardStateMessage(cells, game.currentType(), pieceX, pieceY,
                game.getLinesRemoved(), game.isGameOver());
    }

    private void onMessage(Object message) {
        if (!(message instanceof BoardStateMessage)) {
            return;
        }
        BoardStateMessage state = (BoardStateMessage) message;
        SwingUtilities.invokeLater(() -> {
            opponentView.setState(state);
            opponentStatus.setText(state.gameOver ? "game over" : String.valueOf(state.linesRemoved));
        });
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "left", myBoard::moveLeft);
        bind(KeyEvent.VK_RIGHT, "right", myBoard::moveRight);
        bind(KeyEvent.VK_UP, "rotate", myBoard::rotateRight);
        bind(KeyEvent.VK_DOWN, "softDrop", myBoard::softDrop);
        bind(KeyEvent.VK_SPACE, "hardDrop", myBoard::hardDrop);
    }

    private void bind(int keyCode, String name, Runnable action) {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
}