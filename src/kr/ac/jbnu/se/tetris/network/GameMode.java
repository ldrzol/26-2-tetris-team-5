package kr.ac.jbnu.se.tetris.network;

/** 2인 대전 모드. 방장이 고르고 신호로 상대에게 알린다. */
public enum GameMode {
    CLASSIC("클래식"),
    SHARED("땅따먹기");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    /** 화면에 보이는 이름. */
    public String label() {
        return label;
    }
}