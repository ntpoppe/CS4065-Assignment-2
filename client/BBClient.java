/**
 * Project 2 - Bulletin Board Client (Part 1)
 * Styled like templateA.java, task1.java, and FtpClient.java
 */
import java.io.*;
import java.net.*;
import java.util.*;

public final class BBClient {

    final static String CRLF = "\r\n";

    private Socket clientSocket = null;
    private BufferedReader serverReader = null;
    private DataOutputStream serverWriter = null;
    private boolean connected = false;

    public static void main(String argv[]) throws Exception {
        BBClient client = new BBClient();
        client.startClient();
    }

    /*
     * Console loop — reads % commands from the user.
     */
    public void startClient() {
        try {
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Bulletin Board Client Started.");
            System.out.println("Commands:");
            System.out.println("  %connect <ip> <port>");
            System.out.println("  %join");
            System.out.println("  %post <subject> <body>");
            System.out.println("  %users");
            System.out.println("  %leave");
            System.out.println("  %message <id>");
            System.out.println("  %exit");
            System.out.println();

            String command;
            while (true) {
                System.out.print("> ");
                command = userIn.readLine();
                if (command == null) continue;

                if (command.startsWith("%connect")) {
                    handleConnect(command);
                } else if (!connected) {
                    System.out.println("ERROR: Not connected. Use %connect first.");
                } else {
                    // All other commands are sent directly to server
                    sendLine(command + CRLF);

                    if (command.equals("%exit")) {
                        disconnect();
                        break;
                    }
                }
            }

        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
        }
    }

    /*
     * Handle %connect <ip> <port>
     */
    private void handleConnect(String line) {
        try {
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken(); // skip %connect

            if (st.countTokens() < 2) {
                System.out.println("Usage: %connect <ip> <port>");
                return;
            }

            String ip = st.nextToken();
            int port = Integer.parseInt(st.nextToken());

            // Establish socket
            clientSocket = new Socket(ip, port);
            serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            serverWriter = new DataOutputStream(clientSocket.getOutputStream());
            connected = true;

            System.out.println("Connected to server at " + ip + ":" + port);

            // Start server listener thread (templateA style)
            ServerListener listener = new ServerListener(clientSocket, serverReader);
            Thread t = new Thread(listener);
            t.start();

        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    /*
     * Sends a line to the server using DataOutputStream
     */
    private void sendLine(String msg) {
        try {
            serverWriter.writeBytes(msg);
            serverWriter.flush();
        } catch (IOException ex) {
            System.out.println("IOException sending message: " + ex);
        }
    }

    /*
     * Close everything cleanly
     */
    private void disconnect() {
        try {
            connected = false;
            if (serverReader != null) serverReader.close();
            if (serverWriter != null) serverWriter.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Disconnected.");
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

}


/*
 * Background server-listener thread
 * Styled like HttpRequest runnable in templateA/task1.
 */
final class ServerListener implements Runnable {

    Socket socket;
    BufferedReader in;

    public ServerListener(Socket socket, BufferedReader in) {
        this.socket = socket;
        this.in = in;
    }

    public void run() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("[SERVER] " + response);
            }
        } catch (IOException e) {
            System.out.println("Server connection closed.");
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
