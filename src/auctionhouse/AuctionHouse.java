package auctionhouse;

import shared.BankClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionHouse {
    private final int serverPort;
    private ServerSocket serverSocket;
    private final ExecutorService agentThreadPool;
    private volatile boolean running = false;

    private final BankClient bankClient;
    private final ItemManager itemManager;

    public AuctionHouse(int port, BankClient bankClient, ItemManager itemManager) {
        this.serverPort = port;
        this.bankClient = bankClient;
        this.itemManager = itemManager;
        this.agentThreadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        // TODO: Register with bank using bankClient
        try {
            serverSocket = new ServerSocket(serverPort);
            running = true;
            System.out.println("Auction House listening on port " + serverPort);
            listenForAgents();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private void listenForAgents() {
        while (running) {
            try {
                Socket agentSocket = serverSocket.accept();
                agentThreadPool.submit(new AgentHandler(agentSocket, itemManager, bankClient));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting agent connection: " + e.getMessage());
                }
                break;
            }
        }
    }

    public void shutdown() {
        // TODO: Deregister from bank, close sockets, clean shutdown
    }
}
