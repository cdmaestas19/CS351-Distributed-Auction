package agent;

/**
 * The item info needed for agent GUI display
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction
 *
 * @author Dustin Ferguson
 */
public class ItemInfo {
    
    public String AuctionId;
    public String itemId;
    public String description;
    public int minBid, currBid;
    
    
    /**
     * Item info constructor
     * @param AuctionId the id of the auction house the item belongs to
     * @param itemId the ID number of the item
     * @param description item description
     * @param minBid the minimum bid allowed for the item
     * @param currBid the current bid on the item
     */
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
    
    /**
     * @return the id of the auction house the item belongs to
     */
    public String getAuctionId() {
        return AuctionId;
    }
    
    /**
     * @return the ID number of the item
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * @return the minimum bid allowed for the item
     */
    public int getMinBid() {
        return minBid;
    }
    
    /**
     * @return the current bid on the item
     */
    public int getCurrBid() {
        return currBid;
    }
}
