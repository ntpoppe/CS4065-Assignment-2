package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;
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
                // TODO: Create a thread for each client connection.
                var clientHandler = new ClientHandler(socket);
                clientHandler.run();
            }
        }
    }

    // Stops the server.
    public void stop() {
        running = false;
    }
}
