package bank;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BankServer {
    private static final int PORT = 44444;
    private static final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    private static final List<String> auctionHouseAddresses = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger nextAccountId = new AtomicInteger(1000);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Bank server running on port " + PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new BankClientHandler(clientSocket, accounts, auctionHouseAddresses, nextAccountId)).start();
        }
    }
}

