package agent;

import shared.BankClient;
import shared.SocketBankClient;
import java.util.Scanner;

public class AgentLauncher {


    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: java AgentLauncher <bankHost> <bankPort>");
            System.exit(1);
        }

        String bankHost = args[0];
        int bankPort = Integer.parseInt(args[1]);

        try (Scanner scanner = new Scanner(System.in)) {
            BankClient bankClient = new SocketBankClient(bankHost, bankPort);

            System.out.print("Enter agent name: ");
            String agentName = scanner.nextLine();

            System.out.print("Enter starting account balance: ");
            int initialBalance = Integer.parseInt(scanner.nextLine());

            // TODO: make this a message? Uncomment:
            int agentId = bankClient.registerAgent(agentName, initialBalance);
            if (agentId < 0) {
                System.err.println("Failed to register agent with the bank.");
                return;
            }

            Agent agent = new Agent(bankHost, bankPort, agentName, agentId,
                    bankClient);
            Thread agentThread = new Thread(agent);
            agentThread.start();
            System.out.println("Registered successfully. Your account ID is: " + agentId);
            
        } catch (Exception e) {
            System.err.println("Agent startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}