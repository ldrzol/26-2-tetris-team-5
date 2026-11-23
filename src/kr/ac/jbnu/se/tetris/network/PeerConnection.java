package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * 두 대 사이의 연결 하나. 방을 열거나(host) 열린 방에 들어가서(connect) 만든다.
 *
 * 받은 메시지는 전용 스레드에서 listener 로 넘어온다. Swing 을 건드리려면
 * listener 안에서 SwingUtilities.invokeLater 로 감싸야 한다.
 */
public class PeerConnection {

	private final Socket socket;
	private final ObjectOutputStream out;
	private final ObjectInputStream in;
	private volatile Consumer<Object> listener;
	/** 접속을 기다리는 최대 시간. 이보다 오래 걸리면 실패로 본다. */
	private static final int CONNECT_TIMEOUT_MS = 5000;
	/** 상대와 연결이 끊어졌을 때 할 일. 받는 스레드에서 불린다. */
	private volatile Runnable onDisconnected = () -> { };

	/** 내가 close() 로 닫았는지. 내가 닫은 건 끊김으로 알리지 않는다. */
	private volatile boolean closed = false;

	/**
	 * 열어 둔 대기 소켓으로 상대가 들어올 때까지 기다린다. 들어오면 대기 소켓은 닫는다.
	 * 기다리는 동안 다른 스레드에서 server.close() 를 부르면 대기가 취소된다 (IOException).
	 */
	public static PeerConnection host(ServerSocket server, Consumer<Object> listener) throws IOException {
		try (ServerSocket waiting = server) {
			return listening(waiting.accept(), listener);
		}
	}

	/** 열려 있는 방에 들어간다. */
	public static PeerConnection connect(String ip, int port, Consumer<Object> listener) throws IOException {
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
		// 양쪽 모두 out 을 먼저 만들어야 한다. in 은 상대가 보낸 머리말을 읽을 때까지 멈춘다.
		out = new ObjectOutputStream(socket.getOutputStream());
		in = new ObjectInputStream(socket.getInputStream());
	}

	/** 받은 메시지를 listener 로 넘기는 전용 스레드를 띄운다. */
	public void startListening() {
		Thread thread = new Thread(this::receiveLoop);
		thread.setDaemon(true);   // 연결이 남아 있어도 창을 닫으면 프로그램이 끝나야 한다
		thread.start();
	}

	/** 연결을 만든 뒤에 받는 쪽을 바꾼다. (화면이 바뀔 때 쓴다) */
	public void setListener(Consumer<Object> listener) {
		this.listener = listener;
	}

	/** 상대와 연결이 끊어졌을 때 할 일을 정한다. 받는 스레드에서 불리므로 화면은 invokeLater 로 바꿔야 한다. */
	public void setOnDisconnected(Runnable onDisconnected) {
		this.onDisconnected = onDisconnected;
	}

	private void receiveLoop() {
		try {
			while (true)
				listener.accept(in.readObject());
		} catch (IOException | ClassNotFoundException e) {
			if (!closed)
				onDisconnected.run();
		}
	}

	public void send(Object message) {
		try {
			out.writeObject(message);
			out.reset();
		} catch (IOException e) {
			System.out.println("전송 실패: " + e.getMessage());
		}
	}

	public void close() {
		closed = true;
		try {
			socket.close();
		} catch (IOException ignored) {
			// 이미 끊어진 연결이라 더 할 일이 없다
		}
	}
}
