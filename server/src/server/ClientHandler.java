package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket; // The client socket.
    private final Server server;
    private PrintWriter out;
    public String username; // Set upon LOGIN call

    public ClientHandler(Socket socket,  Server server) {
        this.socket = socket;
        this.server = server;
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
            var writer = new PrintWriter(socket.getOutputStream(), true);
        ) {
            this.out = writer;

            out.println("WELCOME");

            String line;
            // Continuously attempt read lines from the client until client disconnects.
            while ((line = in.readLine()) != null) {
                System.out.println("CLIENT SAID: " + line);

                // If handleLine returns true, the client should exit.
                if (handleLine(line)) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            // Tell the server the client is gone
            server.remove(this);
            try { socket.close(); } catch (IOException ignored) {} // Ignore exception here. It's closing anyway.
            System.out.println("Client closed: " + socket.getRemoteSocketAddress());
        }
    }

    // Text protocol handler
    private boolean handleLine(String line) {
        line = line.trim();
        if (line.isEmpty()) return false;

        // Preprocessing for consistency 
        String[] parts = line.split(" ", 2);
        String cmd = parts[0].toUpperCase().trim();
        String arg = (parts.length > 1) ? parts[1] : "";

        switch (cmd) {
            case "LOGIN":
                return handleLogin(arg);

            case "MESSAGE":
                return handleMessage(arg);

            case "GET_MESSAGE":
                return handleGetMessage(arg);

            case "PING":
                out.println("PONG");
                return false;

            case "QUIT":
                out.println("BYE");
                return true;

            default:
                out.println("ERR UNKNOWN_COMMAND");
                return false;
        }
    }

    private boolean handleLogin(String usernameArg) {
        if (usernameArg == null || usernameArg.trim().isEmpty()) {
            out.println("ERR INVALID_USERNAME");
            return false;
        }

        String requestedUsername = usernameArg.trim();

        // If already logged in, reject the login attempt
        if (this.username != null) {
            out.println("ERR ALREADY_LOGGED_IN");
            return false;
        }

        // Check if username already exists (by another user)
        if (server.usernameExists(requestedUsername)) {
            out.println("ERR USERNAME_EXISTS");
            return false;
        }

        // Set username
        this.username = requestedUsername;
        out.println("OK LOGIN");

        // Send user list (excluding the current user who just joined)
        var usernames = server.getAllUsernames(this);
        if (!usernames.isEmpty()) {
            out.println("USERS " + String.join(",", usernames));
        } else {
            out.println("USERS");
        }

        // Send last 2 messages
        var lastMessages = server.getLastMessages(2);
        if (!lastMessages.isEmpty()) {
            for (Message msg : lastMessages) {
                out.println("MESSAGE_SUMMARY " + msg.toSummaryString());
            }
        }

        // Notify other clients
        server.broadcast("USER_JOINED " + username, this);
        return false;
    }

    private boolean handleMessage(String arg) {
        // Check if user is logged in
        if (username == null) {
            out.println("ERR NOT_LOGGED_IN");
            return false;
        }

        // Parse message: format is "SUBJECT|CONTENT" or just "CONTENT" (subject defaults to empty)
        String subject = "";
        String content = arg;

        if (arg.contains("|")) {
            String[] parts = arg.split("\\|", 2);
            subject = parts[0].trim();
            content = parts.length > 1 ? parts[1].trim() : "";
        }

        // Create and store message
        Message message = new Message(username, subject.isEmpty() ? "(no subject)" : subject, content);
        server.addMessage(message);

        // Broadcast message summary to all other clients
        server.broadcast("NEW_MESSAGE " + message.toSummaryString(), this);
        out.println("OK MESSAGE");
        return false;
    }

    private boolean handleGetMessage(String arg) {
        // Check if user is logged in
        if (username == null) {
            out.println("ERR NOT_LOGGED_IN");
            return false;
        }

        try {
            int messageId = Integer.parseInt(arg.trim());
            Message message = server.getMessageById(messageId);

            if (message == null) {
                out.println("ERR MESSAGE_NOT_FOUND");
            } else {
                out.println(message.getContent());
            }
        } catch (NumberFormatException e) {
            out.println("ERR INVALID_MESSAGE_ID");
        }

        return false;
    }

    // Called by Server.broadcast()
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
