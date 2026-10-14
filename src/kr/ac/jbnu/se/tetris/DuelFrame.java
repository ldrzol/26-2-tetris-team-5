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
import kr.ac.jbnu.se.tetris.network.ItemMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

public class DuelFrame extends JFrame {
    private final Board myBoard;
    private final OpponentView opponentView = new OpponentView();
    private final JLabel opponentStatus = new JLabel(" 0");
    private final PeerConnection connection;
    private int items = 0;
    private final int fee;
    private boolean finished = false;
    private final long endTime = System.currentTimeMillis() + 5 * 60 * 1000L;
    private int opponentLines = 0;

    public DuelFrame(PeerConnection connection, int fee) {
        this.connection = connection;
        this.fee = fee;

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
        if (finished) {
            return;
        }
        if (myBoard.getGame().isGameOver()) {
            finished = true;
            setTitle("패배 - 참가비 " + fee + " 코인을 잃었습니다");
            return;
        }
        long left = (endTime - System.currentTimeMillis()) / 1000;
        if (left <= 0) {
            finishByTime();
            return;
        }
        setTitle("아이템 대전 - 남은 시간 " + left / 60 + ":" + String.format("%02d", left % 60)
                + " - 아이템 " + itemCount() + "개 (Z 키로 사용)");
    }

    private void finishByTime() {
        finished = true;
        myBoard.setPaused(true);
        int mine = myBoard.getGame().getLinesRemoved();
        if (mine > opponentLines) {
            Coins.add(fee * 2);
            setTitle("시간 종료 - 승리! 참가비 " + fee + " 코인을 받았습니다");
        } else if (mine == opponentLines) {
            Coins.add(fee);
            setTitle("시간 종료 - 무승부, 참가비를 돌려받았습니다");
        } else {
            setTitle("시간 종료 - 패배, 참가비 " + fee + " 코인을 잃었습니다");
        }
    }

    public int itemCount() {
        return items;
    }

    public void addItem() {
        items++;
    }

    private void useItem() {
        if (items > 0) {
            items--;
            connection.send(new ItemMessage(ItemMessage.Effect.PLUS_LINE));
        }
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
        if (message instanceof ItemMessage) {
            SwingUtilities.invokeLater(() -> {
                new Gimmick(myBoard.getGame()).plusLine();
                myBoard.repaint();
            });
            return;
        }
        if (!(message instanceof BoardStateMessage)) {
            return;
        }
        BoardStateMessage state = (BoardStateMessage) message;
        SwingUtilities.invokeLater(() -> {
            opponentView.setState(state);
            opponentLines = state.linesRemoved;
            opponentStatus.setText(state.gameOver ? "game over" : String.valueOf(state.linesRemoved));
            if (state.gameOver && !finished) {
                finished = true;
                Coins.add(fee * 2);
                setTitle("승리! 참가비 " + fee + " 코인을 받았습니다");
            }
        });
    }

    private void setupKeys() {
        bind(KeyEvent.VK_LEFT, "left", myBoard::moveLeft);
        bind(KeyEvent.VK_RIGHT, "right", myBoard::moveRight);
        bind(KeyEvent.VK_UP, "rotate", myBoard::rotateRight);
        bind(KeyEvent.VK_DOWN, "softDrop", myBoard::softDrop);
        bind(KeyEvent.VK_SPACE, "hardDrop", myBoard::hardDrop);
        bind(KeyEvent.VK_Z, "useItem", this::useItem);
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