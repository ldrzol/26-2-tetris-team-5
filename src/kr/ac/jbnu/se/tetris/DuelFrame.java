package kr.ac.jbnu.se.tetris;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Random;

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
    private final Random random = new Random();
    private final int fee;
    private boolean finished = false;
    private final long endTime = System.currentTimeMillis() + 5 * 60 * 1000L;
    private int opponentScore = 0;
    private static final int FEVER_SCORE = 500;
    private int nextFeverScore = FEVER_SCORE;
    private long feverUntil = 0;

    public DuelFrame(PeerConnection connection, int fee) {
        this.connection = connection;
        this.fee = fee;

        JLabel myStatus = new JLabel(" 0");
        myBoard = new Board(myStatus);
        myBoard.getGame().setLevelProgression(false);
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
        if (myBoard.score() >= nextFeverScore) {
            nextFeverScore += FEVER_SCORE;
            addItem();
            feverUntil = System.currentTimeMillis() + 3000;
        }
        setTitle("아이템 대전 - 남은 시간 " + left / 60 + ":" + String.format("%02d", left % 60)
                + " - 점수 " + myBoard.score() + " : " + opponentScore
                + " - 아이템 " + itemCount() + "개 (Z)"
                + (System.currentTimeMillis() < feverUntil ? " - 피버! 아이템 +1" : ""));
    }

    private void finishByTime() {
        finished = true;
        myBoard.setPaused(true);
        int mine = myBoard.score();
        if (mine > opponentScore) {
            Coins.add(fee * 2);
            setTitle("시간 종료 - 승리! 참가비 " + fee + " 코인을 받았습니다");
        } else if (mine == opponentScore) {
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
        if (!finished && items > 0) {
            items--;
            ItemMessage.Effect[] all = ItemMessage.Effect.values();
            connection.send(new ItemMessage(all[random.nextInt(all.length)]));
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
                game.getLinesRemoved(), game.getScore(), game.isGameOver());
    }
    private GimmickEffect makeEffect(ItemMessage.Effect effect) {
        switch (effect) {
            case SPEED_UP:
                return new SpeedUp();
            case FADE_BLOCKS:
                return new FadeBlocks();
            case NO_ROTATE:
                return new NoRotate();
            case REVERSE_KEYS:
                return new ReverseKeys();
            case INVISIBLE:
                return new InvisiblePiece();
            case COVER_SCREEN:
                return new CoverScreen();
            case ERASE_DOT:
                return new EraseDot();
            case ERASE_CROSS:
                return new EraseCross();
            case ERASE_SQUARE:
                return new EraseSquare();
            case ERASE_LINE:
                return new EraseLine();
            default:
                return new PlusLine();
        }
    }

    private void onMessage(Object message) {
        if (message instanceof ItemMessage) {
            ItemMessage item = (ItemMessage) message;
            SwingUtilities.invokeLater(() -> {
                myBoard.getGame().getGimmick().add(makeEffect(item.effect));
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
            opponentScore = state.score;
            opponentStatus.setText(state.gameOver ? "game over" : String.valueOf(state.score));
            if (state.gameOver && !finished) {
                finished = true;
                myBoard.setPaused(true);
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