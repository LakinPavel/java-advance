package info.kgeorgiy.ja.Laskin_Pavel.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {

    private ExecutorService servers;
    private DatagramSocket socket;
    private final int TIME_TO_SEND = 50;

    /**
     * Entry point for the UDP server application.
     *
     * <p>Accepts exactly 2 command-line arguments:
     * <ol>
     *   <li>{@code port} - The port number (must be a valid integer)</li>
     *   <li>{@code threads} - Number of worker threads (must be a valid integer)</li>
     * </ol>
     *
     * @param args Command-line arguments array (must contain exactly 2 non-null elements)
     * @throws IllegalArgumentException If:
     *         <ul>
     *           <li>Argument count is not exactly 2</li>
     *           <li>Any argument is null</li>
     *         </ul>
     * @throws NumberFormatException If port or threads arguments cannot be parsed as integers
     **/
    public static void main(String[] args) {
        if (args.length != 2){
            throw new IllegalArgumentException("Incorrect number of arguments. Should be 2 arguments");
        }
        if (args[0] == null || args[1] == null){
            throw new IllegalArgumentException("Null arguments");
        }
        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            try (HelloUDPServer server = new HelloUDPServer();) {
                server.start(port, threads);
            }
        } catch (NumberFormatException e){
            throw new NumberFormatException("Incorrect arguments");
        }
    }

    @Override
    public void start(int port, int threads) {
        this.servers = Executors.newFixedThreadPool(threads);

        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(TIME_TO_SEND);
        } catch (SocketException e) {
            throw new RuntimeException("ERROR when you try initialize socket: " + e.getMessage());
        }


        for (int s = 0; s < threads; s++) {
            servers.execute(() -> {
                while (!socket.isClosed()) {
                    try {
                        int lenToBytesBuffer = socket.getReceiveBufferSize();
                        byte[] toDPResponse = new byte[lenToBytesBuffer];
                        DatagramPacket myPackToResponse = new DatagramPacket(toDPResponse, lenToBytesBuffer);
                        socket.receive(myPackToResponse);

                        String finalResponse = new String(myPackToResponse.getData(), 0, myPackToResponse.getLength(), StandardCharsets.UTF_8);
                        String messageToSend = "Hello, " + finalResponse;
                        byte[] toDPSend = messageToSend.getBytes();
                        DatagramPacket myPackToSend = new DatagramPacket(toDPSend, toDPSend.length, myPackToResponse.getAddress(), myPackToResponse.getPort());
                        socket.send(myPackToSend);
                    } catch (IOException e) {
                        System.err.println("ERROR when you try send request and get response: " + e.getMessage());
                    }
                }
            });

        }

    }

    // https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
    @Override
    public void close() {
        servers.close();
        socket.close();
    }

}