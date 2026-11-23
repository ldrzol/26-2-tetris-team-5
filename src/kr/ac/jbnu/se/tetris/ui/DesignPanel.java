package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Board;
import kr.ac.jbnu.se.tetris.game.GameBoard;
import kr.ac.jbnu.se.tetris.game.Gimmick;
import kr.ac.jbnu.se.tetris.game.Tetrominoes;
import kr.ac.jbnu.se.tetris.save.Wallet;

import kr.ac.jbnu.se.tetris.Resources;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
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
 *
 * 아래쪽 "부품" 들은 Figma "리팩" 화면에서 되풀이되는 모양이다.
 * (어닝 상자, 타원 버튼, 제목 알약, 코인 알약, 어두운 카드, 키캡, 토글)
 */
abstract class DesignPanel extends JPanel {

	static final int DESIGN_W = 2000, DESIGN_H = 1422;

	/** 게임 위에 덮개를 띄울 때 깔리는 반투명 검정. (Figma rgba(29,29,29,0.32)) */
	static final Color SCRIM = new Color(29, 29, 29, 82);

	/**
	 * 불러온 그림을 경로별로 기억해 둔다. 그리는 중에 부르므로 매번 읽으면 안 된다.
	 *
	 * 아래 그림들이 이 표를 쓰면서 만들어지므로, 반드시 그보다 먼저 선언해야 한다.
	 */
	private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

	static final Image SKY = image("images/bg_sky.jpg");
	static final Image CAFE = image("images/bg_cafe.jpg");
	static final Image GEAR_ICON = image("images/ui_gear.png");
	static final Image SPOON_ICON = image("images/ui_spoon.png");
	static final Image COIN_ICON = image("images/ui_coin.png");
	static final Image OVAL = image("images/ui_oval.png");
	static final Image OVAL_SMALL = image("images/ui_oval_small.png");
	static final Image TITLE_LOGO = image("images/ui_title.png");

	/** 오른쪽 위 톱니 자리 - 게임 화면은 일시정지, 나머지는 환경 설정. */
	static final Rectangle GEAR = new Rectangle(1845, 46, 108, 108);
	/** 왼쪽 위 숟가락 - 뒤로 가기. (그림자까지 포함한 크기) */
	static final Rectangle BACK = new Rectangle(47, 44, 246, 79);
	/** 톱니가 있는 화면에서 코인 알약의 오른쪽 끝. */
	static final int COIN_RIGHT = 1815, COIN_TOP = 52;

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
			URL url = Resources.find(path);
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

	int sz(double designLength) {
		return (int) Math.round(designLength * scale);
	}

	int px(double designX) {
		return originX + sz(designX);
	}

	int py(double designY) {
		return originY + sz(designY);
	}

	/** 디자인 사각형을 화면 사각형으로 바꾼다. */
	Rectangle screen(Rectangle r) {
		return new Rectangle(px(r.x), py(r.y), sz(r.width), sz(r.height));
	}

	/** 상자 안쪽(테두리·어닝 아래)에 블록 미리보기를 그린다. */
	void preview(Graphics2D g, Rectangle area, Tetrominoes shape, int colorId) {
		Board.drawPreview(g, shape, colorId, screen(area));
	}

	/** 디자인 기준 글자 크기로 폰트를 만든다. */
	Font font(float designSize) {
		return Ui.font((float) (designSize * scale));
	}

