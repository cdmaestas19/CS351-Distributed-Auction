package auctionhouse;

import shared.BankClient;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionHouse {
    private final int serverPort;
    private ServerSocket serverSocket;
    private final ExecutorService agentThreadPool;

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
        // TODO: Start server socket on serverPort
        // TODO: Call listenForAgents()
    }

    private void listenForAgents() {
        // TODO: Accept socket connections,
        // TODO: For each connection, create AgentHandler and submit to thread pool
    }

    public void shutdown() {
        // TODO: Deregister from bank, close sockets, clean shutdown
    }
}
