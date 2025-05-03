package shared;

public interface BankClient {
    /**
     * Registers an auction house with the bank.
     *
     * @param host the IP or hostname the auction house is running on
     * @param port the port the auction house is listening on
     * @return the bank account ID assigned to the auction house
     */
    int registerAuctionHouse(String host, int port);

    /**
     * Registers an agent with the bank.
     *
     * @param name the agent's name
     * @param initialBalance the initial amount to deposit
     * @return the bank account ID assigned to the agent
     */
    int registerAgent(String name, int initialBalance);

    /**
     * Requests the bank to block a certain amount of funds from an agent's account.
     *
     * @param agentId the agent's account ID
     * @param amount the amount to block
     * @return true if funds were successfully blocked
     */
    boolean blockFunds(int agentId, int amount);

    /**
     * Requests the bank to unblock previously blocked funds.
     *
     * @param agentId the agent's account ID
     * @param amount the amount to unblock
     */
    void unblockFunds(int agentId, int amount);

    /**
     * Transfers blocked funds from the agent to the auction house.
     *
     * @param fromAgentId the agent's account ID
     * @param toAuctionHouseId the auction house's account ID
     * @param amount the amount to transfer
     */
    void transferFunds(int fromAgentId, int toAuctionHouseId, int amount);
}

