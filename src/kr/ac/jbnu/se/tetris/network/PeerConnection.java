package kr.ac.jbnu.se.tetris.network;
import java.io.*;
import java.net.*;
public class PeerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private NetworkListener listener;
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
                out.writeObject(message);
            } catch (IOException e) {
                System.out.println("전송실패:"+ e.getMessage());
            }
        }
        public void close(){
            try{
                socket.close();
            } catch (IOException e){

            }
        }

    }

