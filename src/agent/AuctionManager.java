package agent;

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
    
    public AuctionManager(String auctionId, SocketAuctionClient auctionClient) {
        this.auctionId = auctionId;
        this.auctionClient = auctionClient;
        items = new ArrayList<>();
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
            
            String itemId = item[1];
            String description = (item[2] + " " + item[3]);
            description = description.substring(1, description.length() - 1);
            int minBid = Integer.parseInt(item[4]);
            int currBid = Integer.parseInt(item[5]);
            ItemInfo itemInfo = new ItemInfo(auctionId, itemId, description,
                    minBid, currBid);
            items.add(itemInfo);
        }
    }
    
    public void handleMessage(String message) throws IOException {
        
        System.out.println(message);
        String[] parts = Message.decode(message);
        
        // TODO: handle incoming messages from auctions
        switch (parts[0]) {
            case "ACCEPTED":
            case "REJECTED":
            case "OUTBID":
            case "BID_PLACED" :
            case "WINNER" :
            default:
                System.out.println("Agent.handleMessage() error!");
                System.out.println(message);
                break;
        }
    }
    
}
