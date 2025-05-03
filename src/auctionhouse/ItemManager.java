package auctionhouse;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemManager {

    private final Map<Integer, AuctionItem> activeItems;
    private final Queue<AuctionItem> pendingItems;
    private final AtomicInteger nextItemId;

    public ItemManager() {
        this.activeItems = new ConcurrentHashMap<>();
        this.pendingItems = new ConcurrentLinkedQueue<>();
        this.nextItemId = new AtomicInteger(1);
    }

    public void loadItemsFromResource(String resourceName) throws IOException {
        try (Scanner scanner = new Scanner(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(resourceName)))) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String desc = parts[0].trim();
                    int minBid = Integer.parseInt(parts[1].trim());
                    AuctionItem item = new AuctionItem(nextItemId.getAndIncrement(), desc, minBid);
                    pendingItems.add(item);
                }
            }

            for (int i = 0; i < 3 && !pendingItems.isEmpty(); i++) {
                AuctionItem item = pendingItems.poll();
                activeItems.put(item.getItemId(), item);
            }
        }
    }

    public List<AuctionItem> getAvailableItems() {
        return new ArrayList<>(activeItems.values());
    }

    public AuctionItem getItem(int itemId) {
        return activeItems.get(itemId);
    }

    public synchronized void markItemAsSold(int itemId) {
        AuctionItem sold = activeItems.remove(itemId);
        if (sold != null) {
            sold.markAsSold();
            if (!pendingItems.isEmpty()) {
                AuctionItem next = pendingItems.poll();
                activeItems.put(next.getItemId(), next);
            }
        }
    }

    public boolean hasActiveAuctions() {
        return !activeItems.isEmpty();
    }
}
