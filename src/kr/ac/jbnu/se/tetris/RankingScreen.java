package kr.ac.jbnu.se.tetris;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * 랭킹 화면. Figma "랭킹 화면".
 *
 * 메인에서 1인 모드를 고르면 먼저 이 화면이 뜨고, 시작하기를 눌러야 게임이 시작된다.
 * 표는 Ranking 이 저장해 둔 기록을 점수 순으로 보여 준다.
 *
 * 걸린 시간은 Ranking.Entry 에 없어서 "--:--" 로 둔다. 시간까지 남기려면 Entry 에
 * 칸을 늘리고 SinglePanel 에서 넘겨야 한다.
 */
class RankingScreen extends DesignPanel {

	private static final Image COIN_ICON = image("images/coin.png");

	// Figma "랭킹 화면" 수치 (디자인 기준 픽셀)
	private static final int TITLE_TOP = 39, TITLE_HEIGHT = 65;

	private static final float TAB_LABEL_SIZE = 36f;   // 랭킹 / 내 기록 탭
	private static final float ROW_SIZE = 40f;         // 닉네임, 뒤로, 시작하기
	private static final float SCORE_SIZE = 36f;
	private static final float TIME_SIZE = 32f;
	private static final float SMALL_SIZE = 30f;       // 표 머리, 순위, 보상
	private static final float HINT_SIZE = 24f;
	private static final float BIG_SIZE = 100f;        // 내 순위 숫자

	private static final Rectangle BACK = new Rectangle(73, 66, 232, 81);
	private static final Rectangle BACK_ARROW = new Rectangle(132, 88, 20, 37);
	private static final int BACK_TEXT_X = 179, BACK_TEXT_TOP = 79;

	private static final Rectangle TOP_COIN = new Rectangle(1546, 81, 51, 51);
	private static final int TOP_COIN_TEXT_X = 1629, TOP_COIN_TEXT_TOP = 78;

	private static final Rectangle RANK_TAB = new Rectangle(73, 201, 186, 62);
	private static final Rectangle WEEK_BUTTON = new Rectangle(1139, 193, 104, 70);
	private static final Rectangle ALL_BUTTON = new Rectangle(1243, 193, 104, 70);
	private static final Rectangle MY_TAB = new Rectangle(1411, 201, 207, 70);

	// 왼쪽 표
	private static final Rectangle LIST = new Rectangle(73, 263, 1274, 1081);
	private static final int HEAD_TOP = 326;
	private static final int RANK_X = 154, NICK_X = 265, SCORE_X = 874, TIME_X = 1157;
	private static final int HEAD_RANK_X = 156, HEAD_NICK_X = 284, HEAD_SCORE_X = 926, HEAD_TIME_X = 1182;
	private static final int RANK_W = 64, RANK_H = 52;
	private static final int[] ROW_TOP = { 397, 470, 538, 620, 688, 760, 828, 896, 968, 1036 };
	/** 점수와 시간은 닉네임보다 조금 아래에 있다 */
	private static final int VALUE_DROP = 5;
	/** 크림색 칸으로 강조하는 순위 수 */
	private static final int HIGHLIGHT_RANKS = 3;

	private static final int DIVIDER_X = 133, DIVIDER_W = 1154;
	private static final int[] DIVIDER_TOP = { 383, 606 };
	private static final int DIVIDER_THICK = 4;

	private static final int ELLIPSIS_X = 674, ELLIPSIS_TOP = 1075;
	private static final float ELLIPSIS_SIZE = 64f;

	private static final Rectangle ME_ROW_OUTER = new Rectangle(123, 1191, 1174, 125);
	private static final Rectangle ME_ROW_INNER = new Rectangle(136, 1204, 1149, 99);
	private static final int ME_RANK_X = 159, ME_NICK_X = 261, ME_SCORE_X = 870, ME_TIME_X = 1153;
	private static final int ME_RANK_TOP = 1233, ME_NICK_TOP = 1226, ME_VALUE_TOP = 1231;

