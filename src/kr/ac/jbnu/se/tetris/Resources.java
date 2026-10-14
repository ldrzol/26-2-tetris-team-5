package kr.ac.jbnu.se.tetris;

import java.io.InputStream;
import java.net.URL;

/**
 * 그림과 글꼴을 찾는 곳.
 *
 * 자원은 패키지를 나누기 전부터 kr/ac/jbnu/se/tetris 아래에 모여 있다. 클래스마다
 * getResource 를 부르면 그 클래스가 든 폴더를 기준으로 찾기 때문에, 클래스를 다른
 * 폴더로 옮기는 순간 경로가 어긋난다. 그래서 여기서 절대 경로로 한 번에 찾는다.
 *
 * 넘기는 경로는 "images/coin.png" 처럼 자원 폴더 안의 상대 경로다.
 */
public final class Resources {

	private static final String BASE = "/kr/ac/jbnu/se/tetris/";

	private Resources() {
	}

	/** 없으면 null. */
	public static URL find(String path) {
		return path == null ? null : Resources.class.getResource(BASE + path);
	}

	/** 없으면 null. */
	public static InputStream open(String path) {
		return path == null ? null : Resources.class.getResourceAsStream(BASE + path);
	}
}
