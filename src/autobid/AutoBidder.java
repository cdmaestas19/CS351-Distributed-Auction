package autobid;

import agent.Agent;
import agent.AuctionManager;
import agent.ItemInfo;
import shared.BankClient;

import java.util.List;
import java.util.Random;

public class AutoBidder {

    private final Agent agent;
    private final BankClient bankClient;
    private final Random random = new Random();
    private volatile boolean shutdown = false;
    private volatile boolean terminated = false;

    public AutoBidder(Agent agent, BankClient bankClient) {
        this.agent = agent;
        this.bankClient = bankClient;
    }

    public void start() {
        new Thread(() -> {
            while (!shutdown) {
                try {
                    Thread.sleep(6000 + random.nextInt(4000)); // 6–10 sec

                    if (shutdown) break;

                    List<AuctionManager> managers = agent.getAuctionManagers();
                    if (managers.isEmpty()) continue;

                    AuctionManager manager = managers.get(random.nextInt(managers.size()));
                    List<ItemInfo> items = manager.getItems();
                    if (items.isEmpty()) continue;

                    ItemInfo item = items.get(random.nextInt(items.size()));
                    int minBid = item.getMinBid();
                    int bid = minBid + random.nextInt(50);

                    manager.getClient().placeBid(Integer.parseInt(item.getItemId()), bid);
                    System.out.printf("[AutoBidder %s] Bid $%d on item %s (auction %s)\n",
                            agent.getAgentName(), bid, item.getItemId(), item.getAuctionId());

                } catch (Exception e) {
                    Thread.yield();
                }
            }

            // Wait until all bids by this agent are resolved before exiting
            waitForNoActiveBidsAndExit();
        }).start();
    }

    public void requestShutdown() {
        this.shutdown = true;
    }

    private void waitForNoActiveBidsAndExit() {
        System.out.printf("[AutoBidder %s] Waiting for active bids to resolve...\n", agent.getAgentName());

        while (true) {
            boolean hasActiveBid = agent.getAuctionManagers().stream()
                    .flatMap(m -> m.getItems().stream())
                    .anyMatch(item -> !item.getItemId().isEmpty() &&
                            item.getCurrBid() > 0 &&
                            agent.getAgentID() == item.getCurrBid());

            if (!hasActiveBid) break;

            try {
                Thread.sleep(2000); // wait and poll again
            } catch (InterruptedException ignored) {}
        }

        try {
            bankClient.deregisterAuctionHouse(agent.getAgentID());
            System.out.printf("[AutoBidder %s] Deregistered and exiting.\n", agent.getAgentName());
        } catch (Exception e) {
            System.err.printf("[AutoBidder %s] Failed to deregister: %s\n", agent.getAgentName(), e.getMessage());
        }
        terminated = true;
    }

    public boolean isTerminated() {
        return terminated;
    }
}