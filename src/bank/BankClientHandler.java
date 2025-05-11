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

/**
 * Bank Client Handler
 *
 * Handles interactions between the Bank and any clients, being Agents and
 * Auction Houses
 *
 * @author Christian Maestas
 */

public class BankClientHandler implements Runnable {
    /**
     * Socket
     */
    private final Socket socket;
    /**
     * All Bank accounts
     */
    private final Map<Integer, Account> accounts;
    /**
     * ID generator for Bank accounts
     */
    private final AtomicInteger idGenerator;
    /**
     * Auction House naming generator
     */
    private static final AtomicInteger houseNames = new AtomicInteger(1);
    /**
     * Agent Print Writers
     */
    private static final List<PrintWriter> agentWriters =
            Collections.synchronizedList(new ArrayList<>());
    /**
     * Agent ID to Writer
     */
    private static final Map<Integer, PrintWriter> agentIdToWriter = new ConcurrentHashMap<>();
    /**
     * Auction House Addresses and names
     */
    private static final Map<Integer, String> auctionHouseAddresses =
            new ConcurrentHashMap<>();


    /**
     * BankClientHandler constructor
     * @param socket socket
     * @param accounts accounts
     * @param idGenerator id generator
     */
    public BankClientHandler(Socket socket, Map<Integer, Account> accounts, AtomicInteger idGenerator) {
        this.socket = socket;
        this.accounts = accounts;
        this.idGenerator = idGenerator;
    }

