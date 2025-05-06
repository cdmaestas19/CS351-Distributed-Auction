package agent;

import auctionhouse.AuctionItem;
import shared.BankClient;
import shared.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Agent implements Runnable {

    private final String agentName;
    private final int agentID;
    private BankClient bankSocket;
    private int totalBalance, availableBalance;
    private final List<Socket> auctionSockets = new ArrayList<>();
    private final List<BufferedReader> auctionInputs = new ArrayList<>();
    private ArrayList<ArrayList<AuctionItem>> AuctionItems;
    private BufferedReader bankIn;
    private PrintWriter out;

    public Agent(String agentName, int agentID, BankClient bankSocket ) {
        this.agentName = agentName;
        this.agentID = agentID;
        this.bankSocket = bankSocket;
    }
    
    // TODO: turn GUI testing stuff off:
    public Agent(String agentName, int agentID) {
        this.agentName = agentName;
        this.agentID = agentID;
    }

    @Override
    public void run() {
        try {
            while (true) {
                for (BufferedReader in : auctionInputs) {
                    if (in.ready()) {
                        String line = in.readLine();
                        handleMessage(line);
                    }
                }
                if (bankIn != null && bankIn.ready()) {
                    String bankMessage = bankIn.readLine();
                    handleMessage(bankMessage);
                }

                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println("Agent message loop error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void connectToAuction(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            auctionSockets.add(socket);
            auctionInputs.add(in);
            System.out.println("Connected to auction house at " + host + ":" + port);
        } catch (Exception e) {
            System.err.println("Failed to connect to auction house: " + e.getMessage());
        }
    }

    public void getBalances() {
        // TODO: send request message to bank ("BALANCE_INQUIRY account#"?)
    }

    public void requestAuctionHouses() {
        // TODO: send request message to bank ("REQUEST_AUCTIONS")
    }

    public void requestItems(Socket auctionSocket) {
        try {
            out = new PrintWriter(auctionSocket.getOutputStream(), true);
            out.println("LIST");
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void handleMessage(String message) {
        
        String[] parts = Message.decode(message);
        
        // TODO: handle incoming messages from bank/auctions
        switch (parts[0]) {
            case "ITEM" :
            case "ACCEPTED" :
            case "REJECTED" :
            case "OUTBID" :
        }
        
        // TODO: "BID_PLACED AuctionItem Amount?"
        // TODO: "WINNER cost?" -> transferFunds, boughtItems list, update bids
        // TODO: "BALANCE totalBalance availableBalance?" -> update balance
        // TODO: "AUCTION_HOUSE houseID host port?" -> add auctionHouse to list
    }

    
    public String getAgentName() {
        return agentName;
    }
    
    public int getAgentID() {
        return agentID;
    }
}
