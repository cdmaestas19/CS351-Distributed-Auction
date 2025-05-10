package auctionhouse;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemManager {
    private final Map<Integer, AuctionItem> activeItems;
    private final Queue<AuctionItem> pendingItems;
    private final List<AuctionItem> soldItems;
    private final AtomicInteger nextItemId;
    private final ScheduledExecutorService auctionTimerService = Executors.newScheduledThreadPool(4);
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public ItemManager() {
        this.activeItems = new ConcurrentHashMap<>();
        this.pendingItems = new ConcurrentLinkedQueue<>();
        this.soldItems = Collections.synchronizedList(new ArrayList<>());
        this.nextItemId = new AtomicInteger(1);
    }

    public void loadItemsFromResource(String resourceName) {
        List<AuctionItem> all = new ArrayList<>();

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
                    all.add(item);
                }
            }

            Collections.shuffle(all);
            pendingItems.addAll(all);

            for (int i = 0; i < 3 && !pendingItems.isEmpty(); i++) {
                AuctionItem item = pendingItems.poll();
                item.setActive(true);
                activeItems.put(item.getItemId(), item);
            }
        }
    }

    public List<AuctionItem> getAvailableItems() {
        return new ArrayList<>(activeItems.values());
    }

    public List<AuctionItem> getAllItems() {
        List<AuctionItem> allItems = new ArrayList<>();

        allItems.addAll(activeItems.values());
        allItems.addAll(pendingItems);
        allItems.addAll(soldItems);

        return allItems;
    }

    public AuctionItem getItem(int itemId) {
        return activeItems.get(itemId);
    }

    public void startAuctionTimer(AuctionItem item, AuctionHouse house) {
        int itemId = item.getItemId();

        ScheduledFuture<?> existing = timers.remove(itemId);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }

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
                    handler.sendWinnerNotification(itemId);
                }

                System.out.printf("Auction ended: item %d sold to agent %d for %d\n",
                        itemId, winnerId, amount);
            }
        }, 30, TimeUnit.SECONDS);

        timers.put(itemId, future);
    }


    public synchronized void markItemAsSold(int itemId, AuctionHouse house) {
        AuctionItem sold = activeItems.remove(itemId);
        if (sold != null) {
            sold.markAsSold();
            soldItems.add(sold);
            if (!pendingItems.isEmpty()) {
                AuctionItem next = pendingItems.poll();
                activeItems.put(next.getItemId(), next);
                house.broadcastItemUpdate(next);
            }
            house.triggerUpdate();
        }
    }

    public boolean hasActiveAuctions() {
        return activeItems.values().stream().anyMatch(item ->
                !item.isSold() && item.getCurrentBidderId() != -1
        );
    }
}