    /**
     * Listen for messages from clients to deal with auction bids
     */
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
                    case "BALANCE" -> handleBalance(parts, out);
                    case "DEREGISTER" -> handleDeregister(parts, out);
                }
            }
        } catch (IOException e) {
            System.err.println("Bank client connection error: " + e.getMessage());
        }
    }

    /**
     * When Agent registers with the Bank, it provides a name and an initial
     * balance and is given an account ID/number
     * @param parts parts of a message
     * @param out output stream
     */
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
        System.out.println("Agent registered: " + name);
        out.println("OK " + id);

        agentWriters.add(out);
        agentIdToWriter.put(id, out);
    }

    /**
     * When an auction house registers with the bank, it will provide its host
     * and port information and will be given a unique account ID/number
     * @param parts parts of message
     * @param out output stream
     */
    private void handleHouseRegistration(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid register format");
            return;
        }

        String host = parts[1];
        int port = Integer.parseInt(parts[2]);
        int id = idGenerator.getAndIncrement();
        String name = "AuctionHouse" + houseNames.getAndIncrement();

        accounts.put(id, new Account(id, name, false, 0));

        String value = (host + ":" + port);
        auctionHouseAddresses.put(id, value);
        System.out.println("Auction house registered: " + name);

        String msg = Message.encode("AUCTION_HOUSE", host, String.valueOf(port),
                String.valueOf(id));
        for (Map.Entry<Integer, PrintWriter> entry : agentIdToWriter.entrySet()) {
            PrintWriter writer = entry.getValue();
            writer.println(msg);
            writer.flush();
        }

        out.println("OK " + id);
    }

    /**
     * Creates a persistent channel of communication between the Agent and
     * Bank to ensure messages are properly passed.
     * When the channel is created, the Bank will send the Agent a list of
     * all open Auction Houses
     * @param parts parts of message
     * @param out output stream
     */
    private void handleAgentChannel(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("ERROR Invalid Message format");
            return;
        }

        int agentId = Integer.parseInt(parts[1]);
        agentWriters.add(out);
        agentIdToWriter.put(agentId, out);

        for (Map.Entry<Integer, String> entry : auctionHouseAddresses.entrySet()) {
            String[] split = entry.getValue().split(":");
            String host = split[0];
            int port = Integer.parseInt(split[1]);
            String id = String.valueOf(entry.getKey());

            String msg = Message.encode("AUCTION_HOUSE", host, String.valueOf(port), id);
            out.println(msg);
            out.flush();
        }
    }


    /**
     * Block funds when an Agent makes a successful bid
     * @param parts parts of message
     * @param out output stream
     */
    private void blockFunds(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid BLOCK_FUNDS format");
            return;
        }
        int agentId = Integer.parseInt(parts[1]);
        int amount = Integer.parseInt(parts[2]);

        Account account = accounts.get(agentId);
        if (account == null || !account.isAgent) {
            out.println("ERROR Invalid agent ID");
            return;
        }

        PrintWriter writer = agentIdToWriter.get(agentId);

        synchronized (account) {
            if (account.getAvailableBalance() < amount) {
                out.println("ERROR Insufficient funds");
            } else {
                account.setBlockedFunds(amount);
                System.out.println("Blocked Funds: " + account.getName());
                out.println("OK");
                writer.println(Message.encode("BALANCE", String.valueOf(account.getTotalBalance()),
                        String.valueOf(account.getAvailableBalance())));
            }
        }
    }

    /**
     * Unblock funds from Agent account
     * @param parts parts of message
     * @param out output stream
     */
    private void unblockFunds(String[] parts, PrintWriter out) {
        if (parts.length != 3) {
            out.println("ERROR Invalid UNBLOCK_FUNDS format");
            return;
        }
        int agentId = Integer.parseInt(parts[1]);
        int amount = Integer.parseInt(parts[2]);

        Account account = accounts.get(agentId);
        if (account != null && account.isAgent) {
            synchronized (account) {
                account.setBlockedFunds(-amount);
                System.out.println("Unblocked Funds: " + amount + "from " + account.getName());
                out.println("OK");
                if (account.getBlockedFunds() < 0) {
                    account.setBlockedFunds(0);
                }
            }
        }
    }

    /**
     * Transfer funds from an Agent to the Auction House in a successful bid
     * @param parts parts of message
     * @param out output stream
     */
    private void transferFunds(String[] parts, PrintWriter out) {
        System.out.println("Reached transfer funds");
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
            if (from.getBlockedFunds() < amount) {
                out.println("ERROR Not enough blocked funds");
                return;
            }
            //Remove funds from Agent blocked and total balance
            System.out.println(from.getTotalBalance() + "before transfer");
            from.setBlockedFunds(-amount);
            from.setTotalBalance(-amount);
            System.out.println(from.getTotalBalance() + "after transfer");
            out.println(Message.encode("BALANCE", String.valueOf(from.getTotalBalance()),
                    String.valueOf(from.getAvailableBalance())));
        }

        synchronized (to) {
            //Transfer to Auction House account
            System.out.println(to.getTotalBalance() + "before transfer");
            to.setTotalBalance(amount);
            System.out.println(to.getTotalBalance());
        }
        System.out.println("Funds transferred from: " + from.getName() + " to "
                + to.getName());
    }

    /**
     * Provide a client with their account total balance and available balance
     * @param parts parts of message
     * @param out output stream
     */
    private void handleBalance(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("ERROR Invalid BALANCE format");
            return;
        }

        int agentId;
        try {
            agentId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("ERROR Invalid agent ID");
            return;
        }

        Account account = accounts.get(agentId);
        if (account == null || !account.isAgent) {
            out.println("ERROR Agent account not found");
            return;
        }

        int total = account.getTotalBalance();
        int available = account.getAvailableBalance();

        out.println(Message.encode("BALANCE", String.valueOf(total),
                String.valueOf(available)));
    }

    private void handleDeregister(String[] parts, PrintWriter out) {
        if (parts.length != 2) {
            out.println("ERROR Invalid DEREGISTER format");
            return;
        }

        int clientID;
        try {
            clientID = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            out.println("ERROR Invalid client ID");
            return;
        }

        Account account = accounts.get(clientID);
        if (account == null) {
            out.println("ERROR Account not found");
            return;
        }

        if (account.isAgent) {
            agentIdToWriter.remove(clientID);
            agentWriters.removeIf(writer -> writer.equals(agentIdToWriter.get(clientID)));
            out.println("OK");
            System.out.printf("Agent %d deregistered.\n", clientID);
        } else {
            String hostPort = auctionHouseAddresses.remove(clientID);
            if (hostPort != null) {

                String msg = Message.encode("REMOVE_AUCTION_HOUSE",
                        String.valueOf(clientID));
                for (PrintWriter writer : agentIdToWriter.values()) {
                    writer.println(msg);
                    writer.flush();
                }
            }
            out.println("OK");
        }
    }
}