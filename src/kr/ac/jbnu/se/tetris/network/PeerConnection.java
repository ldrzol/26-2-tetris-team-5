package kr.ac.jbnu.se.tetris.network;
import java.io.*;
import java.net.*;
public class PeerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile NetworkListener listener;  // ← 이렇게 (단어 하나 추가)
    public PeerConnection(Socket socket, NetworkListener
            listener) throws IOException{
        this.socket=socket;
        this.listener=listener;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }
    public void startListening(){
        Thread thread = new Thread(() -> {
            try{
                while (true){
                    Object received=in.readObject();
                    listener.onMessageReceived(received);
                }
            } catch(IOException|ClassNotFoundException e){
                System.out.println("연결이 끊어졌습니다.");
            }
        });
        thread.start();
    }
        public void send(Object message) {
            try{
                out.writeObject(message);       // ← 31번째 줄 (원래 있음)
                out.reset();                    // ← 새로 추가 (32번째 줄)
            } catch (IOException e) {
                System.out.println("전송실패:"+ e.getMessage());
            }
        }
    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

        public void close(){
            try{
                socket.close();
            } catch (IOException e){

            }
        }

    }

