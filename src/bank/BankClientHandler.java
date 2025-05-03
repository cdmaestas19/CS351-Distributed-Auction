package bank;
import shared.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = Message.decode(line);
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "REGISTER_AUCTION_HOUSE" -> handleHouseRegistration(parts, out);
                    case "REGISTER_AGENT" -> handleAgentRegistration(parts, out);
                    // TODO: handle agents, funds, etc.
                    default -> out.println("ERROR Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Bank client connection error: " + e.getMessage());
        }
    }

    private void handleAgentRegistration(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid agent registration format");
            return;
        }

        String name = parts[1];
        int initialBalance;

        try {
            initialBalance = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println("ERROR Invalid balance amount");
            return;
        }

        int id = idGenerator.getAndIncrement();
        accounts.put(id, new Account(id, name, true, initialBalance));
        out.println("OK " + id);
    }

    private void handleHouseRegistration(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid register format");
            return;
        }

        String host = parts[1];
        String port = parts[2];
        String address = host + ":" + port;

        int id = idGenerator.getAndIncrement();
        accounts.put(id, new Account(id, address, false, 0));
        auctionHouses.add(address);

        out.println("OK " + id);
    }
}
