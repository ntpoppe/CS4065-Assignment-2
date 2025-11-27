package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Group {
    private final int id;
    private final String name;
    private final List<ClientHandler> members = new CopyOnWriteArrayList<>();
    private final List<Message> messages = new CopyOnWriteArrayList<>();

    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addMember(ClientHandler client) {
        members.add(client);
    }

    public void removeMember(ClientHandler client) {
        members.remove(client);
    }

    public boolean hasMember(ClientHandler client) {
        return members.contains(client);
    }

    public List<ClientHandler> getMembers() {
        return members;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<Message> getLastMessages(int count) {
        List<Message> result = new ArrayList<>();
        int start = Math.max(0, messages.size() - count);
        for (int i = start; i < messages.size(); i++) {
            result.add(messages.get(i));
        }
        return result;
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : members) {
            if (client != sender) {
                client.send(message);
            }
        }
    }

    // Get list of all usernames in this group
    public List<String> getUsernames() {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : members) {
            if (client.username != null) {
                usernames.add(client.username);
            }
        }
        return usernames;
    }
}

