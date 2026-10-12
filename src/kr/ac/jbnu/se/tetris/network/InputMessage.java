package kr.ac.jbnu.se.tetris.network;

import java.io.Serializable;

public class InputMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Action {
        LEFT, RIGHT, ROTATE, DOWN, DROP
    }

    public final Action action;

    public InputMessage(Action action) {
        this.action = action;
    }
}