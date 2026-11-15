package com.econest.notification.service;

import com.econest.client.dao.ClientDao;
import com.econest.client.dao.ClientDaoImpl;
import com.econest.client.model.Client;
import com.econest.notification.dao.DeliveryAgentEmailDao;
import com.econest.notification.dao.DispatchEmailDao;
import com.econest.notification.dao.DispatchEmailDaoImpl;
import com.econest.notification.dao.NotificationDao;
import com.econest.notification.dao.NotificationDaoImpl;
import com.econest.notification.model.DispatchedOrder;
import com.econest.notification.model.EmailNotification;
import com.econest.notification.model.NotificationType;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Task 9 - emails a delivery agent when a delivery has been assigned to them.
 *
 * DeliveryAssignmentService calls {@link #notifyDeliveryAgent(int, int)} straight
 * after an order is assigned (or reassigned) to an agent.
 *
 * Rules:
 *  - the email goes to the agent's address in the delivery_agents table
 *  - it tells the agent what to deliver, to whom and where
 *  - every attempt, sent or failed, is saved in the notifications table
 *    as DELIVERY_ASSIGNED
 *  - a problem with email never stops the assignment: this class never throws,
 *    it returns a message saying what happened instead
 */
public class NotificationService {

    private final DeliveryAgentEmailDao agentEmailDao;
    private final DispatchEmailDao orderDao;          // reads the order, its items and its agent
    private final ClientDao clientDao;                // reads who the order is for
    private final NotificationDao notificationDao;    // saves the email in the notifications table
    private final EmailService emailService;

    /** Normal constructor: real database and real email. */
    public NotificationService() {
        this(new DeliveryAgentEmailDao(), new DispatchEmailDaoImpl(), new ClientDaoImpl(),
                new NotificationDaoImpl(), new EmailService());
    }

    /** Lets tests pass in fakes instead of MySQL and Gmail. */
    public NotificationService(DeliveryAgentEmailDao agentEmailDao, DispatchEmailDao orderDao,
                               ClientDao clientDao, NotificationDao notificationDao,
                               EmailService emailService) {
        this.agentEmailDao = agentEmailDao;
        this.orderDao = orderDao;
        this.clientDao = clientDao;
        this.notificationDao = notificationDao;
        this.emailService = emailService;
    }

    /**
     * Emails the agent the details of the delivery they've just been given.
     *
     * @return a one-line message saying what happened,
     *         e.g. "Delivery email sent to kasun.r@econest.example.com."
     */
    public String notifyDeliveryAgent(int orderId, int agentId) {
        try {
            // Find the agent's email using the agent ID
            String agentEmail = agentEmailDao.findEmailByAgentId(agentId);
            if (agentEmail == null || agentEmail.isBlank()) {
                return "No delivery email: agent #" + agentId + " has no email address.";
            }

            Optional<DispatchedOrder> order = orderDao.findOrder(orderId);
            if (order.isEmpty()) {
                return "No delivery email: order #" + orderId + " was not found.";
            }
            Client client = clientDao.findById(order.get().getClientId()).orElse(null);

            String subject = buildSubject(orderId);
            String body = buildBody(order.get(), client);
            return sendAndSave(agentEmail, subject, body, orderId);

        } catch (SQLException e) {
            System.err.println("NotificationService: database problem for order #" + orderId + " - " + e);
            return "Could not send the delivery email for order #" + orderId + " (database problem).";
        }
    }

    // -----------------------------------------------------------------
    // Building, sending and saving the email
    // -----------------------------------------------------------------

    private String sendAndSave(String agentEmail, String subject, String body, int orderId)
            throws SQLException {
        try {
            emailService.sendEmail(agentEmail, subject, body);
        } catch (Exception e) {
            // e.g. no internet, wrong Gmail password, or the email settings are missing
            String reason = e.getMessage() == null ? e.toString() : e.getMessage();
            notificationDao.save(EmailNotification.failed(NotificationType.DELIVERY_ASSIGNED,
                    agentEmail, subject, body, orderId, reason));
            return "Delivery email to " + agentEmail + " was NOT sent: " + reason;
        }
        notificationDao.save(EmailNotification.sent(NotificationType.DELIVERY_ASSIGNED,
                agentEmail, subject, body, orderId));
        return "Delivery email sent to " + agentEmail + ".";
    }

    public static String buildSubject(int orderId) {
        return "EcoNest - New Delivery Assigned (order #" + orderId + ")";
    }

    /** Writes the email: who to deliver to, where, and what. */
    public static String buildBody(DispatchedOrder order, Client client) {
        String address = order.getDeliveryAddress();
        if (address == null && client != null) {
            address = client.getAddress();
        }

        StringBuilder body = new StringBuilder();
        body.append(order.getAgentName() == null ? "Hello," : "Hello " + order.getAgentName() + ",");
        body.append("\n\n");
        body.append("A new delivery has been assigned to you.\n\n");
        body.append("Delivery Details:\n");
        body.append("Order        : #").append(order.getOrderId()).append("\n");
        if (client != null) {
            body.append("Client       : ").append(client.getName())
                    .append(" (").append(client.getPhone()).append(")\n");
        }
        body.append("Deliver to   : ").append(address == null ? "-" : address).append("\n");

        List<String> items = order.getItems();
        for (int i = 0; i < items.size(); i++) {
            body.append(i == 0 ? "Items        : " : "               ").append(items.get(i)).append("\n");
        }
        body.append("Order total  : ").append(formatRupees(order.getTotalAmount())).append("\n\n");

        body.append("Please check the delivery information in the EcoNest system.\n\n");
        body.append("Thank you,\n");
        body.append("EcoNest Team");
        return body.toString();
    }

    /** 17800 -> "Rs. 17,800.00" */
    private static String formatRupees(BigDecimal amount) {
        return String.format(Locale.ENGLISH, "Rs. %,.2f", amount == null ? BigDecimal.ZERO : amount);
    }
}
