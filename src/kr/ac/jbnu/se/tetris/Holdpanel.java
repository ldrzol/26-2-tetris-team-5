package kr.ac.jbnu.se.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class Holdpanel extends JPanel{
    private final GameBoard game;
    private final Shape shape = new Shape();

    public Holdpanel(GameBoard game, int width){
        this.game = game;
        setPreferredSize(new Dimension(width, 0));
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        g.drawString("HOLD", 6, 16);

        Tetrominoes held = game.getHoldType();
        if(held == Tetrominoes.NoShape){
            return;
        }

        int cell = Math.max(2, Math.min(getWidth()/4, (getHeight()-Nextpanel.TOP)/5));
        Nextpanel.drawPiece(g, shape, held, getWidth(), Nextpanel.TOP, cell, game.isHoldUsed());
    }
}
