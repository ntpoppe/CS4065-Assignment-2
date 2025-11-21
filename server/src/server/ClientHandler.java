package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket; // The client socket.

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /*
     * Since the server is multithreaded, each client connection needs to be run independently.
     * Implementing Runnable allows this class to be executed by a thread.
     * Overriding run() lets us handle clients as we want without blocking other clients.
     */
    @Override
    public void run() {
        System.out.println("Thread handling: " + socket.getRemoteSocketAddress());

        try (
            // Initialize a reader to received text lines from the client.
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Initialize a writer to send text lines back to the client.
            var out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            out.println("SERVER MESSAGE");

            String line;

            // Continuously attempt read lines from the client until client disconnects.
            while ((line = in.readLine()) != null) {
                System.out.println("CLIENT SAID: " + line);

                // todo: remove echo, just to test responses
                out.println("ECHO: " + line);
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
