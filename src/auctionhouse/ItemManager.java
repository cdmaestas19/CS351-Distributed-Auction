package auctionhouse;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the lifecycle of auction items.
 * Tracks active, pending, and sold items and handles auction timing logic.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Isaac Tapia
 */
public class ItemManager {

    private final Map<Integer, AuctionItem> activeItems;
    private final Queue<AuctionItem> pendingItems;
    private final List<AuctionItem> soldItems;
    private final AtomicInteger nextItemId;
    private final ScheduledExecutorService auctionTimerService =
            Executors.newScheduledThreadPool(4);
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    /**
     * Initializes item containers and ID counter.
     */
    public ItemManager() {
        this.activeItems = new ConcurrentHashMap<>();
        this.pendingItems = new ConcurrentLinkedQueue<>();
        this.soldItems = Collections.synchronizedList(new ArrayList<>());
        this.nextItemId = new AtomicInteger(1);
    }

    /**
     * Loads auction items from a resource file and populates the pending queue.
     * Initially activates 3 items for auction.
     *
     * @param resourceName the name of the item resource file (e.g., "items.txt")
     */
    public void loadItemsFromResource(String resourceName) {
        List<AuctionItem> all = new ArrayList<>();

        try (Scanner scanner = new Scanner(
                Objects.requireNonNull(getClass().getClassLoader().
                        getResourceAsStream(resourceName)))) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String desc = parts[0].trim();
                    int minBid = Integer.parseInt(parts[1].trim());
                    AuctionItem item = new AuctionItem(nextItemId.getAndIncrement(),
                            desc, minBid);
                    all.add(item);
                }
            }

            Collections.shuffle(all);
            pendingItems.addAll(all);

            // Activate up to 3 items initially
            for (int i = 0; i < 3 && !pendingItems.isEmpty(); i++) {
                AuctionItem item = pendingItems.poll();
                item.setActive(true);
                activeItems.put(item.getItemId(), item);
            }
        }
    }

    /**
     * @return a list of currently active items
     */
    public List<AuctionItem> getAvailableItems() {
        return new ArrayList<>(activeItems.values());
    }

    /**
     * @return a combined list of all items (active, pending, and sold)
     */
    public List<AuctionItem> getAllItems() {
        List<AuctionItem> allItems = new ArrayList<>();

        allItems.addAll(activeItems.values());
        allItems.addAll(pendingItems);
        allItems.addAll(soldItems);

        return allItems;
    }

    /**
     * Returns an active item by ID.
     *
     * @param itemId the item ID
     * @return the item, or null if not active
     */
    public AuctionItem getItem(int itemId) {
        return activeItems.get(itemId);
    }

    /**
     * Starts or restarts the auction timer for a given item.
     * When time expires, the highest bidder is declared the winner.
     *
     * @param item  the item being auctioned
     * @param house reference to the AuctionHouse for callback purposes
     */
    public void startAuctionTimer(AuctionItem item, AuctionHouse house) {
        int itemId = item.getItemId();

        // Cancel any existing timer
        ScheduledFuture<?> existing = timers.remove(itemId);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }

        // Schedule auction to end in 30 seconds
        ScheduledFuture<?> future = auctionTimerService.schedule(() -> {
            synchronized (item) {
                if (item.isSold()) return;

                int winnerId = item.getCurrentBidderId();
                int amount = item.getCurrentBid();
                if (winnerId == -1) return;

                item.markAsSold();
                markItemAsSold(itemId, house);

                AgentHandler handler = house.getAgentHandler(winnerId);
                if (handler != null) {
                    handler.sendWinnerNotification(amount, itemId);
                }

                System.out.printf("Auction ended: item %d sold to agent %d for %d\n",
                        itemId, winnerId, amount);
            }
        }, 30, TimeUnit.SECONDS);

        timers.put(itemId, future);
    }

    /**
     * Marks an item as sold, replaces it with a pending item (if any), and updates the UI.
     *
     * @param itemId the ID of the item that was sold
     * @param house  the auction house managing this item
     */
    public synchronized void markItemAsSold(int itemId, AuctionHouse house) {
        AuctionItem sold = activeItems.remove(itemId);
        if (sold != null) {
            sold.markAsSold();
            soldItems.add(sold);

            house.broadcastItemSold(itemId);

            // Replace with a pending item, if available
            if (!pendingItems.isEmpty()) {
                AuctionItem next = pendingItems.poll();
                activeItems.put(next.getItemId(), next);
                house.broadcastItemUpdate(next);
            }
            house.triggerUpdate();
        }
    }

    /**
     * Checks if there are any items with active bids still in progress.
     *
     * @return true if at least one auction is active; false otherwise
     */
    public boolean hasActiveAuctions() {
        return activeItems.values().stream().anyMatch(item ->
                !item.isSold() && item.getCurrentBidderId() != -1
        );
    }
}
