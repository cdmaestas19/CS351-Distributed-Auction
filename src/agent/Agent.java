package agent;

import shared.BankClient;
import shared.Message;
import shared.SocketAuctionClient;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A bidding agent
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction
 *
 * @author Dustin Ferguson
 * @author Christian Maestas
 * @author Isaac Tapia
 */
public class Agent implements Runnable {

    private final String agentName;
    private final int agentID;
    private final BankClient bankSocketClient;
    private int totalBalance, availableBalance;
    private Socket bankSocket;
    private List<AuctionManager> auctionManagers = new ArrayList<>();
    private Runnable onBalanceUpdate;
    private Consumer<AuctionManager> onAuctionConnected;
    private Consumer<String> onMessage;
    private Consumer<String> onAuctionRemoved;
    
    
    /**
     * Agent constructor
     * @param bankSocket Socket for communication with bank
     * @param agentName Agent name
     * @param agentID Unique agent name
     * @param bankSocketClient Client that handles communication with bank
     */
    public Agent(Socket bankSocket, String agentName, int agentID,
                 BankClient bankSocketClient) {
        this.agentName = agentName;
        this.agentID = agentID;
        this.bankSocketClient = bankSocketClient;
        this.bankSocket = bankSocket;
    }
    
    /**
     * Sets up persistent communication socket with bank and actively listens for
     * incoming messages.
     */
    @Override
    public void run() {
        try {
            
            // Open persistent socket to Bank
            BufferedReader bankIn = new BufferedReader(new InputStreamReader(bankSocket.getInputStream()));
            
            // Notify bank this is the persistent channel for live updates
            PrintWriter bankOut = new PrintWriter(
                    new OutputStreamWriter(bankSocket.getOutputStream()), true);
            bankOut.println(Message.encode("REGISTER_AGENT_CHANNEL",
                    String.valueOf(agentID)));
            balanceInquiry(bankOut);
            
            while (true) {
                if (bankIn.ready()) {
                    String bankMessage = bankIn.readLine();
                    handleMessage(bankMessage);
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println("Agent message loop error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handles incoming messages.
     * @param message The message received
     * @throws IOException
     */
    public void handleMessage(String message) throws IOException {
        
        String[] parts = Message.decode(message);
        
        switch (parts[0]) {
            case "BALANCE": {
                totalBalance = Integer.parseInt(parts[1]);
                availableBalance = Integer.parseInt(parts[2]);
                if (onBalanceUpdate != null) {
                    javafx.application.Platform.runLater(onBalanceUpdate);
                }
                break;
            }
            case "AUCTION_HOUSE": {
                SocketAuctionClient auctionClient = new SocketAuctionClient();
                auctionClient.connect(parts[1], Integer.parseInt(parts[2]), agentID);
                System.out.println("Connection to Auction House " + parts[3] + " successful!");

                AuctionManager auctionManager = new AuctionManager(parts[3],
                        auctionClient, bankSocketClient, this);
                Thread thread = new Thread(auctionManager);
                thread.start();
                auctionManagers.add(auctionManager);

                if (onAuctionConnected != null) {
                    javafx.application.Platform.runLater(() -> onAuctionConnected.accept(auctionManager));
                }
                break;
            }
            case "REMOVE_AUCTION_HOUSE": {
                String removedId = parts[1];
                AuctionManager toRemove = null;

                for (AuctionManager manager : auctionManagers) {
                    if (manager.getAuctionId().equals(removedId)) {
                        toRemove = manager;
                        break;
                    }
                }

                if (toRemove != null) {
                    try {
                        toRemove.getClient().close();
                    } catch (IOException e) {
                        System.err.println("Failed to close auction client for removed house: " + e.getMessage());
                    }
                    auctionManagers.remove(toRemove);

                    if (onAuctionRemoved != null) {
                        javafx.application.Platform.runLater(() -> onAuctionRemoved.accept(removedId));
                    }
                }
                break;
            }
            default:
                System.out.println("Agent.handleMessage() error!");
                System.out.println(message);
                break;
        }
        
    }
    
    /**
     * Sends request for account balances to bank
     * @param bankOut Writer that sends messages to bank
     */
    public void balanceInquiry(PrintWriter bankOut) {
        bankOut.println(Message.encode("BALANCE", Integer.toString(agentID)));
    }
    
    /**
     * Updates balances without need for bankOut (for auction manager use)
     */
    public void refreshBalance() {
        try {
            PrintWriter bankOut = new PrintWriter(bankSocket.getOutputStream(), true);
            bankOut.println(Message.encode("BALANCE", String.valueOf(agentID)));
        } catch (Exception e) {
            System.err.println("Failed to refresh balance: " + e.getMessage());
        }
    }
    
    /**
     * Pushes a text line to the GUI if a consumer is registered.
     */
    public void sendGuiMessage(String msg) {
        if (onMessage != null) {
            onMessage.accept(msg);
        }
    }
    
    /**
     * Updates balance
     * @param callback
     */
    public void setOnBalanceUpdate(Runnable callback) {
        this.onBalanceUpdate = callback;
    }
    
    /**
     * Updates auction info in GUI
     */
    public void setOnAuctionConnected(Consumer<AuctionManager> callback) {
        this.onAuctionConnected = callback;
        
        for (AuctionManager manager : auctionManagers) {
            javafx.application.Platform.runLater(() -> callback.accept(manager));
        }
    }
    
    /**
     * Updates auction info in GUI when one is removed
     * @param callback
     */
    public void setOnAuctionRemoved(Consumer<String> callback) {
        this.onAuctionRemoved = callback;
    }
    
    /**
     * Updates message display in GUI
     * @param callback
     */
    public void setOnMessage(Consumer<String> callback) {
        this.onMessage = callback;
    }
    
    /**
     * @return Agent's total balance.
     */
    public int getTotalBalance() {
        return totalBalance;
    }
    
    /**
     * @return Available balance
     */
    public int getAvailableBalance() {
        return availableBalance;
    }
    
    /**
     * @return List of auction managers - one for each auctionhouse open
     */
    public List<AuctionManager> getAuctionManagers() {
        return auctionManagers;
    }
    
    /**
     * @return Name of agent
     */
    public String getAgentName() {
        return agentName;
    }
    
    /**
     * @return agent ID Number
     */
    public int getAgentID() {
        return agentID;
    }
    
    /**
     * @return True if there are no currently active bids to allow shutdown.
     */
    public boolean canShutdown() {
        for (AuctionManager manager : auctionManagers) {
            if (manager.hasActiveBids()) return false;
        }
        return true;
    }
    
    /**
     * Attempts a graceful shutdown.
     */
    public void shutdown() {
        if (!canShutdown()) {
            sendGuiMessage("Cannot exit: Active bids still in progress.");
            return;
        }
        
        try {
            bankSocketClient.deregister(agentID);
            sendGuiMessage("Deregistered from bank.");
        } catch (Exception e) {
            System.err.println("Error during deregistration: " + e.getMessage());
        }
        
        for (AuctionManager manager : auctionManagers) {
            try {
                manager.getClient().close();
            } catch (IOException e) {
                System.err.println("Error closing connection to auction house: " + e.getMessage());
            }
        }
        
        System.exit(0);
    }
}
