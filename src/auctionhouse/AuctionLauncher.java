package auctionhouse;

import javafx.application.Application;
import javafx.stage.Stage;
import shared.BankClient;
import shared.SocketBankClient;

/**
 * Launches the auction house by connecting to the bank.
 * Loads auction items from a configuration file.
 * <p>
 * Part of CS 351 Project 5 – Distributed Auction.
 *
 * @author Isaac Tapia
 */
public class AuctionLauncher extends Application {

    private AuctionHouse house;

    @Override
    public void start(Stage primaryStage) {
        String[] args = getParameters().getRaw().toArray(new String[0]);

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

            house = new AuctionHouse(auctionPort, bankClient, itemManager);

            new Thread(() -> house.start()).start();

            AuctionHouseGUI gui = new AuctionHouseGUI(house);
            gui.show(primaryStage);

        } catch (Exception e) {
            System.err.println("Failed to launch auction house: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
