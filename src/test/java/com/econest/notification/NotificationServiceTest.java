package com.econest.notification;

import com.econest.client.FakeClientDao;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.notification.dao.DeliveryAgentEmailDao;
import com.econest.notification.dao.DispatchEmailDao;
import com.econest.notification.dao.NotificationDao;
import com.econest.notification.model.DispatchedOrder;
import com.econest.notification.model.EmailNotification;
import com.econest.notification.model.NotificationType;
import com.econest.notification.service.EmailService;
import com.econest.notification.service.NotificationService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the Task 9 rules with a fake database and a fake email server,
 * so no MySQL, internet or Gmail password is needed.
 */
class NotificationServiceTest {

    // ----------------------------------------------------------------- fakes

    /** Pretend email server: remembers what it was asked to send, or fails on purpose. */
    static class FakeEmailService extends EmailService {
        final List<String> sentTo = new ArrayList<>();
        final List<String> subjects = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        boolean fail = false;

        @Override
        public void sendEmail(String recipient, String subject, String body) throws MessagingException {
            if (fail) {
                throw new MessagingException("Could not connect to SMTP host: smtp.gmail.com");
            }
            sentTo.add(recipient);
            subjects.add(subject);
            bodies.add(body);
        }
    }

    /** Pretend delivery_agents table: agent id -> email. */
    static class FakeAgentEmailDao extends DeliveryAgentEmailDao {
        final Map<Integer, String> emails = new HashMap<>();

        @Override
        public String findEmailByAgentId(int agentId) {
            return emails.get(agentId);
        }
    }

    /** Pretend orders table. Only findOrder is needed for Task 9. */
    static class FakeOrderDao implements DispatchEmailDao {
        final Map<Integer, DispatchedOrder> orders = new HashMap<>();

        @Override
        public Optional<DispatchedOrder> findOrder(int orderId) {
            return Optional.ofNullable(orders.get(orderId));
        }

        @Override
        public boolean dispatchEmailAlreadySent(int orderId) {
            return false;
        }

        @Override
        public List<Integer> findOrdersAwaitingDispatchEmail() {
            return List.of();
        }

        @Override
        public void logDispatchEmail(int clientId, int orderId, String recipient, String subject,
                                     String body, boolean sent, String errorMessage) {
            // not used by Task 9
        }
    }

    /** Pretend notifications table. */
    static class FakeNotificationDao implements NotificationDao {
        final List<EmailNotification> saved = new ArrayList<>();

        @Override
        public void save(EmailNotification notification) {
            notification.setNotificationId(saved.size() + 1);
            saved.add(notification);
        }

        @Override
        public List<EmailNotification> findAll() {
            return saved;
        }

        @Override
        public List<EmailNotification> search(String text) {
            return saved;
        }

        @Override
        public int countSent() {
            return (int) saved.stream().filter(EmailNotification::isSent).count();
        }
    }

    // ----------------------------------------------------------------- setup

    private FakeEmailService email;
    private FakeAgentEmailDao agents;
    private FakeOrderDao orders;
    private FakeNotificationDao notifications;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        email = new FakeEmailService();
        agents = new FakeAgentEmailDao();
        orders = new FakeOrderDao();
        notifications = new FakeNotificationDao();
        FakeClientDao clients = new FakeClientDao();

        // client 1
        clients.add(new HouseholdClient(0, "Amara Silva",
                new ContactDetails("amara@gmail.com", "0712345678", "42 Lake Road", null, "Colombo", null),
                4, "Any time"));

        // agent 1 has an email, agent 2 doesn't
        agents.emails.put(1, "kasun.r@econest.example.com");

        // order 4 for client 1, assigned to Kasun
        DispatchedOrder order = new DispatchedOrder(4, 1, "DISPATCHED", LocalDateTime.of(2026, 11, 15, 10, 0),
                new BigDecimal("4500.00"), "42 Lake Road, Colombo", "Kasun Rathnayake", "0714567890");
        order.addItem("Peace Lily", 2);
        order.addItem("Ceramic Planter", 1);
        orders.orders.put(4, order);

        service = new NotificationService(agents, orders, clients, notifications, email);
    }

    // ----------------------------------------------------------------- tests

    @Test
    void assignedAgentIsEmailed() {
        String result = service.notifyDeliveryAgent(4, 1);

        assertEquals("Delivery email sent to kasun.r@econest.example.com.", result);
        assertEquals(List.of("kasun.r@econest.example.com"), email.sentTo);
        assertEquals("EcoNest - New Delivery Assigned (order #4)", email.subjects.get(0));
    }

    @Test
    void emailTellsTheAgentWhatToDeliverAndWhere() {
        service.notifyDeliveryAgent(4, 1);
        String body = email.bodies.get(0);

        assertTrue(body.startsWith("Hello Kasun Rathnayake,"));
        assertTrue(body.contains("Order        : #4"));
        assertTrue(body.contains("Amara Silva (0712345678)"));
        assertTrue(body.contains("42 Lake Road, Colombo"));
        assertTrue(body.contains("Peace Lily x 2"));
        assertTrue(body.contains("Ceramic Planter x 1"));
        assertTrue(body.contains("Rs. 4,500.00"));
    }

    @Test
    void sentEmailIsSavedAsDeliveryAssigned() {
        service.notifyDeliveryAgent(4, 1);

        assertEquals(1, notifications.saved.size());
        EmailNotification saved = notifications.saved.get(0);
        assertEquals(NotificationType.DELIVERY_ASSIGNED, saved.getType());
        assertEquals(EmailNotification.SENT, saved.getStatus());
        assertEquals("kasun.r@econest.example.com", saved.getRecipientEmail());
        assertEquals(4, saved.getOrderId());
        assertNull(saved.getErrorMessage());
    }

    @Test
    void failedEmailIsSavedAsFailedAndNeverThrows() {
        email.fail = true;

        String result = service.notifyDeliveryAgent(4, 1);   // must not throw

        assertTrue(result.contains("NOT sent"));
        EmailNotification saved = notifications.saved.get(0);
        assertEquals(EmailNotification.FAILED, saved.getStatus());
        assertTrue(saved.getErrorMessage().contains("smtp.gmail.com"));
        assertNull(saved.getSentAt());
    }

    @Test
    void agentWithoutAnEmailIsSkipped() {
        String result = service.notifyDeliveryAgent(4, 2);

        assertTrue(result.contains("no email address"));
        assertTrue(email.sentTo.isEmpty());
        assertTrue(notifications.saved.isEmpty());
    }

    @Test
    void unknownOrderIsReported() {
        String result = service.notifyDeliveryAgent(999, 1);

        assertTrue(result.contains("not found"));
        assertTrue(email.sentTo.isEmpty());
    }

    @Test
    void typeNamesMatchTheDatabase() {
        // valueOf() must accept exactly what the notifications table stores
        assertEquals(NotificationType.DELIVERY_ASSIGNED, NotificationType.valueOf("DELIVERY_ASSIGNED"));
        assertEquals(NotificationType.ORDER_STATUS_CHANGE, NotificationType.valueOf("ORDER_STATUS_CHANGE"));
        assertEquals("Delivery assigned", NotificationType.DELIVERY_ASSIGNED.getLabel());
    }
}