	// 오른쪽 내 기록
	private static final Rectangle ME = new Rectangle(1411, 271, 514, 1073);
	private static final int ME_LABEL_X = 1452, ME_VALUE_RIGHT = 1885;
	private static final int ME_TITLE_TOP = 334;
	private static final int MY_RANK_TOP = 401, MY_RANK_UNIT_X = 1618, MY_RANK_UNIT_TOP = 454;
	private static final int ME_DIVIDER_X = 1443, ME_DIVIDER_W = 453;
	private static final int[] ME_DIVIDER_TOP = { 529, 754 };
	private static final int[] STAT_TOP = { 552, 619, 686 };
	private static final int REWARD_TITLE_TOP = 775;
	private static final int[] REWARD_TOP = { 842, 909, 976, 1043 };
	private static final int REWARD_COIN_X = 1727, REWARD_COIN_SIZE = 37, REWARD_COIN_DROP = 4;
	private static final int REWARD_VALUE_X = 1782;
	private static final int DEADLINE_X = 1493, DEADLINE_TOP = 1285;
	private static final Rectangle START = new Rectangle(1451, 1163, 434, 107);

	/** 기록이 없는 칸 */
	private static final String NO_NAME = "-", NO_SCORE = "------", NO_TIME = "--:--", NO_RANK = "--";

	private static final String[] STAT_LABELS = { "최고 점수", "걸린 시간", "횟수" };
	private static final String[] REWARD_LABELS = { "1위", "2~3위", "4~" + Ranking.MAX + "위", "참여" };
	private static final String[] REWARD_VALUES = {
			coins(1), coins(2), coins(4), coins(Ranking.MAX + 1) };

	private static String coins(int rank) {
		return String.format("%,d", Wallet.weeklyReward(rank));
	}

	private final String me = Ranking.clean(Settings.playerName);

	/** 주간 표인지 전체 표인지. 디자인 기본값은 주간이다. */
	private boolean weekly = true;

	private List<Ranking.Entry> entries = Ranking.inWeek(Ranking.thisWeek());

	/** 이번에 받은 지난주 보상 안내. 없으면 null. */
	private String rewardNotice = null;

	RankingScreen(Runnable onStart, Runnable onBack) {
		claimLastWeek();
		onClick(BACK, onBack);
		onClick(START, onStart);
		onClick(WEEK_BUTTON, () -> setWeekly(true));
		onClick(ALL_BUTTON, () -> setWeekly(false));

		Ui.bind(this, KeyEvent.VK_ENTER, "rankStart", onStart);
		Ui.bind(this, KeyEvent.VK_ESCAPE, "rankBack", onBack);
	}

	private void setWeekly(boolean on) {
		weekly = on;
		entries = on ? Ranking.inWeek(Ranking.thisWeek()) : Ranking.load();
		repaint();
	}

	/**
	 * 지난주 랭킹 보상을 받는다. 지난주에 기록이 있으면 등수에 맞는 코인을 주고,
	 * 같은 주의 보상은 다시 주지 않는다.
	 */
	private void claimLastWeek() {
		String week = Ranking.lastWeek();
		int rank = Ranking.rankIn(Ranking.inWeek(week), me);
		int got = Wallet.claimWeekly(week, rank);
		if (got <= 0)
			return;
		rewardNotice = "지난주 " + rank + "위 보상 +" + String.format("%,d", got) + " 코인";
		Sfx.play(Sfx.Sound.COIN);
	}

	// 내 기록
	/** 내 이름으로 남은 가장 좋은 기록의 줄 번호(0 부터). 없으면 -1. */
	private int myIndex() {
		for (int i = 0; i < entries.size(); i++)
			if (entries.get(i).name.equals(me))
				return i;
		return -1;
	}

	private String myRankText() {
		int i = myIndex();
		return i < 0 ? NO_RANK : String.format("%02d", i + 1);
	}

