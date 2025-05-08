package agent;

public class ItemInfo {
    
    String AuctionId;
    String itemId;
    String description;
    int minBid, currBid;
    
    
    public ItemInfo (String AuctionId, String itemId, String description,
                     int minBid, int currBid) {
        
        this.AuctionId = AuctionId;
        this.itemId = itemId;
        this.description = description;
        this.minBid = minBid;
        this.currBid = currBid;
        
    }
    
    @Override
    public String toString() {
        return ("Item " + itemId + ": " + description);
    }
}
