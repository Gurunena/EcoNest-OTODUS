package com.econest.notification.service;

import com.econest.client.dao.ClientDao;
import com.econest.client.dao.ClientDaoImpl;
import com.econest.client.model.Client;
import com.econest.notification.dao.DispatchEmailDao;
import com.econest.notification.dao.DispatchEmailDaoImpl;
import com.econest.notification.model.DispatchedOrder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Task 8 - emails a client when their order has been dispatched.
 *
 * An order counts as dispatched when its status is DISPATCHED, which happens when the
 * Delivery Assignment screen assigns it to an agent. That code calls
 * {@link #notifyOrderDispatched(int)} straight after the assignment.
 *
 * Rules:
 *  - only DISPATCHED orders are emailed
 *  - each order's client is emailed only once
 *  - every attempt, sent or failed, is saved in the notifications table
 *  - a problem with email never stops the assignment: this class never throws,
 *    it returns a message saying what happened instead
 *
 * The email itself is sent by Member D's EmailService (Task 9); this class decides
 * WHAT to send and WHEN.
 */
public class ClientNotificationService {

    public static final String DISPATCHED = "DISPATCHED";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final DispatchEmailDao dispatchDao;
    private final ClientDao clientDao;
    private final EmailService emailService;

    /** Normal constructor: real database and real email. */
    public ClientNotificationService() {
        this(new DispatchEmailDaoImpl(), new ClientDaoImpl(), new EmailService());
    }

    /** Lets tests pass in fakes instead of MySQL and Gmail. */
    public ClientNotificationService(DispatchEmailDao dispatchDao, ClientDao clientDao, EmailService emailService) {
        this.dispatchDao = dispatchDao;
        this.clientDao = clientDao;
        this.emailService = emailService;
    }

    /**
     * Emails the client of this order to say it's on its way.
     *
     * @return a one-line message for the status bar, e.g. "Dispatch email sent to amara@gmail.com."
     */
    public String notifyOrderDispatched(int orderId) {
        try {
            Optional<DispatchedOrder> found = dispatchDao.findOrder(orderId);
            if (found.isEmpty()) {
                return "No dispatch email: order #" + orderId + " was not found.";
            }
            DispatchedOrder order = found.get();
            if (!DISPATCHED.equals(order.getStatus())) {
                return "No dispatch email: order #" + orderId + " is " + order.getStatus() + ", not DISPATCHED.";
            }
            if (dispatchDao.dispatchEmailAlreadySent(orderId)) {
                return "The client was already emailed about order #" + orderId + ".";
            }
            Optional<Client> client = clientDao.findById(order.getClientId());
            if (client.isEmpty()) {
                return "No dispatch email: the client of order #" + orderId + " no longer exists.";
            }
            return sendAndLog(order, client.get());

        } catch (SQLException e) {
            System.err.println("ClientNotificationService: database problem for order #" + orderId + " - " + e);
            return "Could not send the dispatch email for order #" + orderId + " (database problem).";
        }
    }

    /**
     * Sends the dispatch email for every DISPATCHED order whose client hasn't been
     * emailed yet - useful after email was down for a while.
     *
     * @return a summary, e.g. "2 dispatch email(s) sent, 1 failed."
     */
    public String sendMissedDispatchEmails() {
        try {
            List<Integer> orderIds = dispatchDao.findOrdersAwaitingDispatchEmail();
            int sent = 0;
            int failed = 0;
            for (int orderId : orderIds) {
                String result = notifyOrderDispatched(orderId);
                if (result.startsWith("Dispatch email sent")) {
                    sent++;
                } else {
                    failed++;
                }
            }
            return sent + " dispatch email(s) sent, " + failed + " not sent.";
        } catch (SQLException e) {
            System.err.println("ClientNotificationService: could not find orders to email - " + e);
            return "Could not check for missed dispatch emails (database problem).";
        }
    }

    // -----------------------------------------------------------------
    // Building and sending the email
    // -----------------------------------------------------------------

    private String sendAndLog(DispatchedOrder order, Client client) throws SQLException {
        String subject = buildSubject(order);
        String body = buildBody(order, client);
        try {
            emailService.sendEmail(client.getEmail(), subject, body);
        } catch (Exception e) {
            // e.g. no internet, wrong Gmail password, or the email settings are missing
            String reason = e.getMessage() == null ? e.toString() : e.getMessage();
            dispatchDao.logDispatchEmail(client.getClientId(), order.getOrderId(), client.getEmail(),
                    subject, body, false, reason);
            return "Dispatch email to " + client.getEmail() + " was NOT sent: " + reason;
        }
        dispatchDao.logDispatchEmail(client.getClientId(), order.getOrderId(), client.getEmail(),
                subject, body, true, null);
        return "Dispatch email sent to " + client.getEmail() + ".";
    }

    public static String buildSubject(DispatchedOrder order) {
        return "Your EcoNest order #" + order.getOrderId() + " is on its way";
    }

    /**
     * Writes the email. Uses polymorphism: getEmailGreeting() and getDeliveryNote()
     * give different text for household and office clients.
     */
    public static String buildBody(DispatchedOrder order, Client client) {
        String address = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : client.getAddress();

        StringBuilder body = new StringBuilder();
        body.append(client.getEmailGreeting()).append("\n\n");
        body.append("Good news! Your EcoNest order #").append(order.getOrderId())
                .append(" has been dispatched and is on its way.\n\n");

        if (order.getOrderDate() != null) {
            body.append("Ordered on   : ").append(order.getOrderDate().format(DATE)).append("\n");
        }
        List<String> items = order.getItems();
        for (int i = 0; i < items.size(); i++) {
            body.append(i == 0 ? "Items        : " : "               ").append(items.get(i)).append("\n");
        }
        body.append("Order total  : ").append(formatRupees(order.getTotalAmount())).append("\n");
        body.append("Deliver to   : ").append(address).append("\n");
        if (order.getAgentName() != null) {
            body.append("Delivered by : ").append(order.getAgentName());
            if (order.getAgentPhone() != null) {
                body.append(" (").append(order.getAgentPhone()).append(")");
            }
            body.append("\n");
        }

        body.append("\n").append(client.getDeliveryNote()).append("\n\n");
        body.append("Thank you for choosing sustainable living with EcoNest!\n\n");
        body.append("EcoNest Customer Care");
        return body.toString();
    }

    /** 17800 -> "Rs. 17,800.00" */
    private static String formatRupees(BigDecimal amount) {
        return String.format(Locale.ENGLISH, "Rs. %,.2f", amount == null ? BigDecimal.ZERO : amount);
    }
}
