package shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketBankClient implements BankClient {

    private final String bankHost;
    private final int bankPort;

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
