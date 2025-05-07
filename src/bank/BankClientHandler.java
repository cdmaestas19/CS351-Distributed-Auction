package bank;

import shared.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BankClientHandler implements Runnable {
    //TODO: delete any debugging wth agentIdToWriter
    private final Socket socket;
    private final Map<Integer, Account> accounts;
    private static final Map<Integer, String> auctionHouseAddresses = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator;
    private static final List<PrintWriter> agentWriters =
            Collections.synchronizedList(new ArrayList<>());
    private static final Map<Integer, PrintWriter> agentIdToWriter = new ConcurrentHashMap<>();


    public BankClientHandler(Socket socket, Map<Integer, Account> accounts,
                             Map<Integer, String> auctionHouseAddresses, AtomicInteger idGenerator) {
        this.socket = socket;
        this.accounts = accounts;
        this.idGenerator = idGenerator;
    }

    @Override
    public void run() {
        try (
                PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = Message.decode(line);
                if (parts.length == 0) {
                    continue;
                }

                switch (parts[0]) {
                    case "REGISTER_AUCTION_HOUSE" ->
                            handleHouseRegistration(parts, out);
                    case "REGISTER_AGENT" ->
                            handleAgentRegistration(parts, out);
                    case "BLOCK_FUNDS" -> blockFunds(parts, out);
                    case "UNBLOCK_FUNDS" -> unblockFunds(parts, out);
                    case "TRANSFER_FUNDS" -> transferFunds(parts, out);
                    case "REGISTER_AGENT_CHANNEL" -> handleAgentChannel(parts, out);
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

        agentWriters.add(out);
        agentIdToWriter.put(id, out);
    }

    private void handleHouseRegistration(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid register format");
            return;
        }

        String host = parts[1];
        int port = Integer.parseInt(parts[2]);
        int id = idGenerator.getAndIncrement();

        accounts.put(id, new Account(id, host, false, 0));
        auctionHouseAddresses.put(port, host);
        System.out.println(auctionHouseAddresses.size());

        System.out.printf("Auction house registered: %s:%d\n", host, port);

        String msg = Message.encode("AUCTION_HOUSE", host, String.valueOf(port));
        for (Map.Entry<Integer, PrintWriter> entry : agentIdToWriter.entrySet()) {
            int agentId = entry.getKey();
            PrintWriter writer = entry.getValue();
            writer.println(msg);
            writer.flush();
            System.out.printf("→ Notified agent ID %d of new auction house %s:%d\n", agentId, host, port);
        }

        out.println("OK " + id);
    }

    private void handleAgentChannel(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("ERROR Invalid REGISTER_AGENT_CHANNEL format");
            return;
        }

        int agentId = Integer.parseInt(parts[1]);
        agentWriters.add(out);
        agentIdToWriter.put(agentId, out);
        System.out.printf("Registered persistent channel for agent ID %d\n", agentId);

        int counter = 1;
        for (Map.Entry<Integer, String> entry : auctionHouseAddresses.entrySet()) {
            System.out.println(counter);
            String host = entry.getValue();
            int port = entry.getKey();
            String msg = Message.encode("AUCTION_HOUSE", host, String.valueOf(port));
            out.println(msg);
            out.flush();
            System.out.printf("→ Sent existing auction house %s:%d to agent %d\n", host, port, agentId);
            counter++;
        }
    }


    private void blockFunds(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid BLOCK_FUNDS format");
            return;
        }
        int agentId = Integer.parseInt(parts[1]);
        int amount = Integer.parseInt(parts[2]);

        Account acc = accounts.get(agentId);
        if (acc == null || !acc.isAgent) {
            out.println("ERROR Invalid agent ID");
            return;
        }

        synchronized (acc) {
            if (acc.getAvailableBalance() < amount) {
                out.println("ERROR Insufficient funds");
            } else {
                acc.blockedFunds += amount;
                out.println("OK");
            }
        }
    }

    private void unblockFunds(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid UNBLOCK_FUNDS format");
            return;
        }
        int agentId = Integer.parseInt(parts[1]);
        int amount = Integer.parseInt(parts[2]);

        Account acc = accounts.get(agentId);
        if (acc != null && acc.isAgent) {
            synchronized (acc) {
                acc.blockedFunds -= amount;
                if (acc.blockedFunds < 0) acc.blockedFunds = 0;
            }
        }
        out.println("OK");
    }

    private void transferFunds(String[] parts, PrintWriter out) {
        if (parts.length != 4) {
            out.println("ERROR Invalid TRANSFER_FUNDS format");
            return;
        }

        int fromId = Integer.parseInt(parts[1]);
        int toId = Integer.parseInt(parts[2]);
        int amount = Integer.parseInt(parts[3]);

        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);

        if (from == null || !from.isAgent || to == null || to.isAgent) {
            out.println("ERROR Invalid account IDs");
            return;
        }

        synchronized (from) {
            if (from.blockedFunds < amount) {
                out.println("ERROR Not enough blocked funds");
                return;
            }
            from.blockedFunds -= amount;
            from.totalBalance -= amount;
        }

        synchronized (to) {
            to.totalBalance += amount;
        }

        out.println("OK");
    }
}
