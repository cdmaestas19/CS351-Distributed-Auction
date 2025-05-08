package shared;

import java.io.IOException;
import java.util.List;

public interface AuctionClient {
    void connect(String host, int port, int agentId) throws IOException;

    List<String[]> getAvailableItems() throws IOException;

    boolean placeBid(int itemId, int amount) throws IOException;

    void close() throws IOException;
}