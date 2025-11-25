import java.io.*;
import java.net.*;
import java.util.*;

public final class BBClient {

    final static String CRLF = "\r\n";

    private Socket clientSocket = null;
    private BufferedReader serverReader = null;
    private PrintWriter serverWriter = null;
    private boolean connected = false;

    public static void main(String[] argv) {
        BBClient client = new BBClient();
        client.startClient();
    }

    /*
     * Main console loop
     */
    public void startClient() {
        try {
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

            printCommands();

            String command;
            while (true) {
                System.out.print("> ");
                command = userIn.readLine();
                if (command == null) continue;

                // Handle %connect separately
                if (command.startsWith("%connect")) {
                    handleConnect(command);
                    continue;
                }

                // Handle exit even when not connected
                if (command.equals("%exit")) {
                    if (connected) sendLine("QUIT");
                    disconnect();
                    break;
                }

                // Any other command requires connection
                if (!connected) {
                    System.out.println("ERROR: Not connected. Use %connect first.");
                    continue;
                }

                // Protocol-based commands
                if (command.equals("%join")) {
                    handleJoin(userIn);
                }
                else if (command.startsWith("%post")) {
                    handlePost(command);
                }
                else if (command.equals("%users")) {
                    // Users are pushed from server only during login; no direct command.
                    System.out.println("Server does not support %users command directly.");
                }
                else if (command.equals("%leave")) {
                    sendLine("QUIT");
                    disconnect();
                    break;
                }
                else if (command.startsWith("%message")) {
                    handleGetMessage(command);
                }
                else {
                    System.out.println("Unknown command. Type %help.");
                }
            }

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    private void printCommands() {
        System.out.println("Bulletin Board Client (Part 1)");
        System.out.println("Commands:");
        System.out.println("  %connect <ip> <port>");
        System.out.println("  %join");
        System.out.println("  %post <subject> | <body>");
        System.out.println("  %message <id>");
        System.out.println("  %leave");
        System.out.println("  %exit");
        System.out.println("  %help");
        System.out.println();
    }

    /* ==========================
       Connection Handling
       ========================== */

    private void handleConnect(String line) {
        try {
            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                System.out.println("Usage: %connect <ip> <port>");
                return;
            }

            String ip = parts[1];
            int port = Integer.parseInt(parts[2]);

            clientSocket = new Socket(ip, port);
            serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            connected = true;

            System.out.println("Connected to " + ip + ":" + port);

            // Start listener thread
            Thread t = new Thread(new ServerListener(serverReader, clientSocket));
            t.start();

        } catch (Exception ex) {
            System.out.println("ERROR connecting: " + ex.getMessage());
        }
    }

    private void disconnect() {
        try {
            connected = false;
            if (serverWriter != null) serverWriter.close();
            if (serverReader != null) serverReader.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Disconnected.");
        } catch (IOException ex) {
            System.out.println("ERROR during disconnect: " + ex.getMessage());
        }
    }

    private void sendLine(String line) {
        if (serverWriter != null) {
            serverWriter.println(line);
        }
    }

    /* ==========================
       Command Handlers
       ========================== */

    // %join → LOGIN <username>
    private void handleJoin(BufferedReader userIn) {
        try {
            System.out.print("Enter username: ");
            String username = userIn.readLine();

            if (username == null || username.trim().isEmpty()) {
                System.out.println("Username cannot be empty.");
                return;
            }

            sendLine("LOGIN " + username.trim());

        } catch (IOException e) {
            System.out.println("Error reading username.");
        }
    }

    // %post → MESSAGE subject|body
    private void handlePost(String command) {
        if (!command.contains("|")) {
            System.out.println("Usage: %post <subject> | <body>");
            return;
        }

        // Strip the command
        String args = command.substring(5).trim();
        String[] parts = args.split("\\|", 2);

        if (parts.length != 2) {
            System.out.println("Usage: %post <subject> | <body>");
            return;
        }

        String subject = parts[0].trim();
        String body = parts[1].trim();

        if (subject.isEmpty() || body.isEmpty()) {
            System.out.println("Subject and body cannot be empty.");
            return;
        }

        // Server expects: MESSAGE subject|content
        sendLine("MESSAGE " + subject + "|" + body);
    }

    // %message 3 → GET_MESSAGE 3
    private void handleGetMessage(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            System.out.println("Usage: %message <id>");
            return;
        }

        sendLine("GET_MESSAGE " + parts[1]);
    }
}

/* ==========================
   Listener Thread
   ========================== */

class ServerListener implements Runnable {
    private final BufferedReader in;
    private final Socket socket;

    public ServerListener(BufferedReader in, Socket socket) {
        this.in = in;
        this.socket = socket;
    }

    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("\n[SERVER] " + line);
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.out.println("Connection closed by server.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
