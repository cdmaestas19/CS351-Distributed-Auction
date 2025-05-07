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
    private final Socket socket;
    private final Map<Integer, Account> accounts;
    private final List<String> auctionHouses;
    private final AtomicInteger idGenerator;
    private static final List<PrintWriter> agentWriters =
            Collections.synchronizedList(new ArrayList<>());
    private static final Map<Integer, PrintWriter> agentIdToWriter = new ConcurrentHashMap<>();


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
                    case "REGISTER_AGENT_CHANNEL" -> {
                        int agentId = Integer.parseInt(parts[1]);
                        agentWriters.add(out);
                        agentIdToWriter.put(agentId, out);
                        System.out.printf("Registered persistent agent channel for agent ID %d\n", agentId);

                        // Send existing auction houses
                        synchronized (auctionHouses) {
                            for (String address : auctionHouses) {
                                String[] split = address.split(":");
                                out.println(Message.encode("AUCTION_HOUSE", split[0], split[1]));
                                System.out.printf("Sent existing auction house %s to agent %d (via channel)\n", address, agentId);
                            }
                        }
                    }
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

        agentWriters.add(out);
        agentIdToWriter.put(id, out);
        synchronized (auctionHouses) {
            for (String address : auctionHouses) {
                String[] split = address.split(":");
                out.println(Message.encode("AUCTION_HOUSE", split[0], split[1]));
                System.out.printf("Sent existing auction house %s to agent %d\n", address, id);
            }
        }

    }

    private void handleHouseRegistration(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid register format");
            return;
        }

        String host = parts[1];
        String port = parts[2];
        String address = host;

        int id = idGenerator.getAndIncrement();
        accounts.put(id, new Account(id, address, false, 0));
        auctionHouses.add(address);
        System.out.println("Added house " + address);
        System.out.printf("Broadcasting new auction house %s to %d agent(s)\n", address, agentWriters.size());

        synchronized (agentWriters) {
            for (PrintWriter writer : agentWriters) {
                writer.println(Message.encode("AUCTION_HOUSE", host, port));
                System.out.printf("sent message");
            }
        }
//        String msg = Message.encode("AUCTION_HOUSE", host, port);
//        System.out.printf("Broadcasting new auction house %s to %d agent(s)\n", address, agentWriters.size());
//
//        synchronized (agentWriters) {
//            for (Map.Entry<Integer, PrintWriter> entry : agentIdToWriter.entrySet()) {
//                int agentId = entry.getKey();
//                PrintWriter agentOut = entry.getValue();
//                agentOut.println(msg);
//                System.out.printf("  → Sent to agent ID %d: %s\n", agentId, msg);
//            }
//        }
//
//        out.println("OK " + id);
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
