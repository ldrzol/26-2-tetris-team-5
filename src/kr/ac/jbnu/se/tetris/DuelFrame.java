package kr.ac.jbnu.se.tetris;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import kr.ac.jbnu.se.tetris.network.BoardStateMessage;
import kr.ac.jbnu.se.tetris.network.ItemMessage;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

/**
 * 2인 대전 화면.
 *
 * 아이템은 손에 최대 HAND_SIZE 개까지 들고 있고, 1~3 키로 골라 쓴다. 상대에게 거는
 * 아이템은 네트워크로 보내고, 내게 거는 아이템은 그 자리에서 내 보드에 건다.
 */
public class DuelFrame extends JFrame {

    /** 손에 들 수 있는 아이템 수. 키 1~3 과 같아야 한다. */
    private static final int HAND_SIZE = 3;

    private static final int FEVER_SCORE = 500;
    private static final long TIME_LIMIT_MS = 5 * 60 * 1000L;

    private final Board myBoard;
    private final OpponentView opponentView = new OpponentView();
    private final JLabel opponentStatus = new JLabel(" 0");
    private final PeerConnection connection;
    private final Random random = new Random();
    private final int fee;

    /** 손에 든 아이템. 빈 칸은 null. */
    private final Attack[] hand = new Attack[HAND_SIZE];
    private final ItemBar itemBar = new ItemBar(hand);

    private boolean finished = false;
    private final long endTime = System.currentTimeMillis() + TIME_LIMIT_MS;
    private int opponentScore = 0;

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
        add(root, BorderLayout.CENTER);
        add(itemBar, BorderLayout.SOUTH);

        setupKeys();
        connection.setListener(this::onMessage);

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

    // ── 아이템 ──────────────────────────────────────────────────────────────

