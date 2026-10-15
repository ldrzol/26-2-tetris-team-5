package kr.ac.jbnu.se.tetris.network;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.item.Attack;

import java.io.Serializable;

/** 아이템 한 번 사용. 상대에게 거는 것도, 교환처럼 주고받는 것도 이 메시지로 간다. */
public class ItemMessage implements Serializable {
    private static final long serialVersionUID = 3L;

    public final Attack attack;

    /** 블록 교환에서 건네주는 블록. 나머지 아이템은 NoShape. */
    public final Tetrominoes piece;

    /** 블록 교환의 답장인지. 답장은 더 되돌려 보내지 않는다. */
    public final boolean reply;

    public ItemMessage(Attack attack) {
        this(attack, Tetrominoes.NoShape, false);
    }

    public ItemMessage(Attack attack, Tetrominoes piece, boolean reply) {
        this.attack = attack;
        this.piece = piece;
        this.reply = reply;
    }
}
