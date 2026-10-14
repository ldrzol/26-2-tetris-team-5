package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import kr.ac.jbnu.se.tetris.network.PeerConnection;

/** 2인 대전이 끝났을 때 창 위에 덮는 승패·정산 화면. */
class ResultPanel extends JPanel {

    private ResultPanel(String title, String detail, Runnable onMenu) {
        setLayout(new GridBagLayout());
        setOpaque(false);
        addMouseListener(new MouseAdapter() { });   // 뒤 화면이 클릭을 받지 않게 막는다

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);

        JLabel detailLabel = new JLabel(detail, JLabel.CENTER);
        detailLabel.setForeground(Color.WHITE);

        JButton menuButton = new JButton("메인으로");
        menuButton.addActionListener(e -> onMenu.run());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(8, 10, 8, 10);
        c.gridy = 0;
        add(titleLabel, c);
        c.gridy = 1;
        add(detailLabel, c);
        c.gridy = 2;
        add(menuButton, c);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /** 제목과 설명을 창 위에 덮는다. 메인으로를 누르면 연결을 닫고 창을 치운 뒤 onExit 를 부른다. */
    private static void show(JFrame frame, PeerConnection connection, String title, String detail,
            Runnable onExit) {
        ResultPanel panel = new ResultPanel(title, detail, () -> {
            connection.close();
            frame.dispose();
            onExit.run();
        });
        frame.setGlassPane(panel);
        panel.setVisible(true);
    }

    /** 땅따먹기 결과. winner: 0=P1, 1=P2, -1=무승부. me: 나는 P1(0) 또는 P2(1). */
    static void showShared(JFrame frame, PeerConnection connection, int winner, int me, int fee,
            Runnable onExit) {
        String title = winner == -1 ? "무승부" : winner == me ? "승리!" : "패배";
        String detail = Wallet.settle(winner, me, fee) + " (보유 " + Wallet.coins() + ")";
        frame.setTitle(title);
        show(frame, connection, title, detail, onExit);
    }
}