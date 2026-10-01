package com.econest.notification.model;

import java.time.LocalDateTime;

public class EmailNotification {

    private int id;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String body;
    private LocalDateTime sentAt;
    private boolean sent;

    public EmailNotification() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
}
