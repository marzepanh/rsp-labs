package lab1;

import java.io.*;
import java.net.*;

public class Client {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Команда: java HTTPClient <server_host> <server_port> <file_path>");
            System.exit(1);
        }
        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String filePath = args[2];
        try (Socket socket = new Socket(serverHost, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("GET " + filePath + " HTTP/1.1");
            out.println("Host: " + serverHost);
            out.println();
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}