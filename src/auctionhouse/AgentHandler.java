package auctionhouse;

import shared.BankClient;
import shared.Message;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class AgentHandler implements Runnable {
    private final Socket socket;
    private final ItemManager itemManager;
    private final BankClient bankClient;
    private final AuctionHouse auctionHouse;
    private BufferedReader in;
    private PrintWriter out;
    private int agentId = -1;

    public AgentHandler(Socket socket, ItemManager itemManager, BankClient bankClient, AuctionHouse auctionHouse) {
        this.socket = socket;
        this.itemManager = itemManager;
        this.bankClient = bankClient;
        this.auctionHouse = auctionHouse;
    }

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
                    default -> out.println(Message.encode("ERROR", "Unknown command"));
                }
            }

        } catch (IOException e) {
            System.err.println("Agent communication error: " + e.getMessage());
        } finally {
            close();
        }
    }

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
    }

    private void handleBid(String[] tokens) {
        if (tokens.length < 3) {
            out.println(Message.encode("REJECTED", "Invalid BID format"));
            return;
        }

        try {
            int itemId = Integer.parseInt(tokens[1]);
            int bidAmount = Integer.parseInt(tokens[2]);

            AuctionItem item = itemManager.getItem(itemId);
            if (item == null || item.isSold()) {
                out.println(Message.encode("REJECTED", "Item not found or already sold"));
                return;
            }

            synchronized (item) {
                int minBid = item.getMinimumBid();
                int currentBid = item.getCurrentBid();
                int prevBidder = item.getCurrentBidderId();

                if (bidAmount < minBid || bidAmount <= currentBid) {
                    out.println(Message.encode("REJECTED", "Bid too low"));
                    return;
                }

                boolean blocked = bankClient.blockFunds(agentId, bidAmount);
                if (!blocked) {
                    out.println(Message.encode("REJECTED", "Insufficient funds"));
                    return;
                }

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
                auctionHouse.broadcastItemUpdate(item, agentId);
                out.println(Message.encode("ACCEPTED"));
            }

        } catch (NumberFormatException e) {
            out.println(Message.encode("REJECTED", "Invalid number format"));
        }
    }

    public void sendItemUpdate(AuctionItem item) {
        out.println(Message.encode(
                "ITEM_UPDATED",
                String.valueOf(item.getItemId()),
                "\"" + item.getDescription() + "\"",
                String.valueOf(item.getMinimumBid()),
                String.valueOf(item.getCurrentBid())
        ));
    }

    public void sendOutbidNotification(int itemId) {
        // TODO: Send new bid price
        out.println(Message.encode("OUTBID", String.valueOf(itemId)));
    }

    public void sendWinnerNotification(int itemId) {
        out.println(Message.encode("WINNER", String.valueOf(itemId)));
    }

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
