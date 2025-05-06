package auctionhouse;

/**
 * Represents a single item being auctioned.
 * Tracks bid information, bidder ID, and sale status.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Isaac Tapia
 */
public class AuctionItem {

    private final int itemId;
    private final String description;
    private final int minimumBid;

    private int currentBid;
    private int currentBidderId;
    private boolean sold;

    /**
     * Constructs an AuctionItem with the given ID, description, and minimum bid.
     * Initializes the item as unsold with no current bidder.
     *
     * @param itemId      unique identifier for the item
     * @param description text description of the item
     * @param minimumBid  minimum acceptable bid for this item
     */
    public AuctionItem(int itemId, String description, int minimumBid) {
        this.itemId = itemId;
        this.description = description;
        this.minimumBid = minimumBid;
        this.currentBid = 0;
        this.currentBidderId = -1;
        this.sold = false;
    }

    /**
     * @return the unique ID of this item
     */
    public int getItemId() {
        return itemId;
    }

    /**
     * @return the description of the item
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the minimum acceptable bid for this item
     */
    public int getMinimumBid() {
        return minimumBid;
    }

    /**
     * @return the current highest bid placed on this item
     */
    public int getCurrentBid() {
        return currentBid;
    }

    /**
     * @return the ID of the agent with the current highest bid,
     * or -1 if no bids have been placed
     */
    public int getCurrentBidderId() {
        return currentBidderId;
    }

    /**
     * @return true if the item has been sold; false otherwise
     */
    public boolean isSold() {
        return sold;
    }

    /**
     * Attempts to place a bid on the item.
     * The bid is accepted only if the item is still open and the bid is valid.
     *
     * @param agentId   the ID of the bidding agent
     * @param bidAmount the amount of the bid
     */
    public synchronized void placeBid(int agentId, int bidAmount) {
        // TODO: add real bid validation logic later
        if (sold || bidAmount <= currentBid || bidAmount < minimumBid) {
            return;
        }
        currentBid = bidAmount;
        currentBidderId = agentId;
    }

    /**
     * Marks the item as sold and prevents further bidding.
     */
    public synchronized void markAsSold() {
        this.sold = true;
    }

    /**
     * Returns a human-readable summary of the item’s status.
     */
    @Override
    public String toString() {
        return String.format(
                "Item %d: %s | Min Bid: %d | Current Bid: %d | Status: %s",
                itemId, description, minimumBid, currentBid,
                sold ? "SOLD" : "OPEN"
        );
    }
}
