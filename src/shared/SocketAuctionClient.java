package shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles client-side communication with an auction house over a socket.
 * Used by agents to interact with remote auction houses.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Dustin Ferguson
 * @author Christian Maestas
 * @author Isaac Tapia
 */
public class SocketAuctionClient implements AuctionClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int agentId;

    /**
     * Establishes a connection to the auction house server and registers the agent.
     *
     * @param host    the auction house host address
     * @param port    the auction house port
     * @param agentId the unique ID of the agent
     * @throws IOException if the connection fails or is rejected
     */
    @Override
    public void connect(String host, int port, int agentId) throws IOException {
        this.agentId = agentId;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        out.println("AGENT " + agentId);
        String response = in.readLine();
        if (!response.startsWith("WELCOME")) {
            throw new IOException("Connection rejected: " + response);
        }
    }

    /**
     * @return the ID of the agent connected to the auction house
     */
    public int getAgentId() {
        return agentId;
    }

    /**
     * Requests a list of available auction items from the auction house.
     *
     * @return list of item data arrays received from the auction house
     * @throws IOException if communication fails
     */
    @Override
    public List<String[]> getAvailableItems() throws IOException {
        List<String[]> items = new ArrayList<>();
        out.println("LIST");

        String line;
        while ((line = in.readLine()) != null) {
            String[] parts = Message.decode(line);
            if (parts[0].equals("END_ITEMS")) break;
            if (parts[0].equals("ITEM")) {
                items.add(parts);
            }
        }
        return items;
    }

    /**
     * Sends a bid for a specific item to the auction house.
     *
     * @param itemId the ID of the item to bid on
     * @param amount the bid amount
     * @throws IOException if communication fails
     */
    @Override
    public void placeBid(int itemId, int amount) throws IOException {
        out.println(Message.encode("BID", String.valueOf(itemId), String.valueOf(amount)));
    }

    /**
     * Sends a quit command and closes the socket connection.
     *
     * @throws IOException if closing the socket fails
     */
    @Override
    public void close() throws IOException {
        if (out != null) {
            out.println("QUIT");
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Provides access to the socket's input stream for external listener threads.
     *
     * @return the buffered input stream from the auction house
     */
    @Override
    public BufferedReader getInputStream() {
        return in;
    }
}
