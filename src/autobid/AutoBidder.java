package autobid;

import agent.Agent;
import agent.AuctionManager;
import agent.ItemInfo;

import java.util.List;
import java.util.Random;

public class AutoBidder {

    private final Agent agent;
    private final Random random = new Random();
    private volatile boolean shutdownRequested = false;
    private volatile boolean terminated = false;

    public AutoBidder(Agent agent) {
        this.agent = agent;
    }

    public void start() {
        new Thread(() -> {
            while (!shutdownRequested) {
                try {
                    Thread.sleep(6000 + random.nextInt(4000));

                    if (shutdownRequested) break;

                    List<AuctionManager> managers = agent.getAuctionManagers();
                    if (managers.isEmpty()) continue;

                    AuctionManager manager = managers.get(random.nextInt(managers.size()));
                    List<ItemInfo> items = manager.getItems();
                    if (items.isEmpty()) continue;

                    ItemInfo item = items.get(random.nextInt(items.size()));
                    int bid = item.getMinBid() + random.nextInt(50);

                    manager.getClient().placeBid(Integer.parseInt(item.getItemId()), bid);
                    System.out.printf("[AutoBidder %s] Bid $%d on item %s (auction %s)%n",
                            agent.getAgentName(), bid, item.getItemId(), item.getAuctionId());

                } catch (Exception e) {
                    Thread.yield();
                }
            }
            shutdownGracefully();
        }).start();
    }

    public void requestShutdown() {
        this.shutdownRequested = true;
    }

    public boolean isTerminated() {
        return terminated;
    }

    private void shutdownGracefully() {
        System.out.printf("[AutoBidder %s] Shutdown requested. Waiting for bids to resolve...%n", agent.getAgentName());

        while (!agent.canShutdown()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        }

        agent.shutdown();
        terminated = true;
    }
}