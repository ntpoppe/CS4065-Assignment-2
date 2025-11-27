package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket; // The client socket.
    private final Server server;
    private PrintWriter out;
    public String username; // Set upon LOGIN call
    private final List<Group> joinedGroups = new ArrayList<>();

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
            var writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = writer;

            out.println("WELCOME");
            out.println("GROUPS " + getGroupListString());

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
                handleLogin(arg);
                return false;
            
            case "JOIN":
                handleJoin(arg);
                return false;

            case "MESSAGE":
                handlePost(arg);
                return false;

            case "GET_MESSAGE":
                handleGetMessage(arg);
                return false;
            
            case "USERS":
                handleUsers(arg);
                return false;
                
            case "GROUPS":
                out.println("GROUPS " + getGroupListString());
                return false;
                
            case "LEAVE":
                handleLeave(arg);
                return false;

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

    private void handleLogin(String usernameArg) {
        if (usernameArg == null || usernameArg.trim().isEmpty()) {
            out.println("ERR INVALID_USERNAME");
            return;
        }

        String requestedUsername = usernameArg.trim();

        // If already logged in, reject the login attempt
        if (this.username != null) {
            out.println("ERR ALREADY_LOGGED_IN");
            return;
        }

        // Check if username already exists (by another user)
        if (server.usernameExists(requestedUsername)) {
            out.println("ERR USERNAME_EXISTS");
            return;
        }

        // Set username
        this.username = requestedUsername;
        out.println("OK LOGIN");
    }
    
    private void handleJoin(String arg) {
        Group group = validateAndGetGroup(arg, false);
        if (group == null) {
            return;
        }
        
        if (group.hasMember(this)) {
            out.println("ERR ALREADY_JOINED");
            return;
        }
        
        group.addMember(this);
        joinedGroups.add(group);
        out.println("OK JOIN " + group.getName());
        
        // Send user list
        var usernames = group.getUsernames();
        usernames.remove(this.username);
        
        if (!usernames.isEmpty()) {
            out.println("USERS " + group.getId() + " " + String.join(",", usernames));
        } else {
            out.println("USERS " + group.getId());
        }

        // Send last 2 messages
        var lastMessages = group.getLastMessages(2);
        if (!lastMessages.isEmpty()) {
            for (Message msg : lastMessages) {
                out.println("MESSAGE_SUMMARY " + group.getId() + " " + msg.toSummaryString());
            }
        }

        // Notify other clients
        group.broadcast("USER_JOINED " + group.getId() + " " + username, this);
    }
    
    private void handleLeave(String arg) {
        Group group = validateAndGetGroup(arg, true);
        if (group == null) {
            return;
        }
        
        group.removeMember(this);
        joinedGroups.remove(group);
        out.println("OK LEAVE " + group.getName());
        group.broadcast("USER_LEFT " + group.getId() + " " + username, this);
    }

    private void handlePost(String arg) {
        // Expect: <group_id> <subject>|<content>
        String[] parts = arg.split(" ", 2);
        if (parts.length < 2) {
            out.println("ERR INVALID_FORMAT Use: MESSAGE <group_id> <content>");
            return;
        }
        
        String groupIdStr = parts[0];
        String messageBody = parts[1];
        
        Group group = validateAndGetGroup(groupIdStr, true);
        if (group == null) {
            return;
        }

        // Parse message: format is "SUBJECT|CONTENT" or just "CONTENT" (subject defaults to empty)
        String subject = "";
        String content = messageBody;

        if (messageBody.contains("|")) {
            String[] msgParts = messageBody.split("\\|", 2);
            subject = msgParts[0].trim();
            content = msgParts.length > 1 ? msgParts[1].trim() : "";
        }

        // Create and store message
        Message message = new Message(username, subject.isEmpty() ? "(no subject)" : subject, content);
        group.addMessage(message);

        // Broadcast message summary to all other clients in the group
        group.broadcast("NEW_MESSAGE " + group.getId() + " " + message.toSummaryString(), this);
        out.println("OK MESSAGE");
    }
    
    private void handleUsers(String arg) {
        Group group = validateAndGetGroup(arg, true);
        if (group == null) {
            return;
        }
        
        var usernames = group.getUsernames();
        usernames.remove(this.username); // Exclude self
         
        if (!usernames.isEmpty()) {
            out.println("USERS " + group.getId() + " " + String.join(",", usernames));
        } else {
            out.println("USERS " + group.getId());
        }
    }

    private void handleGetMessage(String arg) {
        if (username == null) {
            out.println("ERR NOT_LOGGED_IN");
            return;
        }

        try {
            int messageId = Integer.parseInt(arg.trim());
            Message message = server.getMessageById(messageId);

            if (message == null) {
                out.println("ERR MESSAGE_NOT_FOUND");
            } else {
                // Check access: User must be in the group that has this message
                boolean hasAccess = false;
                for (Group g : joinedGroups) {
                    if (g.getMessages().contains(message)) {
                        hasAccess = true;
                        break;
                    }
                }
                
                if (hasAccess) {
                    out.println(message.getContent());
                } else {
                    out.println("ERR MESSAGE_NOT_FOUND");
                }
            }
        } catch (NumberFormatException e) {
            out.println("ERR INVALID_MESSAGE_ID");
        }
    }

    /**
     * Validates user is logged in, group exists, and optionally checks membership.
     */
    private Group validateAndGetGroup(String groupIdentifier, boolean requireMembership) {
        if (username == null) {
            out.println("ERR NOT_LOGGED_IN");
            return null;
        }
        
        Group group = server.getGroup(groupIdentifier.trim());
        if (group == null) {
            out.println("ERR GROUP_NOT_FOUND");
            return null;
        }
        
        if (requireMembership && !group.hasMember(this)) {
            out.println("ERR NOT_MEMBER");
            return null;
        }
        
        return group;
    }

    // Called by Server.broadcast() or Group.broadcast()
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    private String getGroupListString() {
        StringBuilder sb = new StringBuilder();
        for (Group g : server.getGroups()) {
            sb.append(g.getId()).append(":").append(g.getName()).append(",");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
