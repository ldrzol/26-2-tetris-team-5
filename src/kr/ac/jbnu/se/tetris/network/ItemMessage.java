package kr.ac.jbnu.se.tetris.network;

import java.io.Serializable;

public class ItemMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Effect {
        PLUS_LINE
    }

    public final Effect effect;

    public ItemMessage(Effect effect) {
        this.effect = effect;
    }
}