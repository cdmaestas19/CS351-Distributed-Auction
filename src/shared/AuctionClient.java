package shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Interface for communicating with an auction house.
 * Defines operations for connecting, retrieving item listings, placing bids, and closing the connection.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Dustin Ferguson
 * @author Christian Maestas
 * @author Isaac Tapia
 */
public interface AuctionClient {

    /**
     * Establishes a connection to the auction house server and registers the agent.
     *
     * @param host    the auction house host address
     * @param port    the auction house port
     * @param agentId the unique ID of the agent
     * @throws IOException if the connection fails or is rejected
     */
    void connect(String host, int port, int agentId) throws IOException;

    /**
     * @return the ID of the agent connected to the auction house
     */
    int getAgentId();

    /**
     * Requests a list of available auction items from the auction house.
     *
     * @return list of item data arrays received from the auction house
     * @throws IOException if communication fails
     */
    List<String[]> getAvailableItems() throws IOException;

    /**
     * Sends a bid for a specific item to the auction house.
     *
     * @param itemId the ID of the item to bid on
     * @param amount the bid amount
     * @throws IOException if communication fails
     */
    void placeBid(int itemId, int amount) throws IOException;

    /**
     * Sends a quit command and closes the socket connection.
     *
     * @throws IOException if closing the socket fails
     */
    void close() throws IOException;

    /**
     * Provides access to the socket's input stream for external listener threads.
     *
     * @return the buffered input stream from the auction house
     */
    BufferedReader getInputStream();
}