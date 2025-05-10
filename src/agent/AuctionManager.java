package agent;

import shared.BankClient;
import shared.Message;
import shared.SocketAuctionClient;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager implements Runnable {
    
    private String auctionId;
    private SocketAuctionClient auctionClient;
    private BufferedReader in;
    private List<ItemInfo> items;
    private final BankClient bankClient;
    private Runnable onItemUpdate;

    public AuctionManager(String auctionId, SocketAuctionClient auctionClient, BankClient bankClient) {
        this.auctionId = auctionId;
        this.auctionClient = auctionClient;
        this.bankClient = bankClient;
        this.items = new ArrayList<>();
    }

    @Override
    public void run() {
        
        try {
            parseItemsList(auctionClient.getAvailableItems());
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
        
        // Reconstruct the quoted description from index 2 to length - 3
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
                String acceptMessage = new String("Bid accepted.");
//                dashboardGUI.displayMessage(acceptMessage);
            }

            case "REJECTED" -> {
                System.out.println("Bid rejected: " + (parts.length > 1 ? parts[1] : "Unknown reason"));
                // TODO: GUI callback to show rejection reason
            }

            case "OUTBID" -> {
                String itemId = parts[1];
                System.out.println("You were outbid on item " + itemId);
                // TODO: Notify GUI that user has been outbid
            }

            case "WINNER" -> {
                String itemId = parts[1];
                for (ItemInfo item : items) {
                    if (item.itemId.equals(itemId)) {
                        try {
                            int fromAgentId = auctionClient.getAgentId();
                            int toAuctionHouseId = Integer.parseInt(auctionId);
                            int amount = item.currBid;
                            
                            bankClient.transferFunds(fromAgentId, toAuctionHouseId, amount);
                            System.out.printf("Transferred $%d from agent %d to auction house %d for item %s\n",
                                    amount, fromAgentId, toAuctionHouseId, itemId);

                        } catch (Exception e) {
                            System.err.println("Failed to transfer funds: " + e.getMessage());
                        }
                        break;
                    }
                }
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
                System.out.println("Item " + itemId + " has been sold and removed from the list.");
                
                if (onItemUpdate != null) {
                    javafx.application.Platform.runLater(onItemUpdate);
                }
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
    
}
