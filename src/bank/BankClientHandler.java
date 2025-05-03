package bank;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BankClientHandler implements Runnable {
    private final Socket socket;
    private final Map<Integer, Account> accounts;
    private final List<String> auctionHouses;
    private final AtomicInteger idGenerator;

    public BankClientHandler(Socket socket, Map<Integer, Account> accounts,
                             List<String> auctionHouses, AtomicInteger idGenerator) {
        this.socket = socket;
        this.accounts = accounts;
        this.auctionHouses = auctionHouses;
        this.idGenerator = idGenerator;
    }

    @Override
    public void run() {
        //Implement this
    }

    private void handleAgentRegistration(String[] parts, PrintWriter out) {
    }

    private void handleHouseRegistration(String[] parts, PrintWriter out) {
    }
}
