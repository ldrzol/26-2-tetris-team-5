package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import kr.ac.jbnu.se.tetris.network.BoardStateMessage;

public class OpponentView extends JPanel {
    private static final Color[] COLORS = {
            new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
            new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0), new Color(153, 153, 153)
    };

    private BoardStateMessage state;

    public void setState(BoardStateMessage state) {
        this.state = state;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (state == null) {
            return;
        }

        int cellW = getWidth() / GameBoard.WIDTH;
        int cellH = getHeight() / GameBoard.HEIGHT;
        int boardTop = getHeight() - GameBoard.HEIGHT * cellH;

        for (int y = 0; y < GameBoard.HEIGHT; y++) {
            for (int x = 0; x < GameBoard.WIDTH; x++) {
                Tetrominoes shape = state.cells[y * GameBoard.WIDTH + x];
                if (shape != Tetrominoes.NoShape) {
                    drawSquare(g, x * cellW, boardTop + (GameBoard.HEIGHT - y - 1) * cellH, cellW, cellH, shape);
                }
            }
        }

        if (state.currentType != Tetrominoes.NoShape) {
            for (int i = 0; i < 4; i++) {
                drawSquare(g, state.pieceX[i] * cellW,
                        boardTop + (GameBoard.HEIGHT - state.pieceY[i] - 1) * cellH,
                        cellW, cellH, state.currentType);
            }
        }

        if (state.gameOver) {
            g.setColor(Color.RED);
            g.drawString("game over", 10, 20);
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