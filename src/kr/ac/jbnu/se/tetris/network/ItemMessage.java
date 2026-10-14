package kr.ac.jbnu.se.tetris.network;

import java.io.Serializable;

public class ItemMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Effect {
        SPEED_UP, FADE_BLOCKS, NO_ROTATE, REVERSE_KEYS, INVISIBLE, FLIP_BOARD, PLUS_LINE,
        COVER_SCREEN, ERASE_DOT, ERASE_CROSS, ERASE_SQUARE, ERASE_LINE
    }
    public final Effect effect;

    public ItemMessage(Effect effect) {
        this.effect = effect;
    }
}