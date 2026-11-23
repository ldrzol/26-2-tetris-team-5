package kr.ac.jbnu.se.tetris.ui;

import kr.ac.jbnu.se.tetris.game.Sfx;
import kr.ac.jbnu.se.tetris.save.Ranking;
import kr.ac.jbnu.se.tetris.save.Settings;
import kr.ac.jbnu.se.tetris.save.Wallet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * 랭킹 화면. Figma "리팩 - 랭킹 화면".
 *
 * 게임 선택에서 1인 모드를 고르면 먼저 이 화면이 뜨고, 시작하기를 눌러야 게임이 시작된다.
 * 표는 Ranking 이 저장해 둔 기록을 점수 순으로 보여 준다.
 *
 * 걸린 시간은 Ranking.Entry 에 없어서 "--:--" 로 둔다. 시간까지 남기려면 Entry 에
 * 칸을 늘리고 SinglePanel 에서 넘겨야 한다. 디자인의 순위 변동(▲6)도 같은 이유로 뺐다.
 */
class RankingScreen extends DesignPanel {

	private static final int LIFT = 71;   // 어닝이 상자보다 71 위에서 시작한다

	private static final Rectangle LIST = new Rectangle(97, 341, 1285, 955);
	private static final Rectangle ME = new Rectangle(1428, 341, 468, 955);

	// 랭킹 표 (Figma "랭킹 표")
	private static final int TABLE_X = 140, TABLE_W = 1200;
	private static final Rectangle WEEK_TOGGLE = new Rectangle(1114, 405, 226, 68);
	private static final int HEAD_TOP = 479, HEAD_H = 46;
	private static final int RANK_X = 152, NICK_X = 312, SCORE_RIGHT = 1128, TIME_RIGHT = 1328;
	private static final int ROW_TOP = HEAD_TOP + HEAD_H, ROW_H = 60, ROWS = 10;
	private static final int ELLIPSIS_TOP = 1125;
	private static final Rectangle ME_ROW = new Rectangle(140, 1162, 1200, 64);
	private static final Color[] MEDAL = { new Color(0xFFD45A), new Color(0xDDE1E6), new Color(0xE3A36B) };
	private static final Color PLAIN_RING = new Color(0xE8C9B6);

	// 내 기록 (Figma "내 기록 내용")
	private static final int ME_X = 1462, ME_W = 400, ME_CENTER = ME_X + ME_W / 2;
	private static final int ME_RIGHT = ME_X + ME_W;
	private static final int[] STAT_TOP = { 616, 670, 724 };
	private static final int REWARD_TITLE_TOP = 806;
	private static final int[] REWARD_TOP = { 841, 887, 933, 979 };
	private static final Rectangle START = new Rectangle(1462, 1130, 400, 88);
	private static final int DEADLINE_TOP = 1232;

	/** 기록이 없는 칸 */
	private static final String NO_NAME = "-", NO_SCORE = "------", NO_TIME = "--:--", NO_RANK = "--";

	private static final String[] STAT_LABELS = { "최고 점수", "걸린 시간", "플레이 횟수" };
	private static final String[] REWARD_LABELS = { "1위", "2–3위", "4–" + Ranking.MAX + "위", "참여" };
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
		onBack(onBack);
		onClick(START, onStart);
		onClick(toggleHalf(WEEK_TOGGLE, true), () -> setWeekly(true));
		onClick(toggleHalf(WEEK_TOGGLE, false), () -> setWeekly(false));

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
		background(g, SKY);
		back(g);
		titlePill(g, "랭킹");
		coins(g, COIN_RIGHT, COIN_TOP);
		gear(g);

		awningPanel(g, LIST, "랭킹", LIFT);
		awningPanel(g, ME, "내 기록", LIFT);

