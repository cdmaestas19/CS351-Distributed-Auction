package auctionhouse;

import shared.BankClient;
import shared.SocketBankClient;

public class AuctionLauncher {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java AuctionLauncher <bankHost> <bankPort> <auctionPort>");
            System.exit(1);
        }

        String bankHost = args[0];
        int bankPort = Integer.parseInt(args[1]);
        int auctionPort = Integer.parseInt(args[2]);

        try {
            BankClient bankClient = new SocketBankClient(bankHost, bankPort);

            ItemManager itemManager = new ItemManager();
            itemManager.loadItemsFromResource("items.txt");

            AuctionHouse house = new AuctionHouse(auctionPort, bankClient, itemManager);
            house.start();

        } catch (Exception e) {
            System.err.println("Failed to launch auction house: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
