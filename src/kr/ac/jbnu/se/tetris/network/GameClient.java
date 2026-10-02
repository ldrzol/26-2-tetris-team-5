package kr.ac.jbnu.se.tetris.network;
import java.io.*;
import java.net.*;
public class GameClient {
    public PeerConnection connectTo(String ip, int port, NetworkListener listener) throws IOException{
        Socket socket = new Socket(ip, port);
        System.out.println("서버에 접속됨");
        PeerConnection connection = new PeerConnection(socket, listener);
        connection.startListening();
        return connection;
    }
}
