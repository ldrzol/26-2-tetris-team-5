package kr.ac.jbnu.se.tetris.network;

/** [버그 18] 대전 결과. 예전에는 "YOU WIN" 같은 문자열을 비교해서 정산했다. */
public enum MatchResult {
    WIN, LOSE, DRAW;

    /** 상대 쪽에서 본 결과. 내가 이기면 상대는 지고, 비기면 둘 다 비긴다. */
    public MatchResult opposite() {
        if (this == WIN) {
            return LOSE;
        }
        if (this == LOSE) {
            return WIN;
        }
        return DRAW;
    }
}