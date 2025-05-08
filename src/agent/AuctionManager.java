package agent;

import shared.Message;

import java.io.*;
import java.net.Socket;

public class AuctionManager implements Runnable {
    
    private String auctionId;
    private Socket auctionSocket;
    private PrintWriter out;
    private BufferedReader in;
    
    public AuctionManager(String auctionId, Socket auctionSocket) {
        this.auctionId = auctionId;
        this.auctionSocket = auctionSocket;
    }
    
    @Override
    public void run() {
        
        try {
            in = new BufferedReader(
                    new InputStreamReader(auctionSocket.getInputStream()));
            while (true) {
                if (in.ready()) {
                    String line = in.readLine();
                    handleMessage(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    public void handleMessage(String message) throws IOException {
        
        System.out.println(message);
        String[] parts = Message.decode(message);
        
        // TODO: handle incoming messages from auctions
        switch (parts[0]) {
            case "ITEM": {
                break;
            }
            case "ACCEPTED":
            case "REJECTED":
            case "OUTBID":
            default:
                System.out.println("Agent.handleMessage() error!");
                System.out.println(message);
                break;
        }
        
        // TODO: "BID_PLACED AuctionItem Amount?"
        // TODO: "WINNER cost?" -> transferFunds, boughtItems list, update bids
    }
    
    public void bid(Socket auctionSocket, int itemID, int bidAmount) {
        
        String item = String.valueOf(itemID);
        String bid = String.valueOf(bidAmount);
        String message = Message.encode("BID", item, bid);
        
        try {
            out = new PrintWriter(auctionSocket.getOutputStream(), true);
            out.println(message);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
    
}
