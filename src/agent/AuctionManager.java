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
            String description;
            int minBid;
            int currBid;
            String itemId = item[1];
            if (item.length == 4) {
                description = item[2];
                description = description.substring(1, description.length() - 1);
                minBid = Integer.parseInt(item[3]);
                currBid = Integer.parseInt(item[4]);
            }
            else {
                description = (item[2] + " " + item[3]);
                description = description.substring(1, description.length() - 1);
                minBid = Integer.parseInt(item[4]);
                currBid = Integer.parseInt(item[5]);
            }
            ItemInfo itemInfo = new ItemInfo(auctionId, itemId, description,
                    minBid, currBid);
            items.add(itemInfo);
        }
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
                System.out.println("item update");
                String itemId = parts[1];
                int minBid = Integer.parseInt(parts[2]);
                int currBid = Integer.parseInt(parts[2]);

                // Find the matching item and update it
                for (ItemInfo item : items) {
                    if (item.itemId.equals(itemId)) {
                        item.minBid = minBid;
                        item.currBid = currBid;
                        System.out.println("Item updated: " + item);
                        break;
                    }
                }

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
