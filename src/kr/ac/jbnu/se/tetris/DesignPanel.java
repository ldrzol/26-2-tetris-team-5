package kr.ac.jbnu.se.tetris;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Figma 화면(2000x1422)을 그대로 옮기기 위한 공통 바탕.
 *
 * 디자인이 절대 좌표로 되어 있어서 레이아웃 매니저로 쪼개지 않고 디자인 좌표를
 * 그대로 적는다. px()/py()/sz() 가 창 크기에 맞춰 비율만 바꿔 주고, 남는 여백은
 * 가운데 정렬된다. 자식 화면은 paintDesign() 안에서 디자인 좌표로만 그리면 된다.
 */
abstract class DesignPanel extends JPanel {

	static final int DESIGN_W = 2000, DESIGN_H = 1422;

	/** 상자 테두리 굵기 (디자인 기준) */
	static final int BORDER = 6;

	/** 오른쪽 위 톱니 아이콘 자리 - 모든 화면에 공통이다. */
	static final Rectangle GEAR = new Rectangle(1856, 66, 78, 78);

	static final Color DIM_CREAM = new Color(0x8B857A);

	/** 게임 위에 덮개를 띄울 때 깔리는 반투명 검정. (디자인 0.61) */
	static final Color SCRIM = new Color(11, 11, 11, 156);

	/** 게임 화면에서 되풀이되는 글자 크기 (디자인 기준) */
	static final float TITLE_SIZE = 48f, TAB_SIZE = 40f, VALUE_SIZE = 48f;

	/** Score / Time 상자 안 숫자의 오른쪽 여백 */
	static final int VALUE_PAD = 27;


	/**
	 * 불러온 그림을 경로별로 기억해 둔다. 그리는 중에 부르므로 매번 읽으면 안 된다.
	 *
	 * 아래 아이콘들이 이 표를 쓰면서 만들어지므로, 반드시 그보다 먼저 선언해야 한다.
	 */
	private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

	static final Image GEAR_ICON = image("images/gear.png");
	static final Image ARROW_ICON = image("images/arrow.png");

	private double scale = 1;
	private int originX, originY;

	DesignPanel() {
		setLayout(null);   // 디자인 좌표로 직접 배치한다
		setBackground(Ui.BG);
	}

	/** 없는 경로면 null 을 돌려준다. (drawImage 가 null 을 그냥 건너뛴다) */
	static Image image(String path) {
		if (path == null)
			return null;
		if (!IMAGE_CACHE.containsKey(path)) {   // 못 찾은 경로도 기억해서 다시 찾지 않는다
			URL url = DesignPanel.class.getResource(path);
			IMAGE_CACHE.put(path, url == null ? null : new ImageIcon(url).getImage());
		}
		return IMAGE_CACHE.get(path);
	}

	// 좌표 변환
	/** 디자인 전체가 창 안에 들어가도록 배율을 정하고, 남는 여백만큼 가운데로 민다. */
	private void updateTransform() {
		scale = Math.min(getWidth() / (double) DESIGN_W, getHeight() / (double) DESIGN_H);
		originX = (int) Math.round((getWidth() - DESIGN_W * scale) / 2);
		originY = (int) Math.round((getHeight() - DESIGN_H * scale) / 2);
	}

	int sz(int designLength) {
		return (int) Math.round(designLength * scale);
	}

	int px(int designX) {
		return originX + sz(designX);
	}

	int py(int designY) {
		return originY + sz(designY);
	}

	/** 디자인 사각형을 화면 사각형으로 바꾼다. */
	Rectangle screen(Rectangle r) {
		return new Rectangle(px(r.x), py(r.y), sz(r.width), sz(r.height));
	}

	/** 상자 테두리 안쪽에 블록 미리보기를 그린다. */
	void preview(Graphics2D g, Rectangle box, Tetrominoes shape, int colorId) {
		Rectangle inner = screen(box);
		int pad = sz(BORDER);
		inner.grow(-pad, -pad);
		Board.drawPreview(g, shape, colorId, inner);
	}

	/** 디자인 기준 글자 크기로 폰트를 만든다. tracking 은 글자 크기 대비 자간 비율. */
	Font font(float designSize, double tracking) {
		Font f = Ui.font((float) (designSize * scale));
		if (tracking == 0)
			return f;
		Map<TextAttribute, Object> attrs = new HashMap<>();
		attrs.put(TextAttribute.TRACKING, tracking);
		return f.deriveFont(attrs);
	}

	Font font(float designSize) {
		return font(designSize, 0);
	}

