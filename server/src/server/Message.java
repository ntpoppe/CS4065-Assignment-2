package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private static int nextId = 1;
    private final int id;
    private final String sender;
    private final LocalDateTime postDate;
    private final String subject;
    private final String content;

    public Message(String sender, String subject, String content) {
        this.id = nextId++;
        this.sender = sender;
        this.postDate = LocalDateTime.now();
        this.subject = subject;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public LocalDateTime getPostDate() {
        return postDate;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    // Format: "id|sender|date|subject"
    public String toSummaryString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return id + "|" + sender + "|" + postDate.format(formatter) + "|" + subject;
    }
}

