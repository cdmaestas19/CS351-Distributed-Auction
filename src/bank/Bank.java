package bank;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bank
 *
 * Handles running the Bank Server and Listening for clients to connect.
 * Main Bank where Agents and Auction Houses hold their money
 *
 * @author Christian Maestas
 */
public class Bank {
    /**
     * Port
     */
    private final int port;
    /**
     * Server Socket
     */
    private ServerSocket serverSocket;
    /**
     * Client thread pool
     */
    private final ExecutorService clientThreadPool;
    /**
     * Is Bank running?
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /**
     * Accounts
     */
    private final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    /**
     * Next account id
     */
    private final AtomicInteger initAccountID = new AtomicInteger(1000);

    /**
     * Bank constructor
     * @param port port
     */
    public Bank(int port) {
        this.port = port;
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Start the Bank server
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            System.out.println("Bank server listening on port " + port);
            listenForClients();
        } catch (IOException e) {
            System.err.println("Bank server failed to start: " + e.getMessage());
        }
    }

    /**
     * Listen for incoming clients to connect to server
     */
    private void listenForClients() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();

                clientThreadPool.submit(new BankClientHandler(
                        clientSocket,
                        accounts,
                        initAccountID
                ));
                System.out.println("Bank client accepted: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
}