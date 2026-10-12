package kr.ac.jbnu.se.tetris;
import  java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.InetAddress;
import javax.swing.*;

import kr.ac.jbnu.se.tetris.network.GameClient;
import kr.ac.jbnu.se.tetris.network.GameServer;
import kr.ac.jbnu.se.tetris.network.PeerConnection;

public class LobbyFrame extends JFrame{
    private static final int PORT = 5000;
    private final JLabel statusLabel = new JLabel("방을 만들거나 참가하세요", JLabel.CENTER);
    private final JButton createButton = new JButton("방 만들기");
    private final JButton joinButton = new JButton("방 참가");
    private PeerConnection connection;
    private String mode;

    public LobbyFrame(){
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(createButton);
        buttons.add(joinButton);

        add(statusLabel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        createButton.addActionListener(e-> createRoom());
        joinButton.addActionListener(e-> joinRoom());

        setTitle("대전 로비");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(320,160);
    }
    private void createRoom(){
        String[] options = {"아이템 대전", "땅따먹기"};
        int choice = JOptionPane.showOptionDialog(this, "게임 모드를 고르세요", "모드 선택",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice < 0) {
            return;
        }
        mode = (choice == 0) ? "ITEM" : "SHARED";

        setButtonsEnabled(false);

        try{
            String myIp= InetAddress.getLocalHost().getHostAddress();
            statusLabel.setText("내 IP: "+ myIp + "(포트"+PORT+")상대를 기다리는 중...");
        }catch (IOException e){
            statusLabel.setText("내 IP를 알 수 없음. 상대를 기다리는 중...");
        }
        new Thread(()->{
            try{
                connection = new GameServer().waitForClient(PORT, message-> onMessage(message));
                SwingUtilities.invokeLater(()->onConnected("상대가 들어왔습니다"));
            }catch (IOException e){
                SwingUtilities.invokeLater(() -> onFailed("방 만들기 실패: " + e.getMessage()));
            }
        }).start();
    }
    private void joinRoom(){
        String ip = JOptionPane.showInputDialog(this, "방장의 IP를 입력하세요");
        if (ip == null || ip.trim().isEmpty()) {
            return;
        }
        setButtonsEnabled(false);
        statusLabel.setText(ip.trim() + " 에 접속하는 중...");
        new Thread(() -> {
            try {
                connection = new GameClient().connectTo(ip.trim(), PORT, message -> onMessage(message));
                SwingUtilities.invokeLater(() -> onConnected("방에 들어갔습니다"));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> onFailed("접속 실패: " + e.getMessage()));
            }
        }).start();
    }
    private void onConnected(String text) {
        if (mode == null) {
            statusLabel.setText(text + " - 방장이 시작하기를 기다리는 중...");
            connection.send("READY");
        } else {
            statusLabel.setText(text + " - 상대 준비를 기다리는 중...");
        }
    }
    private void onFailed(String text) {
        statusLabel.setText(text);
        setButtonsEnabled(true);
    }
    private void onMessage(Object message) {
        if (!(message instanceof String)) {
            return;
        }
        String text = (String) message;
        SwingUtilities.invokeLater(() -> handleText(text));
    }

    private void handleText(String text) {
        if (mode != null && text.equals("READY")) {
            connection.send("MODE:" + mode);
            openGame(mode, true);
        } else if (mode == null && text.startsWith("MODE:")) {
            openGame(text.substring(5), false);
        }
    }

    private void openGame(String selectedMode, boolean isHost) {
        JFrame game;
        if (selectedMode.equals("SHARED")) {
            game = isHost ? new SharedHostFrame(connection) : new SharedClientFrame(connection);
        } else {
            game = new DuelFrame(connection);
        }
        game.setLocationRelativeTo(null);
        game.setVisible(true);
        dispose();
    }
    private void setButtonsEnabled(boolean enabled) {
        createButton.setEnabled(enabled);
        joinButton.setEnabled(enabled);
    }
    public static void main(String[] args) {
        LobbyFrame frame = new LobbyFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}