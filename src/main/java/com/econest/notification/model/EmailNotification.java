package com.econest.notification.model;

import java.time.LocalDateTime;

/**
 * One email that EcoNest sent, or tried to send.
 * Each object is one row of the notifications table.
 */
public class EmailNotification {

    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";

    private int notificationId;
    private final NotificationType type;
    private final String recipientEmail;
    private final String subject;
    private final String body;
    private final String status;          // SENT or FAILED
    private final Integer orderId;        // null if the email isn't about an order
    private final LocalDateTime createdAt;
    private final LocalDateTime sentAt;   // null if it wasn't sent
    private final String errorMessage;    // why it failed, null if it was sent

    public EmailNotification(int notificationId, NotificationType type, String recipientEmail,
                             String subject, String body, String status, Integer orderId,
                             LocalDateTime createdAt, LocalDateTime sentAt, String errorMessage) {
        this.notificationId = notificationId;
        this.type = type;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.orderId = orderId;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
    }

    /** A new email that was sent just now. */
    public static EmailNotification sent(NotificationType type, String recipientEmail,
                                         String subject, String body, Integer orderId) {
        LocalDateTime now = LocalDateTime.now();
        return new EmailNotification(0, type, recipientEmail, subject, body, SENT, orderId,
                now, now, null);
    }

    /** A new email that could not be sent, with the reason. */
    public static EmailNotification failed(NotificationType type, String recipientEmail,
                                           String subject, String body, Integer orderId,
                                           String errorMessage) {
        return new EmailNotification(0, type, recipientEmail, subject, body, FAILED, orderId,
                LocalDateTime.now(), null, errorMessage);
    }

    public boolean isSent() {
        return SENT.equals(status);
    }

    public int getNotificationId() {
        return notificationId;
    }

    /** Set by the DAO once the database has given the row its id. */
    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "#" + notificationId + " " + type + " to " + recipientEmail + " (" + status + ")";
    }
}