    /** 빈 자리에 아이템을 하나 넣는다. 손이 가득 차 있으면 그냥 버린다. */
    private void addItem() {
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] == null) {
                hand[i] = Attack.random(random);
                itemBar.repaint();
                return;
            }
        }
    }

    /** 고른 자리의 아이템을 쓴다. 빈 자리면 아무 일도 하지 않는다. */
    private void useItem(int slot) {
        if (finished || hand[slot] == null)
            return;

        Attack attack = hand[slot];
        hand[slot] = null;
        itemBar.repaint();
        Sfx.play(Sfx.Sound.ITEM);

        if (attack == Attack.SWAP) {
            swapPieces();
        } else if (attack.self) {
            myBoard.getGame().getGimmick().apply(attack);
            myBoard.repaint();
        } else {
            connection.send(new ItemMessage(attack));
        }
    }

    /**
     * 내 블록과 상대 블록을 맞바꾼다.
     *
     * 내 블록을 보내기만 하고, 상대가 쥐고 있던 것은 답장으로 받는다. 100ms 마다 오는
     * 화면 정보에 실린 블록은 그만큼 옛날 것이라, 그걸 그대로 가져오면 서로 다른 블록을
     * 주고받아 교환이 어긋난다.
     */
    private void swapPieces() {
        connection.send(new ItemMessage(Attack.SWAP, myBoard.getGame().currentType(), false));
    }

    // ── 네트워크 ────────────────────────────────────────────────────────────

    private void sendSnapshot() {
        connection.send(takeSnapshot());
        if (finished) {
            return;
        }
        if (myBoard.getGame().isGameOver()) {
            finished = true;
            showResult("패배", "참가비 " + fee + " 코인을 잃었습니다 (보유 " + Coins.get() + ")");
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
                + (System.currentTimeMillis() < feverUntil ? " - 피버! 아이템 +1" : ""));
    }

    private BoardStateMessage takeSnapshot() {
        GameBoard game = myBoard.getGame();

        Tetrominoes[] cells = new Tetrominoes[GameBoard.WIDTH * GameBoard.HEIGHT];
        for (int y = 0; y < GameBoard.HEIGHT; y++) {
            for (int x = 0; x < GameBoard.WIDTH; x++) {
                cells[y * GameBoard.WIDTH + x] = game.shapeAt(x, y);
            }
        }

        // 칸 수는 4가 아닐 수 있다. (블록 2개 아이템이 걸리면 8)
        int n = game.pieceCount();
        int[] pieceX = new int[n];
        int[] pieceY = new int[n];
        for (int i = 0; i < n; i++) {
            pieceX[i] = game.pieceX(i);
            pieceY[i] = game.pieceY(i);
        }

        return new BoardStateMessage(cells, game.currentType(), pieceX, pieceY,
                game.getLinesRemoved(), game.getScore(), game.isGameOver());
    }

    private void onMessage(Object message) {
        if (message instanceof ItemMessage) {
            ItemMessage item = (ItemMessage) message;
            SwingUtilities.invokeLater(() -> receiveItem(item));
            return;
        }
        if (!(message instanceof BoardStateMessage)) {
            return;
        }
        BoardStateMessage state = (BoardStateMessage) message;
        SwingUtilities.invokeLater(() -> receiveState(state));
    }

    private void receiveItem(ItemMessage item) {
        if (item.attack == Attack.SWAP) {
            receiveSwap(item);
        } else {
            myBoard.getGame().getGimmick().apply(item.attack);
        }
        myBoard.repaint();
    }

    /**
     * 블록 교환을 받는다.
     *
     * 먼저 받은 쪽은 상대 블록을 받고 자기 것을 답장으로 돌려준다. 답장을 받은 쪽은
     * 거기 실린 블록으로 바꾸기만 한다. 내가 못 받는 상황이면 빈 모양을 돌려보내서
     * 상대도 그대로 두게 한다. 그래야 블록이 늘거나 사라지지 않는다.
     */
    private void receiveSwap(ItemMessage item) {
        if (item.reply) {
            myBoard.getGame().replaceCurrentPiece(item.piece);
            return;
        }
        Tetrominoes mine = myBoard.getGame().currentType();
        boolean took = myBoard.getGame().replaceCurrentPiece(item.piece);
        connection.send(new ItemMessage(Attack.SWAP, took ? mine : Tetrominoes.NoShape, true));
    }

    private void receiveState(BoardStateMessage state) {
        opponentView.setState(state);
        opponentScore = state.score;
        opponentStatus.setText(state.gameOver ? "game over" : String.valueOf(state.score));
        if (state.gameOver && !finished) {
            finished = true;
            myBoard.setPaused(true);
            Coins.add(fee * 2);
            showResult("승리!", "참가비 " + fee + " 코인을 받았습니다 (보유 " + Coins.get() + ")");
        }
    }

    // ── 끝내기 ──────────────────────────────────────────────────────────────

    private void finishByTime() {
        finished = true;
        myBoard.setPaused(true);
        int mine = myBoard.score();
        if (mine > opponentScore) {
            Coins.add(fee * 2);
            showResult("승리!", "시간 종료 - 참가비 " + fee + " 코인을 받았습니다 (보유 " + Coins.get() + ")");
        } else if (mine == opponentScore) {
            Coins.add(fee);
            showResult("무승부", "시간 종료 - 참가비를 돌려받았습니다 (보유 " + Coins.get() + ")");
        } else {
            showResult("패배", "시간 종료 - 참가비 " + fee + " 코인을 잃었습니다 (보유 " + Coins.get() + ")");
        }
    }

    private void showResult(String title, String detail) {
        setTitle(title);
        ResultPanel.show(this, connection, title, detail);
    }

    // ── 조작키 ──────────────────────────────────────────────────────────────

    private void setupKeys() {
        // 환경 설정의 1p 조작키를 그대로 쓴다. (예전에는 방향키가 박혀 있었다)
        GameKeys.bind(getRootPane(), 0, myBoard);

        for (int i = 0; i < HAND_SIZE; i++) {
            final int slot = i;
            Ui.bind(getRootPane(), KeyEvent.VK_1 + i, "item" + i, () -> useItem(slot));
        }
    }

    /**
     * 손에 든 아이템을 아이콘으로 그리는 띠. 빈 자리는 테두리만 그린다.
     *
     * 디자인 화면(DesignPanel)이 아니라 예전 창에 붙는 패널이라 좌표를 직접 쓴다.
     */
    private static class ItemBar extends JPanel {

        private static final int SLOT = 56, GAP = 10, PAD = 8;
        private static final Color BG = new Color(24, 24, 28);
        private static final Color EMPTY = new Color(70, 68, 62);
        private static final Color TEXT = new Color(222, 216, 200);

        private final Attack[] hand;

        ItemBar(Attack[] hand) {
            this.hand = hand;
            setBackground(BG);
            setPreferredSize(new Dimension(0, SLOT + PAD * 2 + 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int x = PAD;
            for (int i = 0; i < hand.length; i++) {
                g.setColor(EMPTY);
                g.drawRect(x, PAD, SLOT, SLOT);

                Attack attack = hand[i];
                if (attack != null) {
                    Image icon = DesignPanel.image(attack.icon);
                    if (icon != null)
                        g.drawImage(icon, x + 2, PAD + 2, SLOT - 4, SLOT - 4, this);
                }

                g.setColor(TEXT);
                g.drawString(String.valueOf(i + 1), x + 2, PAD + SLOT + 14);
                if (attack != null)
                    g.drawString(attack.label, x + 16, PAD + SLOT + 14);

                x += SLOT + GAP + 70;   // 이름을 적을 자리만큼 더 띄운다
            }
        }
    }
}
