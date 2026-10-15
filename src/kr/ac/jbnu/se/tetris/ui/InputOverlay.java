package kr.ac.jbnu.se.tetris.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 한 가지를 묻는 덮개. JOptionPane 대신 쓴다.
 *
 * 두 가지로 쓴다.
 *  - 글자 한 줄 받기 (랭킹 이름, 방장 IP, 참가비) : 확인/취소 버튼
 *  - 둘 중 하나 고르기 (게임 모드)               : 고른 글자가 그대로 넘어온다
 *
 * GAME OVER 덮개와 같은 크림색 카드 모양이다. Enter 로 왼쪽 버튼, Esc 로 취소하고
 * 버튼을 눌러도 된다. 취소하면 null 을 넘긴다.
 */
class InputOverlay extends DesignPanel {

	private static final int CARD_X = 485, CARD_Y = 452, CARD_W = 1029;
	private static final Rectangle FIELD = new Rectangle(575, 620, 849, 104);

	private static final int TITLE_TOP = 520;
	private static final float TITLE_SIZE = 48f, TEXT_SIZE = 36f;
	private static final double TRACKING = 0.1;

	private static final int BUTTON_W = 280, BUTTON_H = 95, BUTTON_GAP = 61;
	private static final int BUTTON_BORDER = 7;
	private static final int FIELD_BORDER = 4;

	/** 입력 칸이 없으면 그 높이만큼 카드가 줄고 버튼이 올라온다. */
	private static final int WITH_FIELD_TOP = 772, NO_FIELD_TOP = 640;
	private static final int CARD_H = 440, CARD_SHRINK = WITH_FIELD_TOP - NO_FIELD_TOP;

	private static final int ARROW_W = 21, ARROW_H = 39, ARROW_GAP = 24;

	private final JTextField field;          // 고르기만 하는 덮개면 null
	private final Rectangle card;
	private final int buttonTop;
	private final String title;
	private final String[] buttons;
	private final Consumer<String> onDone;
	private boolean done = false;   // 확인/취소는 한 번만

	/** 글자 한 줄을 받는 덮개. 취소하면 null 이 간다. */
	InputOverlay(String title, String initial, Consumer<String> onDone) {
		this(title, initial == null ? "" : initial, new String[] { "확인", "취소" }, onDone);
	}

	/** 둘 중 하나를 고르는 덮개. 고른 글자가 그대로 넘어오고, Esc 면 null 이 간다. */
	InputOverlay(String title, String[] buttons, Consumer<String> onDone) {
		this(title, null, buttons, onDone);
	}

	private InputOverlay(String title, String initial, String[] buttons, Consumer<String> onDone) {
		this.title = title;
		this.buttons = buttons;
		this.onDone = onDone;
		this.field = initial == null ? null : new JTextField(initial);
		this.buttonTop = field == null ? NO_FIELD_TOP : WITH_FIELD_TOP;
		this.card = new Rectangle(CARD_X, CARD_Y, CARD_W,
				field == null ? CARD_H - CARD_SHRINK : CARD_H);
		setOpaque(false);   // 뒤 화면이 비쳐야 한다

		if (field != null) {
			field.setHorizontalAlignment(JTextField.CENTER);
			field.setBackground(Color.WHITE);
			field.setForeground(Color.BLACK);
			field.setCaretColor(Color.BLACK);
			field.addActionListener(e -> finish(field.getText()));   // Enter
			add(field);
		} else {
			// 입력 칸이 없으면 Enter 를 받아 줄 곳이 없다. 왼쪽 버튼을 기본으로 둔다.
			Ui.bind(this, KeyEvent.VK_ENTER, "pickFirst", () -> finish(buttons[0]));
		}

		Ui.bind(this, KeyEvent.VK_ESCAPE, "inputCancel", () -> finish(null));
		onClick(button(0), () -> finish(field == null ? buttons[0] : field.getText()));
		onClick(button(1), () -> finish(field == null ? buttons[1] : null));
	}

	/** 덮개가 올라오면 바로 글자를 칠 수 있게 한다. */
	@Override
	public void addNotify() {
		super.addNotify();
		if (field != null)
			SwingUtilities.invokeLater(field::requestFocusInWindow);
	}

	private void finish(String value) {
		if (done)
			return;
		done = true;
		onDone.accept(value);
	}

	/** 두 버튼을 한 덩어리로 보고 카드 가운데에 놓는다. (GAME OVER 덮개와 같은 규칙) */
	private Rectangle button(int index) {
		int total = BUTTON_W * 2 + BUTTON_GAP;
		int x = card.x + card.width / 2 - total / 2 + index * (BUTTON_W + BUTTON_GAP);
		return new Rectangle(x, buttonTop, BUTTON_W, BUTTON_H);
	}

	@Override
	void layoutDesign() {
		if (field == null)
			return;
		field.setBounds(screen(FIELD));
		field.setFont(font(TEXT_SIZE));
		field.setBorder(BorderFactory.createLineBorder(Color.BLACK, Math.max(1, sz(FIELD_BORDER))));
	}

	@Override
	void paintDesign(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		fill(g, card, Ui.CREAM);
		centeredOnCard(g, title, TITLE_TOP, TITLE_SIZE);

		drawButton(g, button(0), buttons[0], true);
		drawButton(g, button(1), buttons[1], false);
	}

	private void centeredOnCard(Graphics2D g, String s, int designTop, float size) {
		g.setColor(Color.BLACK);
		g.setFont(font(size, TRACKING));
		int x = px(card.x + card.width / 2) - g.getFontMetrics().stringWidth(s) / 2;
		g.drawString(s, x, py(designTop) + g.getFontMetrics().getAscent());
	}

	/** Enter 로 바로 되는 왼쪽 버튼만 검게 채운다. */
	private void drawButton(Graphics2D g, Rectangle r, String label, boolean on) {
		if (on)
			fill(g, r, Color.BLACK);
		else
			outline(g, r, Color.BLACK, BUTTON_BORDER);

		g.setColor(on ? Ui.CREAM : Color.BLACK);
		g.setFont(font(TEXT_SIZE, TRACKING));
		int textW = g.getFontMetrics().stringWidth(label);
		int textX = px(r.x) + (sz(r.width) - textW) / 2;
		g.drawString(label, textX, centeredBaseline(g, r.y, r.height));

		if (!on || ARROW_ICON == null)
			return;

		int arrowW = sz(ARROW_W), arrowH = sz(ARROW_H);
		int arrowY = py(r.y) + (sz(r.height) - arrowH) / 2;
		g.drawImage(ARROW_ICON, textX - sz(ARROW_GAP) - arrowW, arrowY, arrowW, arrowH, this);
	}
}