	// 그림
	/** 배경 그림을 창에 꽉 차게 깐다. 비율은 유지하고, 넘치는 부분은 오른쪽·아래가 잘린다. */
	void background(Graphics2D g, Image img) {
		if (img == null)
			return;
		int iw = img.getWidth(this), ih = img.getHeight(this);
		if (iw <= 0 || ih <= 0)
			return;
		double k = Math.max(getWidth() / (double) iw, getHeight() / (double) ih);
		int w = (int) Math.ceil(iw * k), h = (int) Math.ceil(ih * k);
		g.drawImage(img, 0, (getHeight() - h) / 2, w, h, this);
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

	void gear(Graphics2D g) {
		drawImage(g, GEAR_ICON, GEAR);
	}

	/** 왼쪽 위 숟가락 (뒤로 가기). */
	void back(Graphics2D g) {
		drawImage(g, SPOON_ICON, BACK);
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

	/** 왼쪽 위 숟가락을 눌렀을 때. */
	void onBack(Runnable action) {
		onClick(BACK, action);
	}

	private void click(Point p) {
		// 나중에 등록한 영역이 위에 있다고 본다
		for (int i = clickAreas.size() - 1; i >= 0; i--)
			if (screen(clickAreas.get(i)).contains(p)) {
				clickActions.get(i).run();
				return;
			}
	}

	// 글자
	/** 주아체에 없는 기호(← · … − 등)를 대신 그릴 글꼴. */
	private static final Font FALLBACK = new Font(Font.DIALOG, Font.BOLD, 12);

	/**
	 * drawString 대신 쓴다. 지금 글꼴에 없는 글자는 그 부분만 FALLBACK 으로 그린다.
	 * (자바는 글꼴 파일에 없는 글자를 네모로 그려 버린다)
	 */
	void drawText(Graphics2D g, String s, int x, int baseline) {
		Font f = g.getFont();
		if (f.canDisplayUpTo(s) == -1) {
			g.drawString(s, x, baseline);
			return;
		}
		for (String[] run : runs(f, s)) {
			g.setFont(run[1] == null ? f : FALLBACK.deriveFont(f.getSize2D()));
			g.drawString(run[0], x, baseline);
			x += g.getFontMetrics().stringWidth(run[0]);
		}
		g.setFont(f);
	}

	/** drawText 로 그렸을 때의 화면 폭. */
	int stringWidth(Graphics2D g, String s) {
		Font f = g.getFont();
		if (f.canDisplayUpTo(s) == -1)
			return g.getFontMetrics().stringWidth(s);
		int w = 0;
		for (String[] run : runs(f, s))
			w += g.getFontMetrics(run[1] == null ? f : FALLBACK.deriveFont(f.getSize2D())).stringWidth(run[0]);
		return w;
	}

	/** 글꼴로 그릴 수 있는 조각과 없는 조각으로 나눈다. [1] 이 null 이면 그릴 수 있는 조각이다. */
	private static List<String[]> runs(Font f, String s) {
		List<String[]> out = new ArrayList<>();
		int i = 0;
		while (i < s.length()) {
			boolean ok = f.canDisplay(s.codePointAt(i));
			int j = i;
			while (j < s.length() && f.canDisplay(s.codePointAt(j)) == ok)
				j += Character.charCount(s.codePointAt(j));
			out.add(new String[] { s.substring(i, j), ok ? null : "fallback" });
			i = j;
		}
		return out;
	}

	/** 글자를 디자인 좌표 (x, top) 에 놓는다. top 은 글상자 위쪽. */
	void text(Graphics2D g, String s, double designX, double designTop, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		drawText(g, s, px(designX), py(designTop) + g.getFontMetrics().getAscent());
	}

	/** 글자 가운데를 designCenterX 에 맞춘다. */
	void textAt(Graphics2D g, String s, double designCenterX, double designTop, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		int x = px(designCenterX) - stringWidth(g, s) / 2;
		drawText(g, s, x, py(designTop) + g.getFontMetrics().getAscent());
	}

	/** 글자를 화면 가로 가운데에 놓는다. */
	void textCentered(Graphics2D g, String s, double designTop, float size, Color color) {
		textAt(g, s, DESIGN_W / 2.0, designTop, size, color);
	}

	/** 오른쪽 끝을 기준으로 글자를 놓는다. */
	void textRight(Graphics2D g, String s, double designRight, double designTop, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		int x = px(designRight) - stringWidth(g, s);
		drawText(g, s, x, py(designTop) + g.getFontMetrics().getAscent());
	}

	/** 상자 한가운데에 글자를 놓는다. */
	void textInBox(Graphics2D g, String s, Rectangle box, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		int x = px(box.x) + (sz(box.width) - stringWidth(g, s)) / 2;
		drawText(g, s, x, centeredBaseline(g, box.y, box.height));
	}

	/** 디자인 기준 글자 폭. */
	int textWidth(Graphics2D g, String s, float size) {
		g.setFont(font(size));
		return (int) Math.ceil(stringWidth(g, s) / scale);
	}

	/** 디자인상 세로 범위 한가운데에 글자가 오도록 기준선을 구한다. */
	int centeredBaseline(Graphics2D g, int designY, int designHeight) {
		FontMetrics fm = g.getFontMetrics();
		return py(designY) + (sz(designHeight) + fm.getAscent() - fm.getDescent()) / 2;
	}

	/** 남은 시간을 mm:ss 로. 0 아래로는 내려가지 않는다. */
	static String mmss(int seconds) {
		int s = Math.max(0, seconds);
		return String.format("%02d:%02d", s / 60, s % 60);
	}

	// 부품 - 모양
	/** 디자인 좌표의 둥근 사각형을 화면 좌표 도형으로. */
	private Shape rr(double x, double y, double w, double h, double radius) {
		double r = Math.min(radius, Math.min(w, h) / 2);
		return new RoundRectangle2D.Double(px(x), py(y), sz(w), sz(h), sz(r * 2), sz(r * 2));
	}

	/**
	 * Figma 상자 하나. 테두리는 상자 '안쪽'에 그려지고, 그림자는 아래로 밀린 같은 모양이
	 * 상자 밖으로 삐져나온 부분만 보인다. (CSS box-shadow 와 같다)
	 *
	 * fill / border 가 null 이면 그 부분을 건너뛴다.
	 */
	void box(Graphics2D g, Rectangle r, double radius, Color fill, Color border, double borderW,
			double shadowDy, Color shadow) {
		Shape outer = rr(r.x, r.y, r.width, r.height, radius);
		if (shadow != null && shadowDy > 0) {
			Area s = new Area(rr(r.x, r.y + shadowDy, r.width, r.height, radius));
			s.subtract(new Area(outer));
			g.setColor(shadow);
			g.fill(s);
		}
		Shape inner = rr(r.x + borderW, r.y + borderW, r.width - borderW * 2, r.height - borderW * 2,
				Math.max(0, radius - borderW));
		if (fill != null) {
			g.setColor(fill);
			g.fill(border == null ? outer : inner);
		}
		if (border != null && borderW > 0) {
			Area ring = new Area(outer);
			ring.subtract(new Area(inner));
			g.setColor(border);
			g.fill(ring);
		}
	}

	/** 반투명 크림 상자 (Hold / Next / Score 등). */
	void panel(Graphics2D g, Rectangle r) {
		box(g, r, 15, Ui.PANEL_FILL, Ui.BROWN, 10, 19, Ui.SHADOW);
	}

	/** 일시정지 / 결과 카드. */
	void darkCard(Graphics2D g, Rectangle r) {
		box(g, r, 41, Ui.DARK_CARD, Ui.DARK_CARD_BORDER, 17, 7, Ui.SHADOW);
	}

	/** 덮개 바탕 전체를 어둡게. */
	void scrim(Graphics2D g) {
		g.setColor(SCRIM);
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	/**
	 * 가게 차양(어닝). 빨강·하양 줄이 번갈아 있고 아래 끝이 반원이다.
	 * 줄 하나는 stripe 쯤의 폭이고, 첫 줄 왼쪽 위와 끝 줄 오른쪽 위만 둥글다.
	 */
	void awning(Graphics2D g, double x, double width, double top, double stripeW) {
		final double height = 117, cornerR = 29, shadowDy = 4;
		int n = Math.max(2, (int) Math.round(width / stripeW));
		double sw = width / n;
		for (int pass = 0; pass < 2; pass++) {             // 0 = 그림자, 1 = 줄
			for (int i = 0; i < n; i++) {
				double sx = x + i * sw, sy = top + (pass == 0 ? shadowDy : 0);
				Area stripe = new Area(new Rectangle2D.Double(px(sx), py(sy + cornerR), sz(sw) + 1,
						sz(height - sw / 2 - cornerR)));
				stripe.add(new Area(new Ellipse2D.Double(px(sx), py(sy + height - sw), sz(sw), sz(sw))));
				// 위쪽 끝: 바깥 두 줄만 둥근 모서리
				double rl = i == 0 ? cornerR : 0, rRight = i == n - 1 ? cornerR : 0;
				stripe.add(new Area(topRounded(sx, sy, sw, cornerR * 2, rl, rRight)));
				g.setColor(pass == 0 ? Ui.AWNING_SHADOW : i % 2 == 0 ? Ui.AWNING : Color.WHITE);
				g.fill(stripe);
			}
		}
	}

	/** 위쪽 두 모서리만 둥근 사각형. */
	private Shape topRounded(double x, double y, double w, double h, double rl, double rr) {
		Path2D.Double p = new Path2D.Double();
		double X = px(x), Y = py(y), W = sz(w) + 1, H = sz(h), RL = sz(rl), RR = sz(rr);
		p.moveTo(X, Y + RL);
		p.quadTo(X, Y, X + RL, Y);
		p.lineTo(X + W - RR, Y);
		p.quadTo(X + W, Y, X + W, Y + RR);
		p.lineTo(X + W, Y + H);
		p.lineTo(X, Y + H);
		p.closePath();
		return p;
	}

	/** 어닝 위에 얹는 흰 이름표. 가운데 x 기준, 폭은 글자에 맞춰 늘어난다. */
	void label(Graphics2D g, double centerX, double top, String text) {
		float size = 48;
		int w = Math.max(197, textWidth(g, text, size) + 64);
		Rectangle r = new Rectangle((int) Math.round(centerX - w / 2.0), (int) Math.round(top), w, 68);
		box(g, r, 34, Color.WHITE, null, 0, 4, new Color(200, 0, 0, 64));
		textInBox(g, text, r, size, Ui.TEXT);
	}

	/**
	 * 어닝 상자 한 벌: 상자 + 차양 + 이름표.
	 * lift 는 차양 위쪽이 상자 위쪽보다 얼마나 위에 있는지다. (작은 상자 16, 큰 상자 59)
	 */
	void awningPanel(Graphics2D g, Rectangle r, String name, int lift) {
		panel(g, r);
		double top = r.y - lift;
		awning(g, r.x - 13, r.width + 26, top, lift > 16 ? 48 : 65);   // 큰 상자는 줄이 촘촘하다
		label(g, r.x + r.width / 2.0, top + 15, name);
	}

	/** 작은 어닝 상자 (게임 화면). */
	void awningPanel(Graphics2D g, Rectangle r, String name) {
		awningPanel(g, r, name, 16);
	}

	/** 어닝 아래로 보이는 상자 안쪽. 미리보기·글자를 놓는 자리다. */
	static Rectangle below(Rectangle r, int lift) {
		int top = r.y - lift + 117;
		return new Rectangle(r.x + 10, top, r.width - 20, r.y + r.height - 10 - top);
	}

	/**
	 * 손그림 타원 버튼. 그림에 그림자가 붙어 있어서, Figma 처럼 타원 자리보다
	 * 조금 크게 그린다.
	 */
	void oval(Graphics2D g, Rectangle r, String text, float size) {
		// 그림의 여백 비율 (Figma inset)
		drawImage(g, OVAL, new Rectangle((int) Math.round(r.x - r.width * 0.0268),
				(int) Math.round(r.y - r.height * 0.0532), (int) Math.round(r.width * 1.0541),
				(int) Math.round(r.height * 1.1524)));
		textInBox(g, text, r, size, Ui.TITLE_TEXT);
	}

	/** 작은 타원 버튼 (결과 카드의 다시하기 / 메인으로). 키보드로 고른 버튼은 글자가 빨갛다. */
	void smallOval(Graphics2D g, Rectangle r, String text, float size, boolean selected) {
		drawImage(g, OVAL_SMALL, new Rectangle((int) Math.round(r.x - r.width * 0.045),
				(int) Math.round(r.y - r.height * 0.0718), (int) Math.round(r.width * 1.0912),
				(int) Math.round(r.height * 1.205)));
		textInBox(g, text, r, size, selected ? Ui.RED : Color.BLACK);
	}

	/** 오른쪽을 가리키는 빨간 삼각형. (x = 오른쪽 꼭짓점, centerY = 세로 가운데) */
	void arrow(Graphics2D g, double x, double centerY, double size) {
		double h = size, w = size * 0.866;
		Path2D.Double p = new Path2D.Double();
		p.moveTo(px(x), py(centerY));
		p.lineTo(px(x - w), py(centerY - h / 2));
		p.lineTo(px(x - w), py(centerY + h / 2));
		p.closePath();
		g.setColor(Ui.ARROW_RED);
		g.fill(p);
	}

	/** 화면 위 가운데의 크림색 제목 알약. (환경 설정 / 랭킹 / 상점) */
	void titlePill(Graphics2D g, String text) {
		int w = textWidth(g, text, 48) + 134;
		Rectangle r = new Rectangle(DESIGN_W / 2 - w / 2, 24, w, 110);
		box(g, r, 55, Ui.CREAM, Ui.BROWN, 7, 8, new Color(168, 105, 63, 230));
		textInBox(g, text, new Rectangle(r.x, r.y + 2, r.width, r.height), 48, Ui.TEXT);
	}

	/** 메뉴 화면의 큰 제목. */
	void menuTitle(Graphics2D g, String text, int top) {
		textCentered(g, text, top, 128, Ui.TITLE_TEXT);
	}

	/** 코인 알약. right 는 알약 오른쪽 끝. 그린 사각형을 돌려준다. */
	Rectangle coins(Graphics2D g, int right, int top) {
		return coinPill(g, right, top, Wallet.coinText(), 42, 40, 84);
	}

	/** 가격 알약. (상점) */
	Rectangle coinPill(Graphics2D g, int right, int top, String value, int coin, float size, int height) {
		int w = 10 + 16 + coin + 12 + textWidth(g, value, size) + 26;
		Rectangle r = new Rectangle(right - w, top, w, height);
		box(g, r, height / 2.0, Ui.CREAM, Ui.BROWN, 5, 5, new Color(168, 105, 63, 230));
		drawImage(g, COIN_ICON, new Rectangle(r.x + 5 + 16, top + (height - coin) / 2, coin, coin));
		textInBox(g, value, new Rectangle(r.x + 5 + 16 + coin + 12, top, textWidth(g, value, size), height),
				size, Ui.TEXT);
		return r;
	}

	/** 키캡 한 칸. waiting 이면 새 키를 기다리는 칸이라 노랗게 빛난다. */
	void keycap(Graphics2D g, Rectangle r, String label, float size, boolean waiting) {
		if (waiting) {
			Rectangle ring = new Rectangle(r.x - 7, r.y - 7, r.width + 14, r.height + 14);
			box(g, ring, 19, new Color(0xF3A99A), null, 0, 0, null);
			box(g, r, 12, Ui.YELLOW, Ui.RED, 5, 0, null);
		} else {
			box(g, r, 12, Color.WHITE, Ui.BROWN, 4, 0, null);
		}
		textInBox(g, label, r, size, Ui.TEXT);
	}

	/** 작은 키캡 (안내 줄). 그림자가 있다. */
	void smallKeycap(Graphics2D g, Rectangle r, String label) {
		box(g, r, 8, Color.WHITE, Ui.BROWN, 3, 3, new Color(168, 105, 63, 204));
		textInBox(g, label, r, 20, Ui.TEXT);
	}

	/** 두 칸짜리 토글. (ON/OFF, 주간/전체) 고른 쪽만 진한 갈색으로 채운다. */
	void toggle(Graphics2D g, Rectangle r, String left, String right, boolean leftOn, float size) {
		box(g, r, r.height / 2.0, Color.WHITE, Ui.DARK_BROWN, 4, 0, null);
		int half = (r.width - 18) / 2;
		Rectangle a = new Rectangle(r.x + 9, r.y + 9, half, r.height - 18);
		Rectangle b = new Rectangle(r.x + 9 + half, r.y + 9, half, r.height - 18);
		box(g, leftOn ? a : b, a.height / 2.0, Ui.DARK_BROWN, null, 0, 0, null);
		textInBox(g, left, a, size, leftOn ? Color.WHITE : Ui.SUB_TEXT);
		textInBox(g, right, b, size, leftOn ? Ui.SUB_TEXT : Color.WHITE);
	}

	/** 토글의 왼쪽 / 오른쪽 반쪽. (마우스 영역) */
	static Rectangle toggleHalf(Rectangle r, boolean left) {
		int half = r.width / 2;
		return new Rectangle(left ? r.x : r.x + half, r.y, half, r.height);
	}

	/** 점선 가로줄. */
	void dashed(Graphics2D g, double x, double y, double w) {
		g.setColor(Ui.DASH);
		float unit = Math.max(1f, sz(6));
		g.setStroke(new BasicStroke(Math.max(1, sz(2)), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
				new float[] { unit, unit }, 0f));
		g.drawLine(px(x), py(y), px(x + w), py(y));
	}

	/** 실선 가로줄. */
	void solid(Graphics2D g, double x, double y, double w, Color color, double thick) {
		g.setColor(color);
		g.setStroke(new BasicStroke(Math.max(1, sz(thick))));
		g.drawLine(px(x), py(y), px(x + w), py(y));
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
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		paintDesign(g);
	}

	// 보드
	/**
	 * 디자인 자리(테두리 포함 상자) 안에 10 x VISIBLE_HEIGHT 칸을 정사각형으로 채워 넣은 화면 영역.
	 * 칸 크기의 정확한 배수라서 판이 이 영역에 꽉 들어맞는다.
	 */
	Rectangle boardBounds(Rectangle box) {
		Rectangle in = new Rectangle(box);
		in.grow(-15, -15);   // 상자 테두리 안쪽
		return fit(in, GameBoard.WIDTH, GameBoard.VISIBLE_HEIGHT);
	}

	/** 디자인 자리 안에 cols x rows 정사각형 칸을 가운데 맞춰 넣은 화면 영역. (칸 크기의 정확한 배수) */
	Rectangle fit(Rectangle slot, int cols, int rows) {
		int cell = Math.max(1, Math.min(sz(slot.width) / cols, sz(slot.height) / rows));
		int w = cell * cols, h = cell * rows;
		return new Rectangle(px(slot.x) + (sz(slot.width) - w) / 2, py(slot.y) + (sz(slot.height) - h) / 2, w, h);
	}

	/**
	 * 판(화면 좌표)에 꼭 맞춘 보드 상자. 테두리(15)를 판 바깥에 두르므로
	 * 판과 테두리 사이에 빈틈이 생기지 않는다.
	 */
	void boardFrame(Graphics2D g, Rectangle b, Color border) {
		int bw = Math.max(1, sz(15)), arc = sz(30), dy = sz(19);
		Shape outer = new RoundRectangle2D.Double(b.x - bw, b.y - bw, b.width + bw * 2, b.height + bw * 2, arc, arc);
		Area shadow = new Area(new RoundRectangle2D.Double(b.x - bw, b.y - bw + dy, b.width + bw * 2,
				b.height + bw * 2, arc, arc));
		shadow.subtract(new Area(outer));
		g.setColor(Ui.SHADOW);
		g.fill(shadow);
		g.setColor(border);
		g.fill(outer);
		g.setColor(Ui.BOARD_FILL);
		g.fill(b);
	}

	abstract void paintDesign(Graphics2D g);
}
