package com.econest.notification;

import com.econest.client.FakeClientDao;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.client.model.OfficeClient;
import com.econest.notification.dao.DispatchEmailDao;
import com.econest.notification.model.DispatchedOrder;
import com.econest.notification.service.ClientNotificationService;
import com.econest.notification.service.EmailService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the Task 8 rules with a fake database and a fake email server,
 * so no MySQL, internet or Gmail password is needed.
 */
class ClientNotificationServiceTest {

    // ----------------------------------------------------------------- fakes

    /** Pretend email server: remembers what it was asked to send, or fails on purpose. */
    static class FakeEmailService extends EmailService {
        final List<String> sentTo = new ArrayList<>();
        final List<String> bodies = new ArrayList<>();
        boolean fail = false;

        @Override
        public void sendEmail(String recipient, String subject, String body) throws MessagingException {
            if (fail) {
                throw new MessagingException("Could not connect to SMTP host: smtp.gmail.com");
            }
            sentTo.add(recipient);
            bodies.add(body);
        }
    }

    /** Pretend database for orders and the notifications log. */
    static class FakeDispatchEmailDao implements DispatchEmailDao {
        final Map<Integer, DispatchedOrder> orders = new HashMap<>();
        final List<String> log = new ArrayList<>();   // e.g. "order 2 SENT"

        @Override
        public Optional<DispatchedOrder> findOrder(int orderId) {
            return Optional.ofNullable(orders.get(orderId));
        }

        @Override
        public boolean dispatchEmailAlreadySent(int orderId) {
            return log.contains("order " + orderId + " SENT");
        }

        @Override
        public List<Integer> findOrdersAwaitingDispatchEmail() {
            List<Integer> ids = new ArrayList<>();
            for (DispatchedOrder o : orders.values()) {
                if ("DISPATCHED".equals(o.getStatus()) && !dispatchEmailAlreadySent(o.getOrderId())) {
                    ids.add(o.getOrderId());
                }
            }
            return ids;
        }

        @Override
        public void logDispatchEmail(int clientId, int orderId, String recipient, String subject, String body,
                                     boolean sent, String errorMessage) {
            log.add("order " + orderId + (sent ? " SENT" : " FAILED: " + errorMessage));
        }
    }

    // ----------------------------------------------------------------- setup

    private FakeDispatchEmailDao dispatchDao;
    private FakeEmailService email;
    private ClientNotificationService service;

    @BeforeEach
    void setUp() {
        dispatchDao = new FakeDispatchEmailDao();
        email = new FakeEmailService();
        FakeClientDao clients = new FakeClientDao();

        // client 1: household, client 2: office
        clients.add(new HouseholdClient(0, "Amara Silva",
                new ContactDetails("amara@gmail.com", "0712345678", "42 Lake Road", null, "Colombo", null),
                4, "Weekday evening"));
        clients.add(new OfficeClient(0, "BlueSky Software (Pvt) Ltd",
                new ContactDetails("facilities@bluesky.lk", "0112567890", "120 Union Place", "Level 7", "Colombo", "00200"),
                null, "Rashmi Gunawardena", "Facilities"));

        DispatchedOrder office = order(2, 2, "DISPATCHED");
        office.addItem("Compost Bin Kit", 2);
        office.addItem("Balcony Starter Kit", 1);
        dispatchDao.orders.put(2, office);

        DispatchedOrder household = order(5, 1, "DISPATCHED");
        household.addItem("Peace Lily", 1);
        dispatchDao.orders.put(5, household);

        dispatchDao.orders.put(3, order(3, 1, "CONFIRMED"));

        service = new ClientNotificationService(dispatchDao, clients, email);
    }

    private DispatchedOrder order(int orderId, int clientId, String status) {
        return new DispatchedOrder(orderId, clientId, status, LocalDateTime.of(2026, 9, 28, 14, 40),
                new BigDecimal("17800.00"), "120 Union Place, Level 7, Colombo 00200",
                "Pradeep Kumara", "0778901234");
    }

    // ----------------------------------------------------------------- tests

    @Test
    void dispatchedOrderEmailsTheClientAndIsLogged() {
        String result = service.notifyOrderDispatched(2);

        assertEquals("Dispatch email sent to facilities@bluesky.lk.", result);
        assertEquals(List.of("facilities@bluesky.lk"), email.sentTo);
        assertEquals(List.of("order 2 SENT"), dispatchDao.log);
    }

    @Test
    void emailListsTheOrderDetails() {
        service.notifyOrderDispatched(2);
        String body = email.bodies.get(0);

        assertTrue(body.contains("order #2 has been dispatched"));
        assertTrue(body.contains("Compost Bin Kit x 2"));
        assertTrue(body.contains("Balcony Starter Kit x 1"));
        assertTrue(body.contains("Rs. 17,800.00"));
        assertTrue(body.contains("Pradeep Kumara (0778901234)"));
        assertTrue(body.contains("28 Sep 2026"));
    }

    @Test
    void officeAndHouseholdClientsGetDifferentWording() {
        service.notifyOrderDispatched(2);   // office
        service.notifyOrderDispatched(5);   // household

        String officeEmail = email.bodies.get(0);
        String householdEmail = email.bodies.get(1);

        // Polymorphism: same method calls, different text depending on the client type
        assertTrue(officeEmail.startsWith("Dear Rashmi Gunawardena,"));
        assertTrue(officeEmail.contains("office hours"));
        assertTrue(householdEmail.startsWith("Dear Amara Silva,"));
        assertTrue(householdEmail.contains("Weekday evening"));
    }

    @Test
    void orderThatIsNotDispatchedIsNotEmailed() {
        String result = service.notifyOrderDispatched(3);

        assertTrue(result.contains("CONFIRMED"));
        assertTrue(email.sentTo.isEmpty());
        assertTrue(dispatchDao.log.isEmpty());
    }

    @Test
    void clientIsOnlyEmailedOncePerOrder() {
        service.notifyOrderDispatched(2);
        String second = service.notifyOrderDispatched(2);

        assertTrue(second.contains("already emailed"));
        assertEquals(1, email.sentTo.size());
    }

    @Test
    void failedEmailIsLoggedAndNeverThrows() {
        email.fail = true;

        String result = service.notifyOrderDispatched(2);   // must not throw

        assertTrue(result.contains("NOT sent"));
        assertTrue(dispatchDao.log.get(0).startsWith("order 2 FAILED"));
        assertFalse(dispatchDao.dispatchEmailAlreadySent(2));   // so it can be tried again later
    }

    @Test
    void unknownOrderIsReported() {
        assertTrue(service.notifyOrderDispatched(999).contains("not found"));
        assertTrue(email.sentTo.isEmpty());
    }

    @Test
    void missedEmailsCanBeSentLater() {
        email.fail = true;
        service.notifyOrderDispatched(2);
        service.notifyOrderDispatched(5);
        assertEquals(2, dispatchDao.findOrdersAwaitingDispatchEmail().size());

        email.fail = false;   // email is working again
        String summary = service.sendMissedDispatchEmails();

        assertEquals("2 dispatch email(s) sent, 0 not sent.", summary);
        assertTrue(dispatchDao.findOrdersAwaitingDispatchEmail().isEmpty());
    }
}
