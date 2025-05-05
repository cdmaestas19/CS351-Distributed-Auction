package auctionhouse;

public class AuctionItem {

    private final int itemId;
    private final String description;
    private final int minimumBid;

    private int currentBid;
    private int currentBidderId;
    private boolean sold;

    public AuctionItem(int itemId, String description, int minimumBid) {
        this.itemId = itemId;
        this.description = description;
        this.minimumBid = minimumBid;
        this.currentBid = 0;
        this.currentBidderId = -1;
        this.sold = false;
    }

    public int getItemId() {
        return itemId;
    }

    public String getDescription() {
        return description;
    }

    public int getMinimumBid() {
        return minimumBid;
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public int getCurrentBidderId() {
        return currentBidderId;
    }

    public boolean isSold() {
        return sold;
    }

    public synchronized void placeBid(int agentId, int bidAmount) {
        // TODO: add real bid validation logic later
        if (sold || bidAmount <= currentBid || bidAmount < minimumBid) {
            return;
        }
        currentBid = bidAmount;
        currentBidderId = agentId;
    }

    public synchronized void markAsSold() {
        this.sold = true;
    }

    @Override
    public String toString() {
        return String.format(
                "Item %d: %s | Min Bid: %d | Current Bid: %d | Status: %s",
                itemId, description, minimumBid, currentBid,
                sold ? "SOLD" : "OPEN"
        );
    }
}