	// 공통 부품
	/** 배경 그림을 화면에 꽉 차게 깐다. (비율 유지, 넘치는 부분은 잘림) */
	void background(Graphics2D g, Image img) {
		if (img == null)
			return;
		int iw = img.getWidth(this), ih = img.getHeight(this);
		if (iw <= 0 || ih <= 0)
			return;
		double k = Math.max(getWidth() / (double) iw, getHeight() / (double) ih);
		int w = (int) Math.ceil(iw * k), h = (int) Math.ceil(ih * k);
		g.drawImage(img, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, this);
	}

	void drawImage(Graphics2D g, Image img, Rectangle r) {
		if (img != null)
			g.drawImage(img, px(r.x), py(r.y), sz(r.width), sz(r.height), this);
	}

	/**
	 * 켜져 있는 기믹을 아이콘 한 줄로 그린다. 그림이 없는 기믹은 자리를 비우지 않고 건너뛴다.
	 *
	 * x, y, size, gap 은 모두 디자인 기준 픽셀이다.
	 */
	void gimmickIcons(Graphics2D g, Gimmick gimmick, int x, int y, int size, int gap) {
		for (String path : gimmick.activeIcons()) {
			drawImage(g, image(path), new Rectangle(x, y, size, size));
			x += size + gap;
		}
	}

	/** 좌우를 뒤집어 그린다. 오른쪽 화살표 그림을 왼쪽(뒤로) 화살표로 쓴다. */
	void drawImageFlipped(Graphics2D g, Image img, Rectangle r) {
		if (img == null)
			return;
		int x = px(r.x), y = py(r.y);
		g.drawImage(img, x + sz(r.width), y, x, y + sz(r.height),
				0, 0, img.getWidth(this), img.getHeight(this), this);
	}

	void gear(Graphics2D g) {
		drawImage(g, GEAR_ICON, GEAR);
	}

	// 마우스
	private List<Rectangle> clickAreas;
	private List<Runnable> clickActions;