	private String myScoreText() {
		int i = myIndex();
		return i < 0 ? NO_SCORE : String.format("%06d", entries.get(i).score);
	}

	/** 내 이름으로 남은 기록 수. */
	private int myCount() {
		int n = 0;
		for (Ranking.Entry e : entries)
			if (e.name.equals(me))
				n++;
		return n;
	}

	// 그리기
	@Override
	void paintDesign(Graphics2D g) {
		textCenteredIn(g, "TETRIS", TITLE_TOP, TITLE_HEIGHT, TITLE_SIZE, Ui.CREAM);

		outline(g, BACK);
		drawImageFlipped(g, ARROW_ICON, BACK_ARROW);
		text(g, "뒤로", BACK_TEXT_X, BACK_TEXT_TOP, ROW_SIZE, Ui.CREAM);

		drawImage(g, COIN_ICON, TOP_COIN);
		text(g, Wallet.coinText(), TOP_COIN_TEXT_X, TOP_COIN_TEXT_TOP, ROW_SIZE, Ui.CREAM);

		tab(g, RANK_TAB, "랭킹", TAB_LABEL_SIZE);
		pill(g, WEEK_BUTTON, "주간", weekly);
		pill(g, ALL_BUTTON, "전체", !weekly);
		tab(g, MY_TAB, "내 기록", TAB_LABEL_SIZE);

		list(g);
		me(g);

		gear(g);
	}

	/** 주간 / 전체 버튼. 고른 쪽만 크림색으로 채운다. */
	private void pill(Graphics2D g, Rectangle r, String label, boolean on) {
		if (on)
			fill(g, r, Ui.CREAM);
		else
			outline(g, r, DIM_CREAM, BORDER);
		textInBox(g, label, r, TIME_SIZE, on ? Color.BLACK : DIM_CREAM);
	}

	private void list(Graphics2D g) {
		outline(g, LIST);

		text(g, "순위", HEAD_RANK_X, HEAD_TOP, SMALL_SIZE, DIM_CREAM);
		text(g, "닉네임", HEAD_NICK_X, HEAD_TOP, SMALL_SIZE, DIM_CREAM);
		text(g, "점수", HEAD_SCORE_X, HEAD_TOP, SMALL_SIZE, DIM_CREAM);
		text(g, "시간", HEAD_TIME_X, HEAD_TOP, SMALL_SIZE, DIM_CREAM);

		for (int top : DIVIDER_TOP)
			divider(g, DIVIDER_X, top, DIVIDER_W);

		for (int i = 0; i < ROW_TOP.length; i++)
			row(g, i);

		text(g, "...", ELLIPSIS_X, ELLIPSIS_TOP, ELLIPSIS_SIZE, Ui.CREAM);

		// 내 기록 줄은 표 아래에 두 겹 테두리로 따로 둔다
		outline(g, ME_ROW_OUTER);
		outline(g, ME_ROW_INNER);
		text(g, myRankText(), ME_RANK_X, ME_RANK_TOP, SMALL_SIZE, Ui.CREAM);
		text(g, me + "(PLAYER)", ME_NICK_X, ME_NICK_TOP, ROW_SIZE, Ui.CREAM);
		text(g, myScoreText(), ME_SCORE_X, ME_VALUE_TOP, SCORE_SIZE, Color.WHITE);
		text(g, NO_TIME, ME_TIME_X, ME_VALUE_TOP, TIME_SIZE, Ui.CREAM);
	}

