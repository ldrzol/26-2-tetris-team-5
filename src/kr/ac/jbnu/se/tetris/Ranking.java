package kr.ac.jbnu.se.tetris;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1인 모드 랭킹. 사용자 폴더의 작은 텍스트 파일에 줄 단위로 적어 둔다.
 *
 * 한 줄 = 이름,점수,날짜. 이름 안의 쉼표와 줄바꿈은 지워서 적는다.
 * 다른 사람 기록도 같은 파일의 다른 이름일 뿐이라 따로 둘 것이 없다.
 *
 * ponytail: 한 PC 안의 파일 하나. 여러 PC 기록을 한 표에 모아야 하면
 * 그때 서버(HTTP + 파일)를 세운다. DB 는 그래도 필요 없다.
 */
final class Ranking {

	/** 한 표에 남겨 두는 줄 수. */
	static final int MAX = 10;

	/** 기록 한 줄. */
	static final class Entry {
		final String name;
		final int score;
		final String date;

		Entry(String name, int score, String date) {
			this.name = name;
			this.score = score;
			this.date = date;
		}
	}

	private Ranking() {
	}

	/** 점수 높은 순. 파일이 없거나 줄이 깨졌으면 읽히는 줄만 돌려준다. */
	static List<Entry> load() {
		List<Entry> out = new ArrayList<>();
		for (String line : lines()) {
			String[] c = line.split(",", 3);
			if (c.length < 3)
				continue;
			try {
				out.add(new Entry(c[0], Integer.parseInt(c[1].trim()), c[2]));
			} catch (NumberFormatException ignored) {
				// 점수 자리가 숫자가 아닌 줄은 버린다
			}
		}
		sort(out);
		return out;
	}

	/** 기록을 넣고 등수(1부터)를 돌려준다. MAX 밖으로 밀려나면 0. */
	static int add(String name, int score) {
		List<Entry> all = load();
		Entry mine = new Entry(clean(name), score, LocalDate.now().toString());
		all.add(mine);
		sort(all);
		if (all.size() > MAX)
			all = all.subList(0, MAX);
		save(all);
		return all.indexOf(mine) + 1;   // 잘려 나갔으면 -1 + 1 = 0
	}

	/** 점수 내림차순. 같은 점수면 먼저 기록한 쪽이 위다 (sort 가 안정 정렬이다). */
	private static void sort(List<Entry> list) {
		Collections.sort(list, (a, b) -> Integer.compare(b.score, a.score));
	}

	/** 파일에 적을 수 있는 이름으로 다듬는다. 화면 폭에 맞춰 길이도 자른다. */
	static String clean(String name) {
		String s = name == null ? "" : name.replaceAll("[,\r\n]", " ").trim();
		if (s.isEmpty())
			return "이름없음";
		return s.length() > 12 ? s.substring(0, 12) : s;
	}

	/** 저장 위치. 자가 점검이 진짜 파일을 건드리지 않도록 속성으로 바꿀 수 있다. */
	private static Path file() {
		String override = System.getProperty("tetris.ranking");
		return override != null ? Paths.get(override)
				: Paths.get(System.getProperty("user.home"), ".tetris-ranking.csv");
	}

	private static List<String> lines() {
		try {
			Path f = file();
			return Files.exists(f) ? Files.readAllLines(f, StandardCharsets.UTF_8) : new ArrayList<String>();
		} catch (IOException e) {
			System.err.println("랭킹을 읽지 못했다: " + e);
			return new ArrayList<String>();
		}
	}

	/** 저장이 실패해도 게임은 계속 돌아가야 하므로 알리고 넘어간다. */
	private static void save(List<Entry> entries) {
		List<String> out = new ArrayList<>();
		for (Entry e : entries)
			out.add(e.name + "," + e.score + "," + e.date);
		try {
			Files.write(file(), out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("랭킹을 저장하지 못했다: " + e);
		}
	}

	/** 자가 점검. java -ea 로 실행한다. */
	public static void main(String[] args) throws IOException {
		Path tmp = Files.createTempFile("ranking-test", ".csv");
		System.setProperty("tetris.ranking", tmp.toString());
		Files.delete(tmp);

		assert load().isEmpty() : "파일이 없으면 빈 표여야 한다";
		assert add("나", 100) == 1 : "첫 기록은 1등";
		assert add("남", 300) == 1 : "더 높은 점수가 1등";
		assert add("또다른,사람\n", 200) == 2 : "쉼표/줄바꿈 이름도 2등으로 들어가야 한다";

		List<Entry> all = load();
		assert all.size() == 3 : "세 줄이어야 한다: " + all.size();
		assert all.get(0).score == 300 && all.get(2).score == 100 : "점수 내림차순이 아니다";
		assert all.get(1).name.indexOf(',') < 0 : "이름의 쉼표가 지워지지 않았다";

		for (int i = 0; i < MAX; i++)
			add("채우기" + i, 1000 + i);
		assert load().size() == MAX : "MAX 줄을 넘겨 저장했다";
		assert add("꼴찌", 1) == 0 : "MAX 밖이면 등수가 0 이어야 한다";
		assert load().get(0).score == 1009 : "가장 높은 점수가 1등이 아니다";

		Files.deleteIfExists(tmp);
		System.out.println("ok");
	}
}
