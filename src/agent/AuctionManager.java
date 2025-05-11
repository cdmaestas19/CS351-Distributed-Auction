package agent;

import shared.BankClient;
import shared.Message;
import shared.SocketAuctionClient;
import java.io.*;
import java.util.*;

public class AuctionManager implements Runnable {
    
    private String auctionId;
    private SocketAuctionClient auctionClient;
    private BufferedReader in;
    private final List<ItemInfo> items;
    private final BankClient bankClient;
    private Runnable onItemUpdate;
    private final Agent agent;
    private final Set<String> activeBids = Collections.synchronizedSet(new HashSet<>());

    public AuctionManager(String auctionId, SocketAuctionClient auctionClient,
                          BankClient bankClient, Agent agent) {
        this.auctionId = auctionId;
        this.auctionClient = auctionClient;
        this.bankClient = bankClient;
        this.items = Collections.synchronizedList(new ArrayList<>());
        this.agent = agent;
    }
    
    @Override
    public void run() {
        try {
            parseItemsList(auctionClient.getAvailableItems());
            if (onItemUpdate != null) {
                javafx.application.Platform.runLater(onItemUpdate);
            }
            in = auctionClient.getInputStream();
            
            while (true) {
                if (in.ready()) {
                    String line = in.readLine();
                    handleMessage(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void parseItemsList(List<String[]> itemList) {
        
        for (String[] item : itemList) {
            parseItem(item);
        }
    }
    
    private void parseItem(String[] item) {
        
        String itemId = item[1];

        StringBuilder descBuilder = new StringBuilder();
        for (int i = 2; i < item.length - 2; i++) {
            descBuilder.append(item[i]);
            if (i != item.length - 3) descBuilder.append(" ");
        }
        String description = descBuilder.toString();
        description = description.substring(1, description.length() - 1); // Strip quotes
        
        int minBid = Integer.parseInt(item[item.length - 2]);
        int currBid = Integer.parseInt(item[item.length - 1]);
        
        ItemInfo itemInfo = new ItemInfo(auctionId, itemId, description, minBid, currBid);
        items.add(itemInfo);
    }

    public void handleMessage(String message) throws IOException {
        System.out.println("AuctionManager received: " + message);
        String[] parts = Message.decode(message);

        switch (parts[0]) {
            case "ACCEPTED" -> {
                String itemId = parts[1];
                activeBids.add(itemId);
                agent.sendGuiMessage("Bid accepted!");
            }

            case "REJECTED" -> {
                StringBuilder rejMsg = new StringBuilder("Bid rejected: ");
                    for (int i = 1; i < parts.length; i++) {
                        rejMsg.append(parts[i]);
                        rejMsg.append(" ");
                    }
                agent.sendGuiMessage(rejMsg.toString());
            }

            case "OUTBID" -> {
                String itemId = parts[1];
                activeBids.remove(itemId);
                agent.sendGuiMessage("You were outbid on item " + itemId);
            }

            case "WINNER" -> {
                System.out.println("winner case");
                int amount = Integer.parseInt(parts[1]);
                int toAuctionHouseId = Integer.parseInt(auctionId);
                int fromAgentId = auctionClient.getAgentId();
                String itemId = parts[2];
                try {
                    bankClient.transferFunds(fromAgentId, toAuctionHouseId, amount);
                } catch (Exception e) {
                    System.err.println("Failed to transfer funds: " + e.getMessage());
                }
                activeBids.remove(itemId);
                agent.sendGuiMessage("You won item " + itemId + " from auction " +
                        "house" + auctionId);
                agent.refreshBalance();
            }
            
            case "ITEM_UPDATED" -> {
                if (parts.length >= 5) {
                    String itemId = parts[1];
                    StringBuilder descBuilder = new StringBuilder();
                    for (int i = 2; i < parts.length - 2; i++) {
                        descBuilder.append(parts[i]);
                        if (i != parts.length - 3) descBuilder.append(" ");
                    }
                    String description = descBuilder.toString();
                    description = description.substring(1, description.length() - 1); // strip quotes
                    int minBid = Integer.parseInt(parts[parts.length - 2]);
                    int currBid = Integer.parseInt(parts[parts.length - 1]);
                    
                    boolean updated = false;
                    for (ItemInfo item : items) {
                        if (item.itemId.equals(itemId)) {
                            item.description = description;
                            item.minBid = minBid;
                            item.currBid = currBid;
                            updated = true;
                            break;
                        }
                    }
                    if (!updated) {
                        items.add(new ItemInfo(auctionId, itemId, description, minBid, currBid));
                    }
                    
                    if (onItemUpdate != null) {
                        javafx.application.Platform.runLater(onItemUpdate);
                    }
                }
            }
            
            case "ITEM_SOLD" -> {
                String itemId = parts[1];
                items.removeIf(item -> item.itemId.equals(itemId));
                activeBids.remove(itemId);
                if (onItemUpdate != null) {
                    javafx.application.Platform.runLater(onItemUpdate);
                }
                agent.sendGuiMessage("Item " + itemId + " from auction " + auctionId + " sold!");
            }

            default -> {
                System.out.println("Unknown message: " + message);
            }
        }
    }

    public String getAuctionId() {
        return auctionId;
    }

    public List<ItemInfo> getItems() {
        return items;
    }

    public SocketAuctionClient getClient() {
        return auctionClient;
    }

    public void setOnItemUpdate(Runnable callback) {
        this.onItemUpdate = callback;
    }

    public boolean hasActiveBids() {
        return !activeBids.isEmpty();
    }
    
}
