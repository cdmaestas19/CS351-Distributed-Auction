package bank;

public class Account {
    public final int id;
    public final String name;
    public final boolean isAgent;
    public int totalBalance;
    public int blockedFunds;

    public Account(int id, String name, boolean isAgent, int initialBalance) {
        this.id = id;
        this.name = name;
        this.isAgent = isAgent;
        this.totalBalance = initialBalance;
        this.blockedFunds = 0;
    }

    public int getAvailableBalance() {
        return totalBalance - blockedFunds;
    }
}