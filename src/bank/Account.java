package bank;

/**
 * Bank Account
 *
 * @author Christian Maestas
 */
public class Account {

    /**
     * Account ID
     */
    private final int id;
    /**
     * Name
     */
    private final String name;
    /**
     * Is an Agent account?
     */
    public final boolean isAgent;
    /**
     * Total Balance
     */
    private int totalBalance;
    /**
     * Blocked Funds
     */
    private int blockedFunds;

    /**
     * Account constructor with an ID, name, and initial balance
     * @param id ID
     * @param name name
     * @param isAgent is agent
     * @param initialBalance initial balance
     */
    public Account(int id, String name, boolean isAgent, int initialBalance) {
        this.id = id;
        this.name = name;
        this.isAgent = isAgent;
        this.totalBalance = initialBalance;
        this.blockedFunds = 0;
    }

    /**
     * Get available balance
     * @return available balance
     */
    public int getAvailableBalance() {
        return totalBalance - blockedFunds;
    }

    /**
     * Get account ID
     * @return account ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get account name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get total balance
     * @return total balance
     */
    public int getTotalBalance() {
        return totalBalance;
    }

    /**
     * Get blocked funds
     * @return blocked funds
     */
    public int getBlockedFunds() {
        return blockedFunds;
    }

    /**
     * Set blocked funds
     * @param amount amount
     */
    public void setBlockedFunds(int amount) {
        this.blockedFunds += amount;
    }

    /**
     * Set total balance
     * @param amount amount
     */
    public void setTotalBalance(int amount) {
        this.totalBalance += amount;
    }
}