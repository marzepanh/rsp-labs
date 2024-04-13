package lab4;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    public static void main(String[] args) {
        int port = 20000;
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            System.out.println("Server failed to load " + port);
            return;
        }
        server.createContext("/", new Helper(port));
        server.start();
        System.out.println("Server loaded on port " + port);
    }

}