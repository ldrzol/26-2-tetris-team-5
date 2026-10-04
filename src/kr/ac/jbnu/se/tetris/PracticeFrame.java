package kr.ac.jbnu.se.tetris;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

import kr.ac.jbnu.se.tetris.ai.AIplayer;

public class PracticeFrame extends JFrame {
    private final Board playerBoard;
    private final Board aiBoard;
    private boolean paused = false;

    public PracticeFrame(){
        JLabel playerStatus = new JLabel("0");
        JLabel aiStatus = new JLabel("0");

        playerBoard = new Board(playerStatus);
        aiBoard = new Board(aiStatus);

        playerBoard.setPreferredSize(new Dimension(200,440));
        aiBoard.setPreferredSize(new Dimension(200,440));

        JPanel root = new JPanel(new GridLayout(1,2));
        root.add(makePanel("나",playerBoard,playerStatus));
        root.add(makePanel("AI",aiBoard, aiStatus));
        add(root);

        setupKeys();
        setTitle("AI연습");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();

        playerBoard.start();
        aiBoard.start();
        new AIplayer(aiBoard).start();
    }
    private JPanel makePanel(String name, Board board, JLabel status){
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(" "+name), BorderLayout.NORTH);
        panel.add(board, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }
    private void setupKeys(){
        bind(KeyEvent.VK_LEFT, "left", playerBoard::moveLeft);
        bind(KeyEvent.VK_RIGHT, "right", playerBoard::moveRight);
        bind(KeyEvent.VK_UP, "rotate", playerBoard::rotateRight);
        bind(KeyEvent.VK_DOWN, "softDrop", playerBoard::softDrop);
        bind(KeyEvent.VK_SPACE, "hardDrop", playerBoard::hardDrop);
        bind(KeyEvent.VK_P, "pause", () -> {
            paused = !paused;
            playerBoard.setPaused(paused);
            aiBoard.setPaused(paused);
        });
    }
    private void bind(int keyCode, String name, Runnable action){
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
    public static void main(String[] args) {
        PracticeFrame frame = new PracticeFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
