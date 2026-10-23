
package com.econest.notification.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    public void sendEmail(
            String recipient,
            String subject,
            String body
    ) throws MessagingException {

        String sender = System.getenv("ECONEST_EMAIL");
        String appPassword = System.getenv("ECONEST_EMAIL_PASSWORD");

        if (sender == null || sender.isBlank()
                || appPassword == null || appPassword.isBlank()) {
            throw new IllegalStateException(
                    "Email configuration is missing."
            );
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(
                properties,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication
                    getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                sender, appPassword
                        );
                    }
                }
        );

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(recipient)
        );
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }
}
