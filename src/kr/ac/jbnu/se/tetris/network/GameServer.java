package kr.ac.jbnu.se.tetris.network;
import java.io.*;
import java.net.*;
public class GameServer {
    public PeerConnection waitForClient(int port, NetworkListener
            listener) throws IOException{
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println(" 서버시작."+ port + "번 포트에서 대기 중...");
        Socket socket = serverSocket.accept();
        serverSocket.close();
        System.out.println("클라이언트 접속됨");
        PeerConnection connection = new PeerConnection(socket, listener);
        connection.startListening();
        return connection;
    }
}
