package shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles communication with the bank over TCP sockets.
 * Implements all bank operations for auction houses and agents.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction
 *
 * @author Dustin Ferguson
 * @author Christian Maestas
 * @author Isaac Tapia
 */
public class SocketBankClient implements BankClient {

    private final String bankHost;
    private final int bankPort;

    /**
     * Constructs a new SocketBankClient to communicate with the bank.
     *
     * @param bankHost the bank's hostname or IP address
     * @param bankPort the bank's listening port
     */
    public SocketBankClient(String bankHost, int bankPort) {
        this.bankHost = bankHost;
        this.bankPort = bankPort;
    }

    /**
     * Registers an auction house with the bank.
     *
     * @param host the IP or hostname the auction house is running on
     * @param port the port the auction house is listening on
     * @return the bank account ID assigned to the auction house
     */
    @Override
    public int registerAuctionHouse(String host, int port) {
        String msg = Message.encode("REGISTER_AUCTION_HOUSE", host, String.valueOf(port));
        String response = sendMessage(msg);

        if (response != null && response.startsWith("OK")) {
            return Integer.parseInt(response.split(" ")[1]);
        } else {
            return -1;
        }
    }

    /**
     * Registers an agent with the bank.
     *
     * @param name           the agent's name
     * @param initialBalance the initial amount to deposit
     * @return the bank account ID assigned to the agent
     */
    @Override
    public int registerAgent(String name, int initialBalance) {
        String msg = Message.encode("REGISTER_AGENT", name, String.valueOf(initialBalance));
        String response = sendMessage(msg);
        if (response != null && response.startsWith("OK")) {
            return Integer.parseInt(response.split(" ")[1]);
        } else {
            return -1;
        }
    }

    /**
     * Sends a deregistration request to the bank for the given account ID.
     * Used when the agent or auction house is shutting down.
     *
     * @param id the account ID to deregister
     */
    @Override
    public void deregister(int id) {
        String msg = (Message.encode("DEREGISTER", String.valueOf(id)));
        sendMessage(msg);
    }

    /**
     * Requests the bank to block a certain amount of funds from an agent's account.
     *
     * @param agentId the agent's account ID
     * @param amount  the amount to block
     * @return true if funds were successfully blocked
     */
    @Override
    public boolean blockFunds(int agentId, int amount) {
        String msg = Message.encode("BLOCK_FUNDS", String.valueOf(agentId), String.valueOf(amount));
        String response = sendMessage(msg);
        return response != null && response.startsWith("OK");
    }

    /**
     * Requests the bank to unblock previously blocked funds.
     *
     * @param agentId the agent's account ID
     * @param amount  the amount to unblock
     */
    @Override
    public void unblockFunds(int agentId, int amount) {
        String msg = Message.encode("UNBLOCK_FUNDS", String.valueOf(agentId), String.valueOf(amount));
        sendMessage(msg);
    }

    /**
     * Transfers blocked funds from the agent to the auction house.
     *
     * @param fromAgentId      the agent's account ID
     * @param toAuctionHouseId the auction house's account ID
     * @param amount           the amount to transfer
     */
    @Override
    public void transferFunds(int fromAgentId, int toAuctionHouseId, int amount) {
        String msg = Message.encode("TRANSFER_FUNDS", String.valueOf(fromAgentId),
                String.valueOf(toAuctionHouseId), String.valueOf(amount));
        sendMessage(msg);
    }

    /**
     * Sends a single-line message to the bank and returns the response.
     * Opens a socket to the bank host/port, sends the message, and reads one reply.
     *
     * @param message the message to send
     * @return the bank's response, or null if an I/O error occurred
     */
    private String sendMessage(String message) {
        try (
                Socket socket = new Socket(bankHost, bankPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println(message);
            return in.readLine();
        } catch (IOException e) {
            System.err.println("Bank communication error: " + e.getMessage());
            return null;
        }
    }
}
