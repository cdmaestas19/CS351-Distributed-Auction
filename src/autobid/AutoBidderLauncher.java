package autobid;
import agent.Agent;
import shared.BankClient;
import shared.SocketBankClient;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Auto bidder launcher
 *
 * Starts the auto bidders and keeps running until requested to shut down or
 * there are no more items to bid on
 */

public class AutoBidderLauncher {

    /**
     * Auto bidders
     */
    private static final List<AutoBidder> autobidders = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java autobid.AutoBidderLauncher " +
                    "<bankHost> <bankPort> <agentCount> <initialBalance>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int agentCount = Integer.parseInt(args[2]);
        int initialBalance = Integer.parseInt(args[3]);

        for (int i = 0; i < agentCount; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    BankClient bankClient = new SocketBankClient(host, port);
                    Socket bankSocket = new Socket(host, port);
                    String name = "AutoAgent" + id;
                    int agentId = bankClient.registerAgent(name, initialBalance);

                    Agent agent = new Agent(bankSocket, name, agentId, bankClient);
                    Thread agentThread = new Thread(agent);
                    agentThread.start();

                    AutoBidder autobidder = new AutoBidder(agent);
                    autobidders.add(autobidder);
                    autobidder.start();

                } catch (Exception e) {
                    System.err.println("Failed to launch autobidder " +
                            id + ": " + e.getMessage());
                }
            }).start();
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("q")) {
                    System.out.println("Waiting for autobidders to finish...");
                    for (AutoBidder a : autobidders) {
                        a.requestShutdown();
                    }

                    while (autobidders.stream().anyMatch(autoBidder ->
                            !autoBidder.isTerminated())) {
                        Thread.sleep(1000);
                    }

                    System.out.println("All autobidders have shut down. Exiting.");
                    System.exit(0);
                } else {
                    System.out.println("Type 'q' to quit.");
                }
            }
        } catch (Exception e) {
            System.err.println("Shutdown error: " + e.getMessage());
        }
    }
}