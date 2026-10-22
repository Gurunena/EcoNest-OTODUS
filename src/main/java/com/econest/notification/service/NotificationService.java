
package com.econest.notification.service;

import jakarta.mail.MessagingException;

public class NotificationService {

    private final EmailService emailService;

    public NotificationService() {
        this.emailService = new EmailService();
    }

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
}
