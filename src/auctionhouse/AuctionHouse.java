package auctionhouse;

import shared.BankClient;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates agent connections, bidding logic, and communication with the bank.
 * Manages item listings, agent threads, and auction lifecycle.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Isaac Tapia
 */
public class AuctionHouse {

    private final int serverPort;
    private ServerSocket serverSocket;
    private final ExecutorService agentThreadPool;
    private volatile boolean running = false;
    public int accountId;
    private final BankClient bankClient;
    private final ItemManager itemManager;
    private final Map<Integer, AgentHandler> agentHandlers = new ConcurrentHashMap<>();
    private Runnable onUpdateCallback;

    /**
     * Constructs an AuctionHouse that manages agents, items, and bank interaction.
     * Prepares to accept agent connections and coordinate auction activity.
     *
     * @param port        the port to listen on for incoming agent connections
     * @param bankClient  the client used to communicate with the bank
     * @param itemManager the manager responsible for item storage and bidding state
     */
    public AuctionHouse(int port, BankClient bankClient, ItemManager itemManager) {
        this.serverPort = port;
        this.bankClient = bankClient;
        this.itemManager = itemManager;
        this.agentThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Starts the auction house: registers with the bank and begins
     * listening for incoming agent connections.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(serverPort);
            running = true;

            String localHost = getExternalIpAddress();
            int localPort = serverSocket.getLocalPort();

            accountId = bankClient.registerAuctionHouse(localHost, localPort);

            if (accountId < 0) {
                System.err.println("Failed to register with bank. Aborting startup.");
                return;
            }

            listenForAgents();

        } catch (IOException e) {
            System.err.println("Failed to start auction house: " + e.getMessage());
        }
    }

    /**
     * Accepts and handles incoming agent connections
     */
    private void listenForAgents() {
        while (running) {
            try {
                Socket agentSocket = serverSocket.accept();
                AgentHandler handler = new AgentHandler(agentSocket, itemManager,
                        bankClient, this);
                agentThreadPool.submit(handler);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting agent connection: " + e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * Gracefully shuts down the auction house.
     */
    public void shutdown() {
        if (hasActiveAuctions()) {
            return;
        }

        running = false;

        try {
            bankClient.deregister(accountId);
        } catch (Exception e) {
            System.err.println("Failed to deregister auction house: " + e.getMessage());
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        agentThreadPool.shutdownNow();
    }

    /**
     * Registers an agent by its ID and associates it with its handler.
     * Used for later communication (e.g., outbid or winner notifications).
     *
     * @param agentId the unique ID of the agent
     * @param handler the AgentHandler managing this agent's socket
     */
    public void registerAgent(int agentId, AgentHandler handler) {
        agentHandlers.put(agentId, handler);
        triggerUpdate();
    }

    /**
     * Retrieves the handler associated with the given agent ID.
     *
     * @param agentId the ID of the agent
     * @return the AgentHandler managing that agent, or null if not found
     */
    public AgentHandler getAgentHandler(int agentId) {
        return agentHandlers.get(agentId);
    }

    /**
     * Returns a list of all currently connected agent IDs.
     *
     * @return list of agent IDs
     */
    public List<Integer> getAgentIds() {
        return new ArrayList<>(agentHandlers.keySet());
    }

    /**
     * Returns the port number on which the auction house server is listening.
     *
     * @return server port
     */
    public int getPort() {
        return serverPort;
    }

    /**
     * Returns the unique account ID assigned by the bank to this auction house.
     *
     * @return bank account ID
     */
    public int getAccountId() {
        return accountId;
    }

    /**
     * Returns the item manager responsible for tracking active and pending auction items.
     *
     * @return the item manager instance
     */
    public ItemManager getItemManager() {
        return itemManager;
    }

    /**
     * Determines whether there are any active auctions in progress.
     * An auction is considered active if it has been listed and has received a bid.
     *
     * @return true if at least one auction is active, false otherwise
     */
    public boolean hasActiveAuctions() {
        return itemManager.hasActiveAuctions();
    }

    /**
     * Broadcasts the updated state of an item to all connected agents.
     * Each update is sent on a separate thread to avoid blocking.
     *
     * @param item the item whose update should be broadcast
     */
    public void broadcastItemUpdate(AuctionItem item) {
        for (Map.Entry<Integer, AgentHandler> entry : agentHandlers.entrySet()) {
            AgentHandler handler = entry.getValue();
            new Thread(() -> {
                try {
                    handler.sendItemUpdate(item);
                } catch (Exception e) {
                    System.err.println("Failed to send item update to Agent " +
                            entry.getKey() + ": " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Notifies all connected agents that a specific item has been sold.
     * Each notification is sent on a separate thread to avoid blocking.
     *
     * @param itemId the ID of the item that was sold
     */
    public void broadcastItemSold(int itemId) {
        for (Map.Entry<Integer, AgentHandler> entry : agentHandlers.entrySet()) {
            AgentHandler handler = entry.getValue();
            new Thread(() -> {
                try {
                    handler.sendItemSoldNotification(itemId);
                } catch (Exception e) {
                    System.err.println("Failed to send ITEM_SOLD to Agent " +
                            entry.getKey() + ": " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Attempts to determine the external IP address of the machine running this auction house.
     * Filters out loopback, virtual, and link-local addresses.
     *
     * @return a usable external IPv4 address
     * @throws IOException if no valid address is found
     */
    private String getExternalIpAddress() throws IOException {
        for (NetworkInterface netInterface :
                Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (netInterface.isLoopback() || !netInterface.isUp() ||
                    netInterface.isVirtual())
                continue;

            for (InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                if (address instanceof Inet4Address
                        && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) {

                    String ip = address.getHostAddress();

                    if (ip.startsWith("172.") || ip.startsWith("169."))
                        continue;

                    return ip;
                }
            }
        }
        throw new IOException("No suitable external IP address found.");
    }

    /**
     * Registers a UI update callback to be run whenever auction state changes.
     *
     * @param callback the update routine to run
     */
    public void setOnUpdate(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    /**
     * Triggers the registered UI update callback, if one is set.
     * Used to notify the GUI of state changes.
     */
    public void triggerUpdate() {
        if (onUpdateCallback != null) {
            onUpdateCallback.run();
        }
    }
}
