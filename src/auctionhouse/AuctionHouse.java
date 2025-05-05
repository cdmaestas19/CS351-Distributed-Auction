package auctionhouse;

import shared.BankClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionHouse {
    private final int serverPort;
    private ServerSocket serverSocket;
    private final ExecutorService agentThreadPool;
    private volatile boolean running = false;

    private final BankClient bankClient;
    private final ItemManager itemManager;

    private final Map<Integer, AgentHandler> agentHandlers = new ConcurrentHashMap<>();

    public AuctionHouse(int port, BankClient bankClient, ItemManager itemManager) {
        this.serverPort = port;
        this.bankClient = bankClient;
        this.itemManager = itemManager;
        this.agentThreadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(serverPort);
            running = true;

            String localHost = InetAddress.getLocalHost().getHostAddress();
            int localPort = serverSocket.getLocalPort();

            int accountId = bankClient.registerAuctionHouse(localHost, localPort);

            if (accountId < 0) {
                System.err.println("Failed to register with bank. Aborting startup.");
                return;
            }

            System.out.println("Auction House registered with bank. Account ID: " + accountId);
            System.out.println("Listening for agents on port " + serverPort);

            listenForAgents();

        } catch (IOException e) {
            System.err.println("Failed to start auction house: " + e.getMessage());
        }
    }

    private void listenForAgents() {
        while (running) {
            try {
                Socket agentSocket = serverSocket.accept();
                AgentHandler handler = new AgentHandler(agentSocket, itemManager, bankClient, this);
                agentThreadPool.submit(handler);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting agent connection: " + e.getMessage());
                }
                break;
            }
        }
    }

    public void registerAgent(int agentId, AgentHandler handler) {
        agentHandlers.put(agentId, handler);
    }

    public AgentHandler getAgentHandler(int agentId) {
        return agentHandlers.get(agentId);
    }

    public void shutdown() {
        // TODO: Deregister from bank, close sockets, clean shutdown
    }
}
