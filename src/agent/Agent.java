package agent;

import auctionhouse.AuctionItem;
import shared.BankClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Agent implements Runnable {

    private final String agentName;
    private final int agentID;
    private BankClient bank;
    private int totalBalance, availableBalance;
    private final List<Socket> auctionSockets = new ArrayList<>();
    private final List<BufferedReader> auctionInputs = new ArrayList<>();
    private ArrayList<AuctionItem> currentAuctionsItems;
    private BufferedReader bankIn;

    public Agent(String agentName, int agentID, BankClient bankSocket ) {
        this.agentName = agentName;
        this.agentID = agentID;
        this.bank = bank;

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
        // TODO: send request message to bank ("REQUEST_BALANCES account#"?)
    }

    public void requestAuctionHouses() {
        // TODO: send request message to bank ("REQUEST_AUCTIONS")
    }

    public void requestItems() {
        // TODO: send request to auction house ("ITEMS_REQUEST"?)
    }

    public void submitBid() {
        // TODO: send bid message ("BID AuctionItem BidAmount"?)
    }

    public void handleMessage(String message) {
        // TODO: handle incoming messages from bank/auctions
        // "BID_ACCEPTED availableBalance?" -> update balance, update bids
        // "BID_REJECTED AuctionItem reasonMessage?" -> display message
        // "OUTBID AuctionItem returnedBidAmount" ->
        //          update balance, update bids, display message
        // "BID_PLACED AuctionItem Amount?"
        // "WINNER cost?" -> transferFunds, boughtItems list, update bids
        // "BALANCE totalBalance availableBalance?" -> update balance
    }

}
