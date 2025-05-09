package bank;

/**
 * Bank Server
 *
 * @author Christian Maestas
 * @author Isaac Tapia
 * @author Dustin Ferguson
 */
public class BankServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java bank.BankServer <port>");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number.");
            System.exit(1);
            return;
        }

        Bank bank = new Bank(port);
        bank.start();
    }
}