package kr.ac.jbnu.se.tetris.game;

import java.awt.Color;
import java.awt.Graphics;
import java.util.function.BooleanSupplier;

import javax.swing.Timer;

public class LineClearEffect {
    private static final int TOTAL = 6;
    private static final int INTERVER = 100;

    private final GameBoard game;
    private final BooleanSupplier paused;
    private final Runnable onRepaint;
    private final Runnable onFinish;
    private final Timer timer = new Timer(INTERVER, e -> tick());

    private boolean on = false;
    private int count = 0;

    public LineClearEffect(GameBoard game, BooleanSupplier paused, Runnable onRepaint, Runnable onFinish){
        this.game = game;
        this.paused = paused;
        this.onRepaint = onRepaint;
        this.onFinish = onFinish;
    }

    public void startIfNeeded(){
        if(game.isClearing()&&!timer.isRunning()){
            count = 0;
            on = false;
            timer.start();
        }
    }
    public void stop(){
        timer.stop();
        on = false;
    }

    private void tick(){
        if(paused.getAsBoolean()){
            return;
        }
        on = !on;
        count++;
        if(count>=TOTAL){
            timer.stop();
            on = false;
            game.finishClear();
            onFinish.run();
            return;
        }
        onRepaint.run();
    }

    public void draw(Graphics g, int boardTop, int squareW, int squareH){
        if(!on){
            return;
        }
        g.setColor(Color.WHITE);
        for(int row = 0; row<GameBoard.VISIBLE_HEIGHT; row++){
            if(game.isFlashRow(row)){
                g.fillRect(0, boardTop+(GameBoard.VISIBLE_HEIGHT-row-1)*squareH, GameBoard.WIDTH*squareW, squareH);
            }
        }
    }
}