		list(g);
		me(g);
	}

	private void list(Graphics2D g) {
		toggle(g, WEEK_TOGGLE, "주간", "전체", weekly, 28);

		text(g, "순위", RANK_X, HEAD_TOP + 7, 24, Ui.SUB_TEXT);
		text(g, "닉네임", NICK_X, HEAD_TOP + 7, 24, Ui.SUB_TEXT);
		textRight(g, "점수", SCORE_RIGHT, HEAD_TOP + 7, 24, Ui.SUB_TEXT);
		textRight(g, "시간", TIME_RIGHT, HEAD_TOP + 7, 24, Ui.SUB_TEXT);
		solid(g, TABLE_X, ROW_TOP, TABLE_W, Ui.DARK_BROWN, 3);

		for (int i = 0; i < ROWS; i++) {
			boolean has = i < entries.size();
			int top = ROW_TOP + i * ROW_H;
			row(g, top, String.valueOf(i + 1), i < MEDAL.length ? MEDAL[i] : null,
					has ? entries.get(i).name : NO_NAME,
					has ? String.format("%06d", entries.get(i).score) : NO_SCORE, false);
			dashed(g, TABLE_X, top + ROW_H, TABLE_W);
		}

		textAt(g, "· · ·", TABLE_X + TABLE_W / 2.0, ELLIPSIS_TOP, 26, Ui.SUB_TEXT);

		// 내 기록 줄은 표 아래에 노란 칸으로 따로 둔다
		box(g, ME_ROW, 14, Ui.YELLOW, null, 0, 0, null);
		row(g, ME_ROW.y + 2, myRankText(), null, me, myScoreText(), true);
	}

	/** 표 한 줄. 상위 세 줄은 순위 동그라미를 금·은·동으로 칠한다. */
	private void row(Graphics2D g, int top, String rank, Color medal, String name, String score, boolean mine) {
		Rectangle circle = new Rectangle(RANK_X, top + 6, 48, 48);
		box(g, circle, 24, medal != null ? medal : Color.WHITE, medal != null ? Ui.BROWN : PLAIN_RING, 3, 0, null);
		textInBox(g, rank, circle, 24, Ui.TEXT);

		text(g, name, NICK_X, top + 9, 32, Ui.TEXT);
		if (mine) {
			int x = NICK_X + textWidth(g, name, 32) + 10;
			Rectangle tag = new Rectangle(x, top + 15, 38, 30);
			box(g, tag, 15, Ui.RED, null, 0, 0, null);
			textInBox(g, "나", tag, 20, Color.WHITE);
		}
		textRight(g, score, SCORE_RIGHT, top + 9, 32, Ui.TEXT);
		textRight(g, NO_TIME, TIME_RIGHT, top + 12, 28, Ui.SUB_TEXT);
	}

	private void me(Graphics2D g) {
		textAt(g, weekly ? "이번 주 랭킹" : "전체 랭킹", ME_CENTER, 405, 26, Ui.SUB_TEXT);

		// 큰 순위 숫자와 '위' 를 한 덩어리로 가운데에 놓는다
		String rank = myRankText();
		int rankW = textWidth(g, rank, 120), unitW = textWidth(g, "위", 52);
		int x = ME_CENTER - (rankW + 8 + unitW) / 2;
		text(g, rank, x, 438, 120, Ui.DARK_BROWN);
		text(g, "위", x + rankW + 8, 501, 52, Ui.DARK_BROWN);
		solid(g, ME_X, 602, ME_W, Ui.DARK_BROWN, 3);

		String[] values = { myScoreText(), NO_TIME, myCount() + "판" };
		for (int i = 0; i < STAT_LABELS.length; i++) {
			text(g, STAT_LABELS[i], ME_X, STAT_TOP[i] + 10, 26, Ui.TEXT);
			textRight(g, values[i], ME_RIGHT, STAT_TOP[i] + 4, 36, Ui.TEXT);
		}
		dashed(g, ME_X, 792, ME_W);

		text(g, "주간 보상", ME_X, REWARD_TITLE_TOP, 28, Ui.DARK_BROWN);
		for (int i = 0; i < REWARD_LABELS.length; i++) {
			int top = REWARD_TOP[i];
			text(g, REWARD_LABELS[i], ME_X, top + 8, 24, Ui.TEXT);
			int valueW = textWidth(g, REWARD_VALUES[i], 30);
			textRight(g, REWARD_VALUES[i], ME_RIGHT, top + 4, 30, Ui.TEXT);
			drawImage(g, COIN_ICON, new Rectangle(ME_RIGHT - valueW - 8 - 28, top + 9, 28, 28));
		}

		box(g, START, 44, Ui.RED, new Color(0x8E2E1E), 4, 6, new Color(0x8E2E1E));
		int labelW = textWidth(g, "시작하기", 40);
		int left = START.x + (START.width - (26 + 14 + labelW)) / 2;
		whiteTriangle(g, left, START.y + START.height / 2.0, 26);
		text(g, "시작하기", left + 40, START.y + 19, 40, Color.WHITE);

		// 보상을 막 받았으면 그 안내를, 아니면 마감까지 남은 시간을 적는다 (자리가 한 줄뿐이다)
		if (rewardNotice != null)
			textAt(g, rewardNotice, ME_CENTER, DEADLINE_TOP, 22, Ui.RED);
		else
			textAt(g, Ranking.deadlineText(), ME_CENTER, DEADLINE_TOP, 22, Ui.SUB_TEXT);
	}

	/** 시작하기 버튼의 흰 삼각형. (x = 왼쪽 끝) */
	private void whiteTriangle(Graphics2D g, double x, double centerY, double size) {
		java.awt.geom.Path2D.Double p = new java.awt.geom.Path2D.Double();
		p.moveTo(px(x), py(centerY - size / 2));
		p.lineTo(px(x + size * 0.866), py(centerY));
		p.lineTo(px(x), py(centerY + size / 2));
		p.closePath();
		g.setColor(Color.WHITE);
		g.fill(p);
	}
}
