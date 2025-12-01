import java.io.*;
import java.net.*;

public final class BBClient {

    private Socket clientSocket = null;
    private BufferedReader serverReader = null;
    private PrintWriter serverWriter = null;
    private boolean connected = false;

    public static void main(String[] argv) {
        BBClient client = new BBClient();
        client.startClient();
    }

    public void startClient() {
        try {
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
            printCommands();

            String input;
            while (true) {
                System.out.print("> ");
                input = userIn.readLine();
                if (input == null) continue;

                if (input.startsWith("%connect")) {
                    handleConnect(input);
                    continue;
                }

                if (input.equals("%exit")) {
                    if (connected) send("QUIT");
                    disconnect();
                    break;
                }

                if (input.equals("%help")) {
                    printCommands();
                    continue;
                }

                if (!connected) {
                    System.out.println("ERROR: Not connected. Use %connect first.");
                    continue;
                }

                /* ============================
                   PART 1 COMMANDS
                   ============================ */

                if (input.equals("%join")) {
                    handleLogin(userIn);
                }
                else if (input.startsWith("%post")) {
                    handlePost(input);
                }
                else if (input.startsWith("%message")) {
                    handleMessageRetrieve(input);
                }
                else if (input.equals("%leave")) {
                    send("LEAVE 1"); // Leaving default group #1 (assignment part 1)
                }

                /* ============================
                   PART 2 COMMANDS
                   ============================ */

                else if (input.equals("%groups")) {
                    send("GROUPS");
                }
                else if (input.startsWith("%groupjoin")) {
                    handleGroupJoin(input);
                }
                else if (input.startsWith("%grouppost")) {
                    handleGroupPost(input);
                }
                else if (input.startsWith("%groupusers")) {
                    handleGroupUsers(input);
                }
                else if (input.startsWith("%groupleave")) {
                    handleGroupLeave(input);
                }
                else if (input.startsWith("%groupmessage")) {
                    handleGroupMessage(input);
                }
                else {
                    System.out.println("Unknown command. Type %help.");
                }
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    /* ==========================================================
       PRINT HELP MENU
       ========================================================== */

    private void printCommands() {
        System.out.println("Bulletin Board Client");
        System.out.println("Commands:");
        System.out.println("  %connect <ip> <port>");
        System.out.println("  %join");
        System.out.println("  %post <subject> | <body>");
        System.out.println("  %message <id>");
        System.out.println("  %leave");
        System.out.println();
        System.out.println("Part 2:");
        System.out.println("  %groups");
        System.out.println("  %groupjoin <group_id>");
        System.out.println("  %groupusers <group_id>");
        System.out.println("  %grouppost <group_id> <subject> | <body>");
        System.out.println("  %groupleave <group_id>");
        System.out.println("  %groupmessage <id>");
        System.out.println();
        System.out.println("  %exit");
        System.out.println("  %help");
    }

    

    private void handleConnect(String line) {
        try {
            String[] p = line.split("\\s+");
            if (p.length != 3) {
                System.out.println("Usage: %connect <ip> <port>");
                return;
            }

            String ip = p[1];
            int port = Integer.parseInt(p[2]);

            clientSocket = new Socket(ip, port);
            serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            connected = true;

            System.out.println("Connected to " + ip + ":" + port);

            Thread t = new Thread(new ServerListener(serverReader, clientSocket));
            t.start();

        } catch (Exception e) {
            System.out.println("ERROR connecting: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            connected = false;
            if (serverWriter != null) serverWriter.close();
            if (serverReader != null) serverReader.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Disconnected.");
        } catch (IOException ignored) {}
    }

    private void send(String msg) {
        if (serverWriter != null) serverWriter.println(msg);
    }

    //part 1

    private void handleLogin(BufferedReader userIn) {
        try {
            System.out.print("Enter username: ");
            String username = userIn.readLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                return;
            }
            send("LOGIN " + username);
        } catch (IOException e) {
            System.out.println("Error reading username.");
        }
    }

    private void handlePost(String cmd) {
        if (!cmd.contains("|")) {
            System.out.println("Usage: %post <subject> | <body>");
            return;
        }
        String args = cmd.substring(5).trim();
        String[] parts = args.split("\\|", 2);
        if (parts.length != 2) {
            System.out.println("Usage: %post <subject> | <body>");
            return;
        }
        String subject = parts[0].trim();
        String body = parts[1].trim();

        // Part 1 always posts to GROUP 1
        send("MESSAGE 1 " + subject + "|" + body);
    }

    private void handleMessageRetrieve(String cmd) {
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %message <id>");
            return;
        }
        send("GET_MESSAGE " + p[1]);
    }

    //part 3

    private void handleGroupJoin(String cmd) {
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupjoin <group_id>");
            return;
        }
        send("JOIN " + p[1]);
    }

    private void handleGroupUsers(String cmd) {
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupusers <group_id>");
            return;
        }
        send("USERS " + p[1]);
    }

    private void handleGroupLeave(String cmd) {
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupleave <group_id>");
            return;
        }
        send("LEAVE " + p[1]);
    }

    private void handleGroupPost(String cmd) {
        if (!cmd.contains("|")) {
            System.out.println("Usage: %grouppost <group_id> <subject> | <body>");
            return;
        }

        // Remove "%grouppost"
        String remainder = cmd.substring(11).trim();

        // remainder should be "<group_id> <subject> | <body>"
        String[] p = remainder.split("\\s+", 2);
        if (p.length < 2) {
            System.out.println("Usage: %grouppost <group_id> <subject> | <body>");
            return;
        }

        String groupId = p[0];
        String rest = p[1];

        String[] parts = rest.split("\\|", 2);
        if (parts.length != 2) {
            System.out.println("Usage: %grouppost <group_id> <subject> | <body>");
            return;
        }

        String subject = parts[0].trim();
        String body = parts[1].trim();

        send("MESSAGE " + groupId + " " + subject + "|" + body);
    }

    private void handleGroupMessage(String cmd) {
        String[] p = cmd.split("\\s+");
        if (p.length != 2) {
            System.out.println("Usage: %groupmessage <id>");
            return;
        }
        send("GET_MESSAGE " + p[1]);
    }
}

