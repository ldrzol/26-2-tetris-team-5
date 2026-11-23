package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import javax.swing.Timer;

/**
 * 두 대 사이의 연결 하나. 방을 열거나(host) 열린 방에 들어가서(connect) 만든다.
 *
 * 받은 메시지는 전용 스레드에서 listener 로 넘어온다. Swing 을 건드리려면
 * listener 안에서 SwingUtilities.invokeLater 로 감싸야 한다.
 *
 * 연결이 끊어지는 경우는 두 가지다.
 * - 상대가 창을 닫는 것처럼 정상적으로 끊김 -> 바로 알 수 있다.
 * - 랜선이 빠지거나 상대 프로그램이 멈춰서 조용히 끊김 -> 아무 신호가 없어서 알 수 없다.
 * 그래서 2초마다 살아 있다는 신호(Heartbeat)를 주고받고, 10초 동안 아무것도 받지 못하면
 * 끊긴 것으로 보고 onDisconnected 를 부른다.
 */
public class PeerConnection {

	/** [버그 4] 접속을 기다리는 최대 시간. 이보다 오래 걸리면 실패로 본다. */
	private static final int CONNECT_TIMEOUT_MS = 5000;

	/** [버그 7] 살아 있다는 신호를 보내는 간격. */
	private static final int HEARTBEAT_MS = 2000;

	/** [버그 7] 이 시간 동안 아무것도 못 받으면 끊긴 것으로 본다. (신호 간격의 5배라 신호 몇 개가 늦어도 괜찮다) */
	private static final int RESPONSE_TIMEOUT_MS = 10000;

	/**
	 * [버그 7] "아직 연결돼 있다"는 신호. 내용 없이 이름만 있는 값이다.
	 * 받는 쪽에서 걸러서 로비나 게임 화면에는 넘기지 않는다.
	 */
	private enum Heartbeat { BEAT }

	private final Socket socket;
	private final ObjectOutputStream out;
	private final ObjectInputStream in;

	/** 받은 메시지를 넘겨줄 곳. 로비 -> 게임 화면처럼 화면이 바뀌면 setListener 로 바꾼다. */
	private volatile Consumer<Object> listener;

	/** [버그 5] 상대와 연결이 끊어졌을 때 할 일. 받는 스레드에서 불린다. 처음에는 아무것도 안 한다. */
	private volatile Runnable onDisconnected = () -> { };

	/** [버그 5] 내가 close() 로 닫았는지. 내가 닫은 건 "상대가 끊었다"로 알리지 않는다. */
	private volatile boolean closed = false;

	/**
	 * [버그 7] 2초마다 Heartbeat 를 보내는 타이머.
	 * Swing 타이머라서 다른 send 들과 같은 화면 스레드에서 보내진다. (동시에 두 곳에서 쓰지 않게)
	 */
	private final Timer heartbeat = new Timer(HEARTBEAT_MS, e -> send(Heartbeat.BEAT));

	/**
	 * [버그 6] 열어 둔 대기 소켓으로 상대가 들어올 때까지 기다린다. 들어오면 대기 소켓은 닫는다.
	 * 기다리는 동안 다른 스레드에서 server.close() 를 부르면 대기가 취소된다 (IOException).
	 */
	public static PeerConnection host(ServerSocket server, Consumer<Object> listener) throws IOException {
		try (ServerSocket waiting = server) {
			return listening(waiting.accept(), listener);
		}
	}

	/** [버그 4] 열려 있는 방에 들어간다. 최대 CONNECT_TIMEOUT_MS 만 기다린다. */
	public static PeerConnection connect(String ip, int port, Consumer<Object> listener) throws IOException {
		// new Socket(ip, port) 는 만들자마자 접속해서 시간을 정할 수 없다. 빈 소켓을 먼저 만들고 시간을 걸어 접속한다.
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
			return listening(socket, listener);
		} catch (IOException e) {
			socket.close();   // 실패하면 만든 소켓을 닫고 실패를 그대로 알린다
			throw e;
		}
	}

	private static PeerConnection listening(Socket socket, Consumer<Object> listener) throws IOException {
		PeerConnection connection = new PeerConnection(socket, listener);
		connection.startListening();
		return connection;
	}

	private PeerConnection(Socket socket, Consumer<Object> listener) throws IOException {
		this.socket = socket;
		this.listener = listener;
		// [버그 7] 이 시간 동안 아무것도 안 오면 readObject 가 기다리지 않고 예외(SocketTimeoutException)로 멈춘다.
		// 원래는 영원히 기다렸다.
		socket.setSoTimeout(RESPONSE_TIMEOUT_MS);
		// 양쪽 모두 out 을 먼저 만들어야 한다. in 은 상대가 보낸 머리말을 읽을 때까지 멈춘다.
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	/** 받은 메시지를 listener 로 넘기는 전용 스레드를 띄우고, 살아 있다는 신호를 보내기 시작한다. */
	public void startListening() {
		Thread thread = new Thread(this::receiveLoop);
		thread.setDaemon(true);   // 연결이 남아 있어도 창을 닫으면 프로그램이 끝나야 한다
		thread.start();
		heartbeat.start();        // [버그 7] 2초마다 Heartbeat 보내기 시작
	}

	/** 연결을 만든 뒤에 받는 쪽을 바꾼다. (화면이 바뀔 때 쓴다) */
	public void setListener(Consumer<Object> listener) {
		this.listener = listener;
	}

	/** [버그 5] 상대와 연결이 끊어졌을 때 할 일을 정한다. 받는 스레드에서 불리므로 화면은 invokeLater 로 바꿔야 한다. */
	public void setOnDisconnected(Runnable onDisconnected) {
		this.onDisconnected = onDisconnected;
	}

	/** 받는 스레드가 계속 도는 곳. 끊어질 때까지 메시지를 받아 listener 로 넘긴다. */
	private void receiveLoop() {
		try {
			while (true) {
				Object message = in.readObject();
				// [버그 7] 살아 있다는 신호는 연결 확인용이라 화면에 넘기지 않는다.
				// 그래도 무언가를 받았으니 10초 제한은 다시 처음부터 센다.
				if (message != Heartbeat.BEAT)
					listener.accept(message);
			}
		} catch (IOException | ClassNotFoundException e) {
			// 여기 오는 경우: 상대가 연결을 닫았거나 [버그 5], 10초 동안 아무것도 오지 않았다 [버그 7].
			// 내가 직접 닫은 게 아니면 "끊어졌다"고 알린다.
			if (!closed)
				onDisconnected.run();
			close();   // 하트비트 타이머도 멈추고 소켓을 닫는다
		}
	}

	public void send(Object message) {
		try {
			out.writeObject(message);
			// [버그 3] ObjectOutputStream 은 보낸 객체를 계속 기억해서 메모리가 쌓인다. 보낼 때마다 기억을 지운다.
			out.reset();
		} catch (IOException e) {
			System.out.println("전송 실패: " + e.getMessage());
		}
	}

	/** 연결을 닫는다. 내가 닫은 것이므로 onDisconnected 는 부르지 않는다. */
	public void close() {
		closed = true;          // [버그 5] 내가 닫았다고 표시
		heartbeat.stop();       // [버그 7] 더 이상 신호를 보내지 않는다
		try {
			socket.close();
		} catch (IOException ignored) {
			// 이미 끊어진 연결이라 더 할 일이 없다
		}
	}
}