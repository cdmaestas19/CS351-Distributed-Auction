package autobid;

import agent.Agent;
import shared.BankClient;
import shared.SocketBankClient;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AutoBidderLauncher {
    private static final List<AutoBidder> autobidders = new ArrayList<>();
    private static volatile boolean shutdownRequested = false;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java autobid.AutoBidderLauncher <bankHost> <bankPort> <agentCount> <initialBalance>");
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

                    AutoBidder autobidder = new AutoBidder(agent, bankClient);
                    autobidders.add(autobidder);
                    autobidder.start();

                } catch (Exception e) {
                    System.err.println("Failed to launch autobidder " + id + ": " + e.getMessage());
                }
            }).start();
        }

        // Enhanced terminal listener with shutdown monitoring
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Type 'q' to request shutdown. Type 'status' to see active agents.");
                String input = scanner.nextLine().trim().toLowerCase();

                if (input.equals("q")) {
                    if (!shutdownRequested) {
                        shutdownRequested = true;
                        System.out.println("Shutdown requested. Signaling autobidders...");
                        autobidders.forEach(AutoBidder::requestShutdown);
                    } else {
                        System.out.println("Shutdown already requested. Waiting for agents to finish...");
                    }
                } else if (input.equals("status")) {
                    int remaining = (int) autobidders.stream().filter(a -> !a.isTerminated()).count();
                    System.out.printf("Still %d autobidders resolving their bids...\n", remaining);
                }

                // Exit once all agents terminate
                if (shutdownRequested && autobidders.stream().allMatch(AutoBidder::isTerminated)) {
                    System.out.println("All autobidders shut down. Exiting.");
                    break;
                }

                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}