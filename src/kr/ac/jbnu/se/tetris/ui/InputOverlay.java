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
 * 글자 한 줄을 받는 덮개. JOptionPane 대신 쓴다. (랭킹 이름, 방장 IP)
 *
 * GAME OVER 덮개와 같은 크림색 카드 모양이다. Enter 로 확인, Esc 로 취소하고
 * 버튼을 눌러도 된다. 취소하면 null 을 넘긴다.
 */
class InputOverlay extends DesignPanel {

	private static final Rectangle CARD = new Rectangle(485, 452, 1029, 440);
	private static final Rectangle FIELD = new Rectangle(575, 620, 849, 104);

	private static final int TITLE_TOP = 520;
	private static final float TITLE_SIZE = 48f, TEXT_SIZE = 36f;
	private static final double TRACKING = 0.1;

	private static final int BUTTON_W = 280, BUTTON_H = 95, BUTTON_GAP = 61;
	private static final int BUTTON_TOP = 772, BUTTON_BORDER = 7;
	private static final int FIELD_BORDER = 4;

	private static final int ARROW_W = 21, ARROW_H = 39, ARROW_GAP = 24;

	private final JTextField field = new JTextField();
	private final String title;
	private final Consumer<String> onDone;
	private boolean done = false;   // 확인/취소는 한 번만

	InputOverlay(String title, String initial, Consumer<String> onDone) {
		this.title = title;
		this.onDone = onDone;
		setOpaque(false);   // 뒤 화면이 비쳐야 한다

		field.setText(initial == null ? "" : initial);
		field.setHorizontalAlignment(JTextField.CENTER);
		field.setBackground(Color.WHITE);
		field.setForeground(Color.BLACK);
		field.setCaretColor(Color.BLACK);
		field.addActionListener(e -> finish(field.getText()));   // Enter
		add(field);

		Ui.bind(this, KeyEvent.VK_ESCAPE, "inputCancel", () -> finish(null));
		onClick(button(0), () -> finish(field.getText()));
		onClick(button(1), () -> finish(null));
	}

	/** 덮개가 올라오면 바로 글자를 칠 수 있게 한다. */
	@Override
	public void addNotify() {
		super.addNotify();
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
		int x = CARD.x + CARD.width / 2 - total / 2 + index * (BUTTON_W + BUTTON_GAP);
		return new Rectangle(x, BUTTON_TOP, BUTTON_W, BUTTON_H);
	}

	@Override
	void layoutDesign() {
		field.setBounds(screen(FIELD));
		field.setFont(font(TEXT_SIZE));
		field.setBorder(BorderFactory.createLineBorder(Color.BLACK, Math.max(1, sz(FIELD_BORDER))));
	}

	@Override
	void paintDesign(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());

		fill(g, CARD, Ui.CREAM);
		centeredOnCard(g, title, TITLE_TOP, TITLE_SIZE);

		drawButton(g, button(0), "확인", true);
		drawButton(g, button(1), "취소", false);
	}

	private void centeredOnCard(Graphics2D g, String s, int designTop, float size) {
		g.setColor(Color.BLACK);
		g.setFont(font(size, TRACKING));
		int x = px(CARD.x + CARD.width / 2) - g.getFontMetrics().stringWidth(s) / 2;
		g.drawString(s, x, py(designTop) + g.getFontMetrics().getAscent());
	}

	/** Enter 로 바로 되는 확인 버튼만 검게 채운다. */
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
