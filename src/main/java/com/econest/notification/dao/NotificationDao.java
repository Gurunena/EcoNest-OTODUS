package com.econest.notification.dao;

import com.econest.notification.model.EmailNotification;

import java.sql.SQLException;
import java.util.List;

/**
 * Saves and reads the notifications table: the history of every email
 * EcoNest has sent or tried to send.
 *
 * It's an interface so the tests can use a fake list instead of MySQL.
 */
public interface NotificationDao {

    /** Saves one email (sent or failed) and gives it the id the database chose. */
    void save(EmailNotification notification) throws SQLException;

    /** Every email, newest first. */
    List<EmailNotification> findAll() throws SQLException;

    /** Emails whose recipient, subject, type or status contains the text. */
    List<EmailNotification> search(String text) throws SQLException;

    /** How many emails were sent successfully. */
    int countSent() throws SQLException;
}
