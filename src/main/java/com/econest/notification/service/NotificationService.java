
package com.econest.notification.service;

import com.econest.notification.dao.DeliveryAgentEmailDao;
import jakarta.mail.MessagingException;

import java.sql.SQLException;

public class NotificationService {

    private final EmailService emailService;
    private final DeliveryAgentEmailDao deliveryAgentEmailDao;

    public NotificationService() {
        this.emailService = new EmailService();
        this.deliveryAgentEmailDao = new DeliveryAgentEmailDao();
    }

    // Send notification using the agent's email address.
    public void notifyDeliveryAgent(
            String agentEmail,
            String orderDetails
    ) throws MessagingException {

        String subject = "EcoNest - New Delivery Assigned";

        String body = "Hello,\n\n"
                + "A new delivery has been assigned to you.\n\n"
                + "Delivery Details:\n"
                + orderDetails + "\n\n"
                + "Please check the delivery information in the EcoNest system.\n\n"
                + "Thank you,\n"
                + "EcoNest Team";

        emailService.sendEmail(agentEmail, subject, body);
    }

    // Find the agent's email using the agent ID and send notification.
    public void notifyDeliveryAgent(
            int agentId,
            String orderDetails
    ) throws SQLException, MessagingException {

        String agentEmail =
                deliveryAgentEmailDao.findEmailByAgentId(agentId);

        if (agentEmail == null || agentEmail.isBlank()) {
            System.err.println(
                    "Agent email not found. Agent ID: " + agentId
            );
            return;
        }

        notifyDeliveryAgent(agentEmail, orderDetails);
    }
}
