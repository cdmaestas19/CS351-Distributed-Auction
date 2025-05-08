package agent;

import shared.BankClient;
import shared.Message;
import shared.SocketAuctionClient;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Agent implements Runnable {

    private final String agentName;
    private final int agentID;
    private final BankClient bankSocketClient;
    private int totalBalance, availableBalance;
    private Socket bankSocket;
    private List<AuctionManager> auctionManagers = new ArrayList<>();

    
    public Agent(Socket bankSocket, String agentName, int agentID,
                 BankClient bankSocketClient) {
        this.agentName = agentName;
        this.agentID = agentID;
        this.bankSocketClient = bankSocketClient;
        this.bankSocket = bankSocket;
    }
    
    @Override
    public void run() {
        try {
            
            // Open persistent socket to Bank
            BufferedReader bankIn = new BufferedReader(new InputStreamReader(bankSocket.getInputStream()));
            
            // Notify bank this is the persistent channel for live updates
            PrintWriter bankOut = new PrintWriter(
                    new OutputStreamWriter(bankSocket.getOutputStream()), true);
            bankOut.println(Message.encode("REGISTER_AGENT_CHANNEL",
                    String.valueOf(agentID)));
            balanceInquiry(bankOut);
            
            while (true) {
                if (bankIn.ready()) {
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

    public void handleMessage(String message) throws IOException {
        
        System.out.println(message);
        String[] parts = Message.decode(message);
        
        switch (parts[0]) {
            case "BALANCE" :{
                totalBalance = Integer.parseInt(parts[1]);
                availableBalance = Integer.parseInt(parts[2]);
                break;
            }
            case "AUCTION_HOUSE" : {
                SocketAuctionClient auctionClient = new SocketAuctionClient();
                auctionClient.connect(parts[1], Integer.parseInt(parts[2]), agentID);
                System.out.println("Connection to Auction House " + parts[3] + " successful!");
                AuctionManager auctionManager = new AuctionManager(parts[3],
                        auctionClient);
                Thread thread = new Thread(auctionManager);
                thread.start();
                auctionManagers.add(auctionManager);
                break;
            }
            default:
                System.out.println("Agent.handleMessage() error!");
                System.out.println(message);
                break;
        }
        
    }
    
    public String getAgentName() {
        return agentName;
    }
    
    public int getAgentID() {
        return agentID;
    }
    
    public void balanceInquiry(PrintWriter bankOut) {
        bankOut.println(Message.encode("BALANCE", Integer.toString(agentID)));
    }
    
    
    
    
    
}
