package com.econest.notification.dao;

import com.econest.notification.model.DispatchedOrder;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Task 8 - database operations for the "order dispatched" email to clients.
 *
 * Reads orders (with their items and delivery agent) and writes a row to the
 * shared notifications table for every email it tries to send.
 * Dispatch emails are logged with notification_type 'ORDER_STATUS_CHANGE'.
 */
public interface DispatchEmailDao {

    /** The order with its items and assigned agent, or empty if there is no such order. */
    Optional<DispatchedOrder> findOrder(int orderId) throws SQLException;

    /** True if a dispatch email for this order has already been SENT successfully. */
    boolean dispatchEmailAlreadySent(int orderId) throws SQLException;

    /** Ids of DISPATCHED orders whose client hasn't been emailed yet (e.g. after a failed send). */
    List<Integer> findOrdersAwaitingDispatchEmail() throws SQLException;

    /**
     * Records an email attempt in the notifications table.
     *
     * @param sent         true if it went out, false if it failed
     * @param errorMessage why it failed (null when sent)
     */
    void logDispatchEmail(int clientId, int orderId, String recipient, String subject, String body,
                          boolean sent, String errorMessage) throws SQLException;
}
