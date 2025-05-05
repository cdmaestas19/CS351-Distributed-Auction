package shared;

/**
 * Utility class for encoding and decoding simple command messages.
 * Used for communication between agents, auction houses, and the bank.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Dustin Ferguson
 * @author Christian Maestas
 * @author Isaac Tapia
 */
public class Message {

    /**
     * Combines a command and optional arguments into a single space-separated string.
     *
     * @param command the command keyword (e.g., "BID", "BLOCK_FUNDS")
     * @param args    any additional arguments to append
     * @return a single-line message string
     */
    public static String encode(String command, String... args) {
        return command + " " + String.join(" ", args);
    }

    /**
     * Splits a message string into its individual tokens.
     * Trims whitespace and splits on one or more spaces.
     *
     * @param line the message line to decode
     * @return an array of tokens, where the first is the command
     */
    public static String[] decode(String line) {
        return line.trim().split("\\s+");
    }
}

