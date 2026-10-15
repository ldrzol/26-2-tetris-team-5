package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.item.Attack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 여러 개 중 하나를 고르는 덮개. 피버에서 블록을 고를 때와, 대전 시작에서
 * 가방에 있는 아이템 하나를 고를 때 쓴다.
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

	/** 아이템 칸 안쪽: 아이콘과 그 아래 이름. */
	private static final int ITEM_ICON = 96, ITEM_ICON_TOP = 20, ITEM_LABEL_TOP = 128;
	private static final float ITEM_LABEL_SIZE = 26f;

	/** 한 번에 보여 줄 수 있는 칸 수. 키 1~7 과 같다. */
	static final int MAX = 7;

	/** 고를 수 있는 블록 7종. 빈 칸과 쓰레기 줄은 뺀다. */
	static Tetrominoes[] pieces() {
		List<Tetrominoes> out = new ArrayList<>();
		for (Tetrominoes t : Tetrominoes.values())
			if (t != Tetrominoes.NoShape && t != Tetrominoes.GarbageShape)
				out.add(t);
		return out.toArray(new Tetrominoes[0]);
	}

	private final String title;

	/** 둘 중 하나만 들어 있다. 블록을 고르는 덮개면 pieces, 아이템이면 items. */
	private final Tetrominoes[] pieces;
	private final Attack[] items;

	private final int count;
	private final Consumer<Integer> onPick;
	private boolean done = false;   // 고르는 것은 한 번만

	PickOverlay(String title, Tetrominoes[] choices, Consumer<Tetrominoes> onPick) {
		this(title, choices, null, choices.length, i -> onPick.accept(choices[i]));
	}

	PickOverlay(String title, Attack[] choices, Consumer<Attack> onPick) {
		this(title, null, choices, choices.length, i -> onPick.accept(choices[i]));
	}

	private PickOverlay(String title, Tetrominoes[] pieces, Attack[] items, int count,
			Consumer<Integer> onPick) {
		this.title = title;
		this.pieces = pieces;
		this.items = items;
		this.count = count;
		this.onPick = onPick;
		setOpaque(false);   // 뒤 화면이 비쳐야 한다

		for (int i = 0; i < count; i++) {
			final int index = i;
			Ui.bind(this, KeyEvent.VK_1 + i, "pick" + i, () -> pick(index));
			onClick(slot(i), () -> pick(index));
		}
	}

	private void pick(int index) {
		if (done)
			return;
		done = true;
		onPick.accept(index);
	}

	/** 칸 전체를 한 덩어리로 보고 카드 가운데에 놓는다. */
	private Rectangle slot(int index) {
		int total = count * SLOT + (count - 1) * SLOT_GAP;
		int x = CARD.x + CARD.width / 2 - total / 2 + index * (SLOT + SLOT_GAP);
		return new Rectangle(x, SLOT_TOP, SLOT, SLOT);
	}

	/** 아이템 칸: 아이콘과 그 아래 이름. */
	private void item(Graphics2D g, Rectangle box, Attack attack) {
		int x = box.x + (box.width - ITEM_ICON) / 2;
		drawImage(g, image(attack.icon), new Rectangle(x, box.y + ITEM_ICON_TOP, ITEM_ICON, ITEM_ICON));

		g.setColor(Color.BLACK);
		g.setFont(font(ITEM_LABEL_SIZE));
		int labelX = px(box.x + box.width / 2) - g.getFontMetrics().stringWidth(attack.label) / 2;
		g.drawString(attack.label, labelX, py(box.y + ITEM_LABEL_TOP) + g.getFontMetrics().getAscent());
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

		for (int i = 0; i < count; i++) {
			Rectangle box = slot(i);
			outline(g, box, Color.BLACK, SLOT_BORDER);
			if (pieces != null)
				preview(g, box, pieces[i], i);       // 칸마다 다른 색으로 보여 준다
			else
				item(g, box, items[i]);

			String key = String.valueOf(i + 1);
			g.setColor(Color.BLACK);
			g.setFont(font(KEY_SIZE, TRACKING));
			int keyX = px(box.x + box.width / 2) - g.getFontMetrics().stringWidth(key) / 2;
			g.drawString(key, keyX, py(KEY_TOP) + g.getFontMetrics().getAscent());
		}
	}
}
