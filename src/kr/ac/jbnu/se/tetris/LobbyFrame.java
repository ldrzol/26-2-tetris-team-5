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
        statusLabel.setText(text + " - 연결 완료!");
        connection.send("안녕! 연결 확인");
    }
    private void onFailed(String text) {
        statusLabel.setText(text);
        setButtonsEnabled(true);
    }
    private void onMessage(Object message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("받은 메시지: " + message));
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
