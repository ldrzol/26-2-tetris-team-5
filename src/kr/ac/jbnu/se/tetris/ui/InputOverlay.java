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
 * 결과 카드와 같은 어두운 카드 모양이다. Enter 로 왼쪽 버튼, Esc 로 취소하고
 * 버튼을 눌러도 된다. 취소하면 null 을 넘긴다.
 */
class InputOverlay extends DesignPanel {

	private static final int CARD_X = 485, CARD_Y = 452, CARD_W = 1029;
	private static final Rectangle FIELD = new Rectangle(575, 600, 849, 104);

	private static final int TITLE_TOP = 515;
	private static final float TITLE_SIZE = 48f, TEXT_SIZE = 36f;

	private static final int BUTTON_W = 281, BUTTON_H = 131;
	private static final int[] BUTTON_X = { 689, 1030 };
	private static final int FIELD_BORDER = 5;

	/** 입력 칸이 없으면 그 높이만큼 카드가 줄고 버튼이 올라온다. */
	private static final int WITH_FIELD_TOP = 740, NO_FIELD_TOP = 610;
	private static final int CARD_H = 470, CARD_SHRINK = WITH_FIELD_TOP - NO_FIELD_TOP;

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
			field.setForeground(Ui.TEXT);
			field.setCaretColor(Ui.TEXT);
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

	private Rectangle button(int index) {
		return new Rectangle(BUTTON_X[index], buttonTop, BUTTON_W, BUTTON_H);
	}

	@Override
	void layoutDesign() {
		if (field == null)
			return;
		field.setBounds(screen(FIELD));
		field.setFont(font(TEXT_SIZE));
		field.setBorder(BorderFactory.createLineBorder(Ui.BROWN, Math.max(1, sz(FIELD_BORDER))));
	}

	@Override
	void paintDesign(Graphics2D g) {
		scrim(g);
		darkCard(g, card);
		textCentered(g, title, TITLE_TOP, TITLE_SIZE, Color.WHITE);

		// Enter 로 바로 되는 왼쪽 버튼에 화살표를 붙인다
		smallOval(g, button(0), buttons[0], TEXT_SIZE, true);
		smallOval(g, button(1), buttons[1], TEXT_SIZE, false);
	}
}
