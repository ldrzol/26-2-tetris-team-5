package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.util.List;


import javax.swing.JPanel;

public class Nextpanel extends JPanel{
    private static final int COUNT = 5;
    public static final int TOP = 24;

    private static final Color[] COLORS = { new Color(0, 0, 0), new Color(204, 102, 102), new Color(102, 204, 102),
            new Color(102, 102, 204), new Color(204, 204, 102), new Color(204, 102, 204), new Color(102, 204, 204),
            new Color(218, 170, 0), new Color(153, 153, 153) };

    private final GameBoard game;
    private final Shape shape = new Shape();

    public Nextpanel(GameBoard game, int width){
        this.game = game;
        setPreferredSize(new Dimension(width, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        g.drawString("NEXT", 6, 16);

        // 블록 하나가 차지하는 자리는 세로 5칸(4칸 + 여백 1칸). 폭과 높이 둘 다에 맞춰 칸 크기를 정한다.
        int cell = Math.max(2, Math.min(getWidth() / 4, (getHeight() - TOP) / (COUNT * 5)));

        List<Tetrominoes> next = game.peekNext(COUNT);
        for (int n = 0; n < next.size(); n++) {
            int slotTop = TOP + n * 5 * cell;
            drawPiece(g, shape, next.get(n), getWidth(), slotTop, cell, false);
        }
    }

    static void drawPiece(Graphics g0, Shape shape, Tetrominoes type, int panelWidth, int slotTop, int cell,
                          boolean dim) {
        shape.setShape(type);
        int originX = (panelWidth - cell) / 2;   // 기준점(0,0) 칸의 왼쪽 위 x
        int originY = slotTop + 2 * cell;        // Shape의 y는 위쪽이 +, 최대 +2
        Color color = COLORS[type.ordinal()];

        Graphics2D g = (Graphics2D) g0.create();
        if (dim)
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));

        for (int i = 0; i < 4; i++) {
            int x = originX + shape.x(i) * cell;
            int y = originY - shape.y(i) * cell;

            g.setColor(color);
            g.fillRect(x + 1, y + 1, cell - 2, cell - 2);
            g.setColor(color.brighter());
            g.drawLine(x, y + cell - 1, x, y);
            g.drawLine(x, y, x + cell - 1, y);
            g.setColor(color.darker());
            g.drawLine(x + 1, y + cell - 1, x + cell - 1, y + cell - 1);
            g.drawLine(x + cell - 1, y + cell - 1, x + cell - 1, y + 1);
        }
        g.dispose();
    }
}
