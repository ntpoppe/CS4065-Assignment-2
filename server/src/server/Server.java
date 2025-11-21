package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // Thread safe list
    private final List<Message> messages = new CopyOnWriteArrayList<>(); // Thread safe message history
    private volatile boolean running = true; // Sets if the while loop should continue.
                                             // Also silences an infinite loop warning.

    public Server(int port) {
        this.port = port;
    }

    // Start the server. Open the ServerSocket and accept connections.
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Main accept loop: runs indefinitely
            System.out.println("Server started on port " + port);
            while (running) {
                // Wait for a client to connect (blocking call)
                Socket socket = serverSocket.accept();
                System.out.println("New client: " + socket.getRemoteSocketAddress());

                // Create a new ClientHandler to handle the client connection.
                var clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);

                // Start a new thread for the client.
                var t = new Thread(clientHandler);
                t.start();
            }
        }
    }

    // Stops the server.
    public void stop() {
        running = false;
    }

    // Called when a client is finished.
    public void remove(ClientHandler client) {
        clients.remove(client);
        if (client.username != null) {
            broadcast("USER_LEFT " + client.username);
        }
        System.out.println("Client removed. Client count: " + clients.size());
    }

    // Broadcast to all clients.
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    // Broadcast to all clients, except the sender.
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client: clients) {
            if (client == sender) continue;
            client.send(message);
        }
    }

    // Check if username already exists
    public boolean usernameExists(String username) {
        for (ClientHandler client : clients) {
            if (client.username != null && client.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    // Get list of all usernames
    public List<String> getAllUsernames() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.username != null) {
                usernames.add(client.username);
            }
        }
        return usernames;
    }

    // Get list of all usernames except the specified client
    public List<String> getAllUsernames(ClientHandler exclude) {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client != exclude && client.username != null) {
                usernames.add(client.username);
            }
        }
        return usernames;
    }

    // Get last N messages
    public List<Message> getLastMessages(int count) {
        List<Message> result = new ArrayList<>();
        int start = Math.max(0, messages.size() - count);
        for (int i = start; i < messages.size(); i++) {
            result.add(messages.get(i));
        }
        return result;
    }

    // Add a new message
    public void addMessage(Message message) {
        messages.add(message);
    }

    // Get message by ID
    public Message getMessageById(int id) {
        for (Message msg : messages) {
            if (msg.getId() == id) {
                return msg;
            }
        }
        return null;
    }
}
