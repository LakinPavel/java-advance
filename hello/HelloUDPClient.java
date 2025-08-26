package info.kgeorgiy.ja.Laskin_Pavel.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.NewHelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class HelloUDPClient implements NewHelloClient {

    private final int NUMBER_OF_ATTEMPTS = 10000;
    private final int TIME_TO_SEND = 100;

    /**
     * Entry point for the UDP client application.
     *
     * <p>Accepts exactly 5 command-line arguments in the following order:
     * <ol>
     *   <li>{@code host} - Server hostname/IP address (String)</li>
     *   <li>{@code port} - Server port number (int)</li>
     *   <li>{@code prefix} - Message prefix (String)</li>
     *   <li>{@code threads} - Number of worker threads (int)</li>
     *   <li>{@code requests} - Requests per thread (int)</li>
     * </ol>
     *
     * @param args Command-line arguments array (must contain exactly 5 non-null elements)
     * @throws IllegalArgumentException If argument count is not 5 or any argument is null
     * @throws NumberFormatException If numeric arguments (port, threads, requests) are invalid
     **/
    public static void main(String[] args) {
        if (args.length != 5){
            throw new IllegalArgumentException("Incorrect number of arguments. Should be 5 arguments");
        }
        boolean notNullArguments = true;
        for (int i = 0; i < 5; i++){
            if (args[i] == null){
                notNullArguments = false;
                break;
            }
        }
        if (notNullArguments){
            throw new IllegalArgumentException("Null arguments");
        }
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String prefix = args[2];
            int threads = Integer.parseInt(args[3]);
            int requestsInThread = Integer.parseInt(args[4]);
            List<Request> requests = new ArrayList<>();
            for (int i = 0; i < requestsInThread; i++){
                requests.add(new Request(host, port, prefix + i + "_$"));
            }
            HelloUDPClient client = new HelloUDPClient();
            client.newRun(requests, threads);
        } catch (NumberFormatException e){
            throw new NumberFormatException("Incorrect arguments");
        }
    }

    @Override
    public void newRun(List<Request> requests, int threads) {
        ExecutorService clients = Executors.newFixedThreadPool(threads);
        Phaser phaser = new Phaser(1);

        for (int t = 1; t < threads + 1; t++){
            final int currentThread = t;
                phaser.register();
                clients.execute(() -> {
                    try (DatagramSocket socket = new DatagramSocket();) {
                        socket.setSoTimeout(TIME_TO_SEND);
                        for (Request curRequest : requests){
                            String request = curRequest.template().replace("$", Integer.toString(currentThread));
                            byte[] toDPSend = request.getBytes();
                            for (int i = 0; i < NUMBER_OF_ATTEMPTS; i++) {
                                try {
                                    DatagramPacket myPackToSend = new DatagramPacket(toDPSend, toDPSend.length, InetAddress.getByName(curRequest.host()), curRequest.port());
                                    socket.send(myPackToSend);
                                } catch (IOException e){
                                    System.err.println("ERROR when you try sending: " + e.getMessage());
                                }
                                try {
                                    int lenToBytesBuffer = socket.getReceiveBufferSize();
                                    byte[] toDPResponse = new byte[lenToBytesBuffer];
                                    DatagramPacket myPackToResponse = new DatagramPacket(toDPResponse, lenToBytesBuffer);
                                    socket.receive(myPackToResponse);
                                    String finalResponse = new String(myPackToResponse.getData(), 0, myPackToResponse.getLength(), StandardCharsets.UTF_8);

                                    if (finalResponse.contains(request)){
                                        System.out.println(request + System.lineSeparator() + finalResponse);
                                        break;
                                    }
                                } catch (IOException e){
                                    System.err.println("ERROR when you try get response: " + e.getMessage());
                                }
                            }
                        }
                    } catch (SocketException e){
                        System.err.println(e.getMessage());
                    } finally {
                        phaser.arrive();
                    }
                });
        }
        phaser.arriveAndAwaitAdvance();
        clients.shutdown();
    }

}


