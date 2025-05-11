package agent;

public class ItemInfo {
    
    public String AuctionId;
    public String itemId;
    public String description;
    public int minBid, currBid;
    
    
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

    public String getAuctionId() {
        return AuctionId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getMinBid() {
        return minBid;
    }

    public int getCurrBid() {
        return currBid;
    }
}