	/** 디자인 좌표의 영역을 누르면 할 일. 생성자에서 한 번만 등록한다. */
	void onClick(Rectangle designArea, Runnable action) {
		if (clickAreas == null) {
			clickAreas = new ArrayList<>();
			clickActions = new ArrayList<>();
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					click(e.getPoint());
				}
			});
		}
		clickAreas.add(designArea);
		clickActions.add(action);
	}

	/** 오른쪽 위 톱니를 눌렀을 때. */
	void onGear(Runnable action) {
		onClick(GEAR, action);
	}

	private void click(Point p) {
		// 나중에 등록한 영역이 위에 있다고 본다
		for (int i = clickAreas.size() - 1; i >= 0; i--)
			if (screen(clickAreas.get(i)).contains(p)) {
				clickActions.get(i).run();
				return;
			}
	}

	/**
	 * 테두리만 있는 상자. 디자인에서 테두리는 상자 '안쪽'에 그려지므로 선을 반 굵기만큼
	 * 안으로 들인다. 그래야 상자의 바깥 면이 위에 붙는 탭의 가장자리와 정확히 맞는다.
	 */
	void outline(Graphics2D g, Rectangle r, Color color, int designWidth) {
		int w = Math.max(1, sz(designWidth));
		g.setColor(color);
		g.setStroke(new BasicStroke(w));
		g.drawRect(px(r.x) + w / 2, py(r.y) + w / 2, sz(r.width) - w, sz(r.height) - w);
	}

	void outline(Graphics2D g, Rectangle r) {
		outline(g, r, Ui.CREAM, BORDER);
	}

	void fill(Graphics2D g, Rectangle r, Color color) {
		g.setColor(color);
		g.fillRect(px(r.x), py(r.y), sz(r.width), sz(r.height));
	}

	/** 상자 위에 붙는 크림색 라벨. 글자는 탭 안에서 가운데 정렬. */
	void tab(Graphics2D g, Rectangle r, String label, float size) {
		fill(g, r, Ui.CREAM);
		textInBox(g, label, r, size, Color.BLACK);
	}

	/** 상자 한가운데에 글자를 놓는다. */
	void textInBox(Graphics2D g, String s, Rectangle box, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		int x = px(box.x) + (sz(box.width) - g.getFontMetrics().stringWidth(s)) / 2;
		g.drawString(s, x, centeredBaseline(g, box.y, box.height));
	}

	/** 글자를 디자인 좌표 (x, top) 에 놓는다. top 은 글상자 위쪽. */
	void text(Graphics2D g, String s, int designX, int designTop, float size, Color color) {
		text(g, s, designX, designTop, size, color, 0);
	}

	void text(Graphics2D g, String s, int designX, int designTop, float size, Color color, double tracking) {
		g.setColor(color);
		g.setFont(font(size, tracking));
		g.drawString(s, px(designX), py(designTop) + g.getFontMetrics().getAscent());
	}

	/** 글자를 가로 가운데에 놓는다. */
	void textCentered(Graphics2D g, String s, int designTop, float size, Color color, double tracking) {
		g.setColor(color);
		g.setFont(font(size, tracking));
		int x = px(DESIGN_W / 2) - g.getFontMetrics().stringWidth(s) / 2;
		g.drawString(s, x, py(designTop) + g.getFontMetrics().getAscent());
	}

	/** 글자를 가로 가운데 + 주어진 세로 범위의 가운데에 놓는다. (TETRIS 제목, VS) */
	void textCenteredIn(Graphics2D g, String s, int designTop, int designHeight, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		int x = px(DESIGN_W / 2) - g.getFontMetrics().stringWidth(s) / 2;
		g.drawString(s, x, centeredBaseline(g, designTop, designHeight));
	}

	/** 남은 시간을 mm:ss 로. 0 아래로는 내려가지 않는다. */
	static String mmss(int seconds) {
		int s = Math.max(0, seconds);
		return String.format("%02d:%02d", s / 60, s % 60);
	}

	/** 타이틀용 세로 그라데이션 글자. (크림색 -> 흐린 회갈색) */
	void gradientTextCentered(Graphics2D g, String s, int designTop, float size, double tracking) {
		g.setFont(font(size, tracking));
		FontMetrics fm = g.getFontMetrics();
		int top = py(designTop);
		int x = px(DESIGN_W / 2) - fm.stringWidth(s) / 2;
		g.setPaint(new GradientPaint(0, top, Ui.CREAM, 0, top + fm.getHeight(), DIM_CREAM));
		g.drawString(s, x, top + fm.getAscent());
		g.setPaint(Ui.CREAM);
	}

	/** 상자 안에 오른쪽 정렬로 넣는 흰 숫자. (Score / Time) */
	void valueRight(Graphics2D g, Rectangle box, String value, float size, int padRight) {
		g.setColor(Color.WHITE);
		g.setFont(font(size));
		int right = px(box.x) + sz(box.width) - sz(padRight);
		g.drawString(value, right - g.getFontMetrics().stringWidth(value),
				centeredBaseline(g, box.y, box.height));
	}

	/** 상자 안 가운데에 넣는 흰 숫자. (대전 화면의 Time) */
	void valueCentered(Graphics2D g, Rectangle box, String value, float size) {
		g.setColor(Color.WHITE);
		g.setFont(font(size));
		int x = px(box.x) + (sz(box.width) - g.getFontMetrics().stringWidth(value)) / 2;
		g.drawString(value, x, centeredBaseline(g, box.y, box.height));
	}

	/** 디자인상 세로 범위 한가운데에 글자가 오도록 기준선을 구한다. */
	int centeredBaseline(Graphics2D g, int designY, int designHeight) {
		FontMetrics fm = g.getFontMetrics();
		return py(designY) + (sz(designHeight) + fm.getAscent() - fm.getDescent()) / 2;
	}

	// 그리기 진입점
	@Override
	public void doLayout() {
		updateTransform();
		layoutDesign();
	}

	/** 자식 컴포넌트(보드 등)를 디자인 좌표로 배치한다. 없으면 비워 둔다. */
	void layoutDesign() {
	}

	@Override
	protected final void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		updateTransform();

		Graphics2D g = (Graphics2D) g0;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		paintDesign(g);
	}

	// 보드
	/**
	 * 디자인 자리 안에 10 x VISIBLE_HEIGHT 칸을 정사각형으로 채워 넣은 영역.
	 * designCell 은 칸 하나의 디자인 기준 크기이고, 자리에 안 들어가면 그만큼 줄어든다.
	 */
	Rectangle boardBounds(Rectangle slot, int designCell) {
		int fit = Math.min(sz(slot.width) / GameBoard.WIDTH, sz(slot.height) / GameBoard.VISIBLE_HEIGHT);
		int cell = Math.max(1, Math.min(sz(designCell), fit));
		int w = cell * GameBoard.WIDTH, h = cell * GameBoard.VISIBLE_HEIGHT;
		return new Rectangle(px(slot.x) + (sz(slot.width) - w) / 2,
				py(slot.y) + (sz(slot.height) - h) / 2, w, h);
	}

	/**
	 * 보드 테두리. 선 굵기의 절반만큼 바깥으로 물려서, 선의 안쪽 면이 보드 가장자리와
	 * 정확히 맞닿게 한다. 안으로 들이면 맨 바깥 블록을 덮어 버린다.
	 */
	void boardFrame(Graphics2D g, Rectangle b) {
		int w = Math.max(1, sz(BORDER));
		g.setColor(Ui.CREAM);
		g.setStroke(new BasicStroke(w));
		g.drawRect(b.x - w / 2, b.y - w / 2, b.width + w, b.height + w);
	}

	abstract void paintDesign(Graphics2D g);
}
