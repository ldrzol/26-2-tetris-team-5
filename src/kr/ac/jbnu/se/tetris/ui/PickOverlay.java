package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 여러 개 중 하나를 고르는 덮개. 지금은 피버에서 블록 7종을 고를 때 쓴다.
 *
 * InputOverlay 와 같은 크림색 카드 모양이고, 칸을 누르거나 1~7 키로 고른다.
 * 반드시 하나를 골라야 하므로 Esc 로 닫을 수 없다.
 */
class PickOverlay extends DesignPanel {

	private static final int CARD_X = 250, CARD_Y = 452, CARD_W = 1500, CARD_H = 440;
	private static final Rectangle CARD = new Rectangle(CARD_X, CARD_Y, CARD_W, CARD_H);

	private static final int TITLE_TOP = 520;
	private static final float TITLE_SIZE = 48f, KEY_SIZE = 36f;
	private static final double TRACKING = 0.1;

	private static final int SLOT = 180, SLOT_GAP = 24, SLOT_TOP = 610;
	private static final int KEY_TOP = 810;
	private static final int SLOT_BORDER = 5;

	/** 고를 수 있는 블록 7종. 빈 칸과 쓰레기 줄은 뺀다. */
	static Tetrominoes[] pieces() {
		List<Tetrominoes> out = new ArrayList<>();
		for (Tetrominoes t : Tetrominoes.values())
			if (t != Tetrominoes.NoShape && t != Tetrominoes.GarbageShape)
				out.add(t);
		return out.toArray(new Tetrominoes[0]);
	}

	private final String title;
	private final Tetrominoes[] choices;
	private final Consumer<Tetrominoes> onPick;
	private boolean done = false;   // 고르는 것은 한 번만

	PickOverlay(String title, Tetrominoes[] choices, Consumer<Tetrominoes> onPick) {
		this.title = title;
		this.choices = choices;
		this.onPick = onPick;
		setOpaque(false);   // 뒤 화면이 비쳐야 한다

		for (int i = 0; i < choices.length; i++) {
			final int index = i;
			Ui.bind(this, KeyEvent.VK_1 + i, "pick" + i, () -> pick(index));
			onClick(slot(i), () -> pick(index));
		}
	}

	private void pick(int index) {
		if (done)
			return;
		done = true;
		onPick.accept(choices[index]);
	}

	/** 칸 전체를 한 덩어리로 보고 카드 가운데에 놓는다. */
	private Rectangle slot(int index) {
		int total = choices.length * SLOT + (choices.length - 1) * SLOT_GAP;
		int x = CARD.x + CARD.width / 2 - total / 2 + index * (SLOT + SLOT_GAP);
		return new Rectangle(x, SLOT_TOP, SLOT, SLOT);
	}

	@Override
	void layoutDesign() {
		// 올려 둘 Swing 부품이 없다. 전부 직접 그린다.
	}

	@Override
	void paintDesign(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		fill(g, CARD, Ui.CREAM);

		g.setColor(Color.BLACK);
		g.setFont(font(TITLE_SIZE, TRACKING));
		int titleX = px(CARD.x + CARD.width / 2) - g.getFontMetrics().stringWidth(title) / 2;
		g.drawString(title, titleX, py(TITLE_TOP) + g.getFontMetrics().getAscent());

		for (int i = 0; i < choices.length; i++) {
			Rectangle box = slot(i);
			outline(g, box, Color.BLACK, SLOT_BORDER);
			preview(g, box, choices[i], i);          // 칸마다 다른 색으로 보여 준다

			String key = String.valueOf(i + 1);
			g.setColor(Color.BLACK);
			g.setFont(font(KEY_SIZE, TRACKING));
			int keyX = px(box.x + box.width / 2) - g.getFontMetrics().stringWidth(key) / 2;
			g.drawString(key, keyX, py(KEY_TOP) + g.getFontMetrics().getAscent());
		}
	}
}
