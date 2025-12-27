package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Client {
    private static Socket socket;
    private static BufferedReader reader;
    private static BufferedWriter writer;

    public static void connectServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public static Socket getSocket() { return socket; }
    public static BufferedReader getReader() { return reader; }
    public static BufferedWriter getWriter() { return writer; }
}
