package auctionhouse;

import shared.BankClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AgentHandler implements Runnable {

    private final Socket socket;
    private final ItemManager itemManager;
    private final BankClient bankClient;

    private BufferedReader in;
    private PrintWriter out;

    public AgentHandler(Socket socket, ItemManager itemManager, BankClient bankClient) {
        this.socket = socket;
        this.itemManager = itemManager;
        this.bankClient = bankClient;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // TODO: Handle agent communication loop here

        } catch (IOException e) {
            System.err.println("Agent communication error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing agent socket: " + e.getMessage());
        }
    }
}
