package auctionhouse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemManager {

    private final Map<Integer, AuctionItem> items;
    private final AtomicInteger nextItemId;

    public ItemManager() {
        this.items = new ConcurrentHashMap<>();
        this.nextItemId = new AtomicInteger(1);

        // TODO: initialize with test items or load from config later
    }

    public int addItem(String description, int minimumBid) {
        int id = nextItemId.getAndIncrement();
        AuctionItem item = new AuctionItem(id, description, minimumBid);
        items.put(id, item);
        return id;
    }

    public List<AuctionItem> getAvailableItems() {
        List<AuctionItem> available = new ArrayList<>();
        for (AuctionItem item : items.values()) {
            if (!item.isSold()) {
                available.add(item);
            }
        }
        return available;
    }

    public AuctionItem getItem(int itemId) {
        return items.get(itemId);
    }

    public boolean hasActiveAuctions() {
        for (AuctionItem item : items.values()) {
            if (!item.isSold()) {
                return true;
            }
        }
        return false;
    }

    public void startAuctionTimer(AuctionItem item) {
        // TODO: implement delayed marking of item as sold
    }
}
