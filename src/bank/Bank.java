package bank;

import shared.BankClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Bank {
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService clientThreadPool;
    private volatile boolean running = false;

    private final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> auctionHouseAddresses = new ConcurrentHashMap<>();
    private final AtomicInteger nextAccountId = new AtomicInteger(1000);

    public Bank(int port) {
        this.port = port;
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Bank server listening on port " + port);
            listenForClients();
        } catch (IOException e) {
            System.err.println("Bank server failed to start: " + e.getMessage());
        }
    }

    private void listenForClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connection from " + clientSocket.getRemoteSocketAddress());
                clientThreadPool.submit(new BankClientHandler(clientSocket, accounts, auctionHouseAddresses, nextAccountId));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientThreadPool.shutdownNow();
            System.out.println("Bank server shut down.");
        } catch (IOException e) {
            System.err.println("Error shutting down bank server: " + e.getMessage());
        }
    }
}