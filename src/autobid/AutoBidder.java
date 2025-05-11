package autobid;
import agent.Agent;
import agent.AuctionManager;
import agent.ItemInfo;
import java.util.List;
import java.util.Random;

/**
 * Auto Bidder
 *
 * Automated Agents that will randomly bid on random open auction houses
 *
 * @author Christian Maestas
 */
public class AutoBidder {

    /**
     * Agent
     */
    private final Agent agent;
    /**
     * Random
     */
    private final Random random = new Random();
    /**
     * Shutdown request
     */
    private volatile boolean shutdownRequested = false;
    /**
     * Terminated
     */
    private volatile boolean terminated = false;

    /**
     * Auto bidder constructor
     * @param agent agent
     */
    public AutoBidder(Agent agent) {
        this.agent = agent;
    }

    /**
     * Start autobidder
     *
     * Handles cleanly shutting down and bidding randomly
     */
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
                    if (items.isEmpty()) {
                        continue;
                    }

                    ItemInfo item = items.get(random.nextInt(items.size()));
                    int bid = item.getMinBid() + random.nextInt(50);

                    manager.getClient().placeBid(Integer.parseInt(item.getItemId()), bid);
                    System.out.printf("[AutoBidder %s] Bid $%d on item %s (auction %s)%n",
                            agent.getAgentName(), bid, item.getItemId(), item.getAuctionId());

                } catch (Exception e) {
                    Thread.yield();
                }
            }
            shutdownClean();
        }).start();
    }

    /**
     * Request shutdown auto bidder. Sets shutdownRequested to true
     */
    public void requestShutdown() {
        this.shutdownRequested = true;
    }

    /**
     * Is it terminated
     * @return true if terminated, false if otherwise
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Shutdown autobidders cleanly.
     *
     * Shouldn't shut down unless all bids are complete
     */
    private void shutdownClean() {
        System.out.printf("[AutoBidder %s] Shutdown requested. Waiting for bids " +
                "to resolve...%n", agent.getAgentName());

        while (!agent.canShutdown()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        }

        agent.shutdown();
        terminated = true;
    }
}