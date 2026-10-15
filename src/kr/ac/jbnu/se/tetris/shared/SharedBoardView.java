package kr.ac.jbnu.se.tetris.shared;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class SharedBoardView extends JPanel {

    /** 칸 하나의 기본 크기(px). 창을 늘리면 그릴 때 실제 크기로 다시 계산한다. */
    private static final int CELL = 20;

    private static final Color[] COLORS = {
            new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
            new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0), new Color(153, 153, 153)
    };

    private SharedState state;

    public SharedBoardView() {
        setPreferredSize(new Dimension(SharedGame.WIDTH * CELL, SharedGame.HEIGHT * CELL));
    }

    public void setState(SharedState state) {
        this.state = state;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (state == null) {
            return;
        }

        int cellW = getWidth() / SharedGame.WIDTH;
        int cellH = getHeight() / SharedGame.HEIGHT;
        int boardTop = getHeight() - SharedGame.HEIGHT * cellH;

        for (int y = 0; y < SharedGame.HEIGHT; y++) {
            for (int x = 0; x < SharedGame.WIDTH; x++) {
                Tetrominoes shape = state.shapeAt(x, y);
                if (shape != Tetrominoes.NoShape) {
                    drawSquare(g, x * cellW, boardTop + (SharedGame.HEIGHT - y - 1) * cellH, cellW, cellH, shape);
                }
            }
        }

        for (int p = 0; p < 2; p++) {
            Tetrominoes type = state.pieceType(p);
            if (type == Tetrominoes.NoShape) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                int px = state.pieceX(p, i) * cellW;
                int py = boardTop + (SharedGame.HEIGHT - state.pieceY(p, i) - 1) * cellH;
                drawSquare(g, px, py, cellW, cellH, type);
                g.setColor(p == 0 ? Color.WHITE : Color.BLACK);
                g.drawRect(px, py, cellW - 1, cellH - 1);
            }
        }

        if (state.isGameOver()) {
            String result = state.getWinner() == -1 ? "draw" : "P" + (state.getWinner() + 1) + " win";
            g.setColor(Color.RED);
            g.drawString("game over - " + result, 10, 20);
        }
    }

    private void drawSquare(Graphics g, int x, int y, int w, int h, Tetrominoes shape) {
        Color color = COLORS[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + h - 1, x, y);
        g.drawLine(x, y, x + w - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + h - 1, x + w - 1, y + h - 1);
        g.drawLine(x + w - 1, y + h - 1, x + w - 1, y + 1);
    }
}