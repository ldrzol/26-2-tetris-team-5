package kr.ac.jbnu.se.tetris.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
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

	/** 방을 열고 상대가 들어올 때까지 기다린다. 들어오면 대기용 소켓은 닫는다. */
	public static PeerConnection host(int port, Consumer<Object> listener) throws IOException {
		try (ServerSocket server = new ServerSocket(port)) {
			return listening(server.accept(), listener);
		}
	}

	/** 열려 있는 방에 들어간다. */
	public static PeerConnection connect(String ip, int port, Consumer<Object> listener) throws IOException {
		return listening(new Socket(ip, port), listener);
	}

	private static PeerConnection listening(Socket socket, Consumer<Object> listener) throws IOException {
		PeerConnection connection = new PeerConnection(socket, listener);
		connection.startListening();
		return connection;
	}

	/** NetworkListener 로 직접 연결을 만드는 쪽(GameClient/GameServer)용. 수신은 startListening() 으로 따로 시작한다. */
	public PeerConnection(Socket socket, NetworkListener listener) throws IOException {
		this(socket, (Consumer<Object>) listener::onMessageReceived);
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

	private void receiveLoop() {
		try {
			while (true)
				listener.accept(in.readObject());
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("연결이 끊어졌습니다.");
		}
	}

	public void send(Object message) {
		try {
			out.writeObject(message);
		} catch (IOException e) {
			System.out.println("전송 실패: " + e.getMessage());
		}
	}

	public void close() {
		try {
			socket.close();
		} catch (IOException ignored) {
			// 이미 끊어진 연결이라 더 할 일이 없다
		}
	}
}
