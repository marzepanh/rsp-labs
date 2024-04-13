package lab2;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        DatagramSocket clientSocket;
        try {
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(1000);
            InetAddress serverAddress = InetAddress.getByName("localhost");
            byte[] sendData;
            byte[] receiveData = new byte[1024];
            for (int sequenceNumber = 1; sequenceNumber <= 10; sequenceNumber++) {
                String message = "Ping " + sequenceNumber + " " + System.currentTimeMillis();
                sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 12000);
                clientSocket.send(sendPacket);
                long startTime = System.nanoTime();
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    clientSocket.receive(receivePacket);
                    long endTime = System.nanoTime();
                    double rtt =  (endTime - startTime) / 1000000.0;
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Response from " + receivePacket.getSocketAddress() + ": " + response + " RTT=" + rtt + " milliseconds");
                } catch (SocketTimeoutException e) {
                    System.out.println("Request timed out");
                }
            }
            clientSocket.close();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
