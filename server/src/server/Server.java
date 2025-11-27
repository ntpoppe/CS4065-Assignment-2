package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // Thread safe list
    private final List<Group> groups = new CopyOnWriteArrayList<>(); // Thread safe group list
    private volatile boolean running = true; // Sets if the while loop should continue.
                                             // Also silences an infinite loop warning.

    public Server(int port) {
        this.port = port;
        initGroups();
    }

    private void initGroups() {
        groups.add(new Group(1, "Group 1"));
        groups.add(new Group(2, "Group 2"));
        groups.add(new Group(3, "Group 3"));
        groups.add(new Group(4, "Group 4"));
        groups.add(new Group(5, "Group 5"));
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
        // Remove from all groups
        for (Group group : groups) {
            if (group.hasMember(client)) {
                group.removeMember(client);
                if (client.username != null) {
                    group.broadcast("USER_LEFT " + client.username, client);
                }
            }
        }
        System.out.println("Client removed. Client count: " + clients.size());
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

    // Get list of all groups
    public List<Group> getGroups() {
        return groups;
    }
    
    // Get group by ID or Name
    public Group getGroup(String identifier) {
        for (Group g : groups) {
            if (String.valueOf(g.getId()).equals(identifier) || g.getName().equalsIgnoreCase(identifier)) {
                return g;
            }
        }
        return null;
    }

    // Get message by ID
    public Message getMessageById(int id) {
        for (Group group : groups) {
            for (Message msg : group.getMessages()) {
                if (msg.getId() == id) {
                    return msg;
                }
            }
        }
        return null;
    }
}
