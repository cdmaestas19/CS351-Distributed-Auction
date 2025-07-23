package auctionhouse;

import shared.BankClient;
import shared.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Handles communication with a single connected agent.
 * Manages bidding requests, item listing, and result notifications.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Isaac Tapia
 * @author Christian Maestas
 */
public class AgentHandler implements Runnable {

    private final Socket socket;
    private final ItemManager itemManager;
    private final BankClient bankClient;
    private final AuctionHouse auctionHouse;
    private BufferedReader in;
    private PrintWriter out;
    private int agentId = -1;

    /**
     * Constructs a new handler for a connected agent socket.
     *
     * @param socket       the socket for agent communication
     * @param itemManager  the item manager used to access auction items
     * @param bankClient   the client used to communicate with the bank
     * @param auctionHouse the auction house managing this handler
     */
    public AgentHandler(Socket socket, ItemManager itemManager, BankClient bankClient, AuctionHouse auctionHouse) {
        this.socket = socket;
        this.itemManager = itemManager;
        this.bankClient = bankClient;
        this.auctionHouse = auctionHouse;
    }

    /**
     * Main handler loop. Parses incoming agent messages and dispatches appropriate responses.
     */
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String init = in.readLine();
            String[] initTokens = Message.decode(init);

            if (initTokens.length != 2 || !initTokens[0].equalsIgnoreCase("AGENT")) {
                out.println(Message.encode("REJECTED", "Missing AGENT ID"));
                close();
                return;
            }

            try {
                agentId = Integer.parseInt(initTokens[1]);
            } catch (NumberFormatException e) {
                out.println(Message.encode("REJECTED", "Invalid AGENT ID"));
                close();
                return;
            }

            auctionHouse.registerAgent(agentId, this);
            out.println(Message.encode("WELCOME", String.valueOf(agentId)));

            // Handle incoming commands
            String line;
            while ((line = in.readLine()) != null) {
                String[] tokens = Message.decode(line);
                if (tokens.length == 0) continue;

                switch (tokens[0].toUpperCase()) {
                    case "LIST" -> handleList();
                    case "BID" -> handleBid(tokens);
                    case "QUIT" -> {
                        out.println(Message.encode("GOODBYE"));
                        close();
                        return;
                    }
                    default -> out.println(Message.encode("ERROR",
                            "Unknown command"));
                }
            }

        } catch (IOException e) {
            System.err.println("Agent communication error: " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Handles a LIST command by sending active auction item details to the agent.
     */
    private void handleList() {
        List<AuctionItem> activeItems = itemManager.getAvailableItems();

        for (AuctionItem item : activeItems) {
            out.println(Message.encode(
                    "ITEM",
                    String.valueOf(item.getItemId()),
                    "\"" + item.getDescription() + "\"",
                    String.valueOf(item.getMinimumBid()),
                    String.valueOf(item.getCurrentBid())
            ));
        }
        out.println(Message.encode("END_ITEMS"));
    }

    /**
     * Handles a BID command from the agent.
     * Validates bid amount, checks funds, updates item state, and notifies other bidders.
     */
    private void handleBid(String[] tokens) {
        if (tokens.length < 3) {
            out.println(Message.encode("REJECTED",
                    "Invalid BID format"));
            return;
        }

        try {
            int itemId = Integer.parseInt(tokens[1]);
            int bidAmount = Integer.parseInt(tokens[2]);

            AuctionItem item = itemManager.getItem(itemId);
            if (item == null || item.isSold()) {
                out.println(Message.encode("REJECTED",
                        "Item not found or already sold"));
                return;
            }

            synchronized (item) {
                int minBid = item.getMinimumBid();
                int currentBid = item.getCurrentBid();
                int prevBidder = item.getCurrentBidderId();

                if (bidAmount < minBid || bidAmount <= currentBid) {
                    out.println(Message.encode("REJECTED",
                            "Bid too low"));
                    return;
                }

                if (agentId == prevBidder) {
                    out.println(Message.encode("REJECTED",
                            "You already have the highest bid "));
                    return;
                }

                boolean blocked = bankClient.blockFunds(agentId, bidAmount);
                if (!blocked) {
                    out.println(Message.encode("REJECTED",
                            "Insufficient funds"));
                    return;
                }

                // Unblock funds for previous bidder
                if (prevBidder != -1) {
                    int prevAmount = item.getCurrentBid();
                    bankClient.unblockFunds(prevBidder, prevAmount);

                    AgentHandler prevHandler = auctionHouse.getAgentHandler(prevBidder);
                    if (prevHandler != null) {
                        prevHandler.sendOutbidNotification(item.getItemId());
                    }
                }

                item.placeBid(agentId, bidAmount);
                itemManager.startAuctionTimer(item, auctionHouse);
                auctionHouse.broadcastItemUpdate(item);
                sendItemUpdate(item);
                auctionHouse.triggerUpdate();
                out.println(Message.encode("ACCEPTED",
                        String.valueOf(itemId)));
            }

        } catch (NumberFormatException e) {
            out.println(Message.encode("REJECTED",
                    "Invalid number format"));
        }
    }

    /**
     * Sends an updated item state to this agent.
     */
    public void sendItemUpdate(AuctionItem item) {
        out.println(Message.encode(
                "ITEM_UPDATED",
                String.valueOf(item.getItemId()),
                "\"" + item.getDescription() + "\"",
                String.valueOf(item.getMinimumBid()),
                String.valueOf(item.getCurrentBid())
        ));
    }

    /**
     * Notifies this agent that an item has been sold.
     */
    public void sendItemSoldNotification(int itemId) {
        out.println(Message.encode("ITEM_SOLD", String.valueOf(itemId)));
    }

    /**
     * Notifies this agent that they have been outbid.
     */
    public void sendOutbidNotification(int itemId) {
        out.println(Message.encode("OUTBID", String.valueOf(itemId)));
    }

    /**
     * Notifies this agent that they have won the item.
     */
    public void sendWinnerNotification(int amount, int itemId) {
        out.println(Message.encode("WINNER", String.valueOf(amount),
                String.valueOf(itemId)));
    }

    /**
     * Closes the connection to this agent.
     */
    private void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing agent socket: " + e.getMessage());
        }
    }
}