	/** 표 한 줄. 상위 세 줄은 순위 칸을 크림색으로 채우고, 빈 자리는 줄표로 둔다. */
	private void row(Graphics2D g, int index) {
		int top = ROW_TOP[index];
		Rectangle rank = new Rectangle(RANK_X, top, RANK_W, RANK_H);
		boolean highlight = index < HIGHLIGHT_RANKS;
		if (highlight)
			fill(g, rank, Ui.CREAM);
		textInBox(g, String.valueOf(index + 1), rank, SMALL_SIZE, highlight ? Color.BLACK : Ui.CREAM);

		boolean has = index < entries.size();
		String name = has ? entries.get(index).name : NO_NAME;
		String score = has ? String.format("%06d", entries.get(index).score) : NO_SCORE;

		text(g, name, NICK_X, top, ROW_SIZE, Ui.CREAM);
		text(g, score, SCORE_X, top + VALUE_DROP, SCORE_SIZE, has ? Color.WHITE : DIM_CREAM);
		text(g, NO_TIME, TIME_X, top + VALUE_DROP, TIME_SIZE, Ui.CREAM);
	}

	/** 표를 가르는 가로줄. 디자인의 SVG 선을 같은 두께의 직선으로 그린다. */
	private void divider(Graphics2D g, int designX, int designTop, int designWidth) {
		g.setColor(Ui.CREAM);
		g.setStroke(new BasicStroke(Math.max(1, sz(DIVIDER_THICK))));
		g.drawLine(px(designX), py(designTop), px(designX + designWidth), py(designTop));
	}

	private void me(Graphics2D g) {
		outline(g, ME);

		text(g, "랭킹", ME_LABEL_X, ME_TITLE_TOP, SMALL_SIZE, DIM_CREAM);
		text(g, myRankText(), ME_LABEL_X, MY_RANK_TOP, BIG_SIZE, Color.WHITE);
		text(g, "위", MY_RANK_UNIT_X, MY_RANK_UNIT_TOP, ROW_SIZE, Ui.CREAM);

		for (int top : ME_DIVIDER_TOP)
			divider(g, ME_DIVIDER_X, top, ME_DIVIDER_W);

		// 값 칸은 디자인에 비어 있어서 오른쪽 끝에 맞춰 넣는다
		String[] values = { myScoreText(), NO_TIME, String.valueOf(myCount()) };
		for (int i = 0; i < STAT_LABELS.length; i++) {
			text(g, STAT_LABELS[i], ME_LABEL_X, STAT_TOP[i], SMALL_SIZE, DIM_CREAM);
			textRight(g, values[i], ME_VALUE_RIGHT, STAT_TOP[i], SMALL_SIZE, Ui.CREAM);
		}

		text(g, "주간 보상", ME_LABEL_X, REWARD_TITLE_TOP, SMALL_SIZE, DIM_CREAM);
		for (int i = 0; i < REWARD_LABELS.length; i++) {
			text(g, REWARD_LABELS[i], ME_LABEL_X, REWARD_TOP[i], SMALL_SIZE, Ui.CREAM);
			drawImage(g, COIN_ICON, new Rectangle(REWARD_COIN_X, REWARD_TOP[i] + REWARD_COIN_DROP,
					REWARD_COIN_SIZE, REWARD_COIN_SIZE));
			text(g, REWARD_VALUES[i], REWARD_VALUE_X, REWARD_TOP[i], SMALL_SIZE, Ui.CREAM);
		}

		// 보상을 막 받았으면 그 안내를, 아니면 마감까지 남은 시간을 적는다 (자리가 한 줄뿐이다)
		if (rewardNotice != null)
			text(g, rewardNotice, DEADLINE_X, DEADLINE_TOP, HINT_SIZE, Color.WHITE);
		else
			text(g, Ranking.deadlineText(), DEADLINE_X, DEADLINE_TOP, HINT_SIZE, DIM_CREAM);

		fill(g, START, Ui.CREAM);
		textInBox(g, "시작하기", START, ROW_SIZE, Color.BLACK);
	}

	/** 오른쪽 끝을 기준으로 글자를 놓는다. */
	private void textRight(Graphics2D g, String s, int designRight, int designTop, float size, Color color) {
		g.setColor(color);
		g.setFont(font(size));
		int x = px(designRight) - g.getFontMetrics().stringWidth(s);
		g.drawString(s, x, py(designTop) + g.getFontMetrics().getAscent());
	}
}
