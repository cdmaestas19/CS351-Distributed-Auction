package shared;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketAuctionClient implements AuctionClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    @Override
    public void connect(String host, int port, int agentId) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        out.println("AGENT " + agentId);
        String response = in.readLine();
        if (!response.startsWith("WELCOME")) {
            throw new IOException("Connection rejected: " + response);
        }
    }

    @Override
    public List<String[]> getAvailableItems() throws IOException {
        List<String[]> items = new ArrayList<>();
        out.println("LIST");

        String line;
        while ((line = in.readLine()) != null) {
            String[] parts = Message.decode(line);
            if (parts[0].equals("END_ITEMS")) break;
            if (parts[0].equals("ITEM")) {
                items.add(parts);
            }
        }
        return items;
    }

    @Override
    public boolean placeBid(int itemId, int amount) throws IOException {
        out.println(Message.encode("BID", String.valueOf(itemId), String.valueOf(amount)));
        String response = in.readLine();
        return response != null && response.startsWith("ACCEPTED");
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.println("QUIT");
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    public BufferedReader getInputStream(){
        return in;
    }
}
