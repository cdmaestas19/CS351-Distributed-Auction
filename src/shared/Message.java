package shared;


/**
 * Command/message formats used in this project:
 * "BID itemId bidAmount"  (agent -> auctionHouse): place bid
 * "REJECTED reason"       (auctionHouse -> agent): bid rejected
 * "ACCEPTED"              (auctionHouse -> agent): bid accepted
 * "LIST"                  (agent -> auctionHouse): request list of items
 */
public class Message {
    public static String encode(String command, String... args) {
        return command + " " + String.join(" ", args);
    }

    public static String[] decode(String line) {
        return line.trim().split("\\s+");
    }
}
