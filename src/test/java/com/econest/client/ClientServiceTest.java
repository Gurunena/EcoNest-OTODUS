package com.econest.client;

import com.econest.client.model.Client;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.client.model.OfficeClient;
import com.econest.client.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks every rule in ClientService.
 * Uses FakeClientDao, so no MySQL is needed: the tests run anywhere, in a second.
 */
class ClientServiceTest {

    private FakeClientDao dao;
    private ClientService service;

    @BeforeEach
    void setUp() {
        dao = new FakeClientDao();
        service = new ClientService(dao);   // the second constructor: give the service our fake DAO
    }

    // ----------------------------------------------------------------- helpers

    private HouseholdClient household(String name, String email, String phone) {
        return new HouseholdClient(0, name,
                new ContactDetails(email, phone, "42 Lake Road", null, "Colombo", "00500"),
                null, "Any time");
    }

    private OfficeClient office(String contactPerson) {
        return new OfficeClient(0, "BlueSky Ltd",
                new ContactDetails("office@bluesky.lk", "0112567890", "120 Union Place", null, "Colombo", null),
                null, contactPerson, null);
    }

    /** Runs the action and returns the message of the IllegalArgumentException it must throw. */
    private String rejected(org.junit.jupiter.api.function.Executable action) {
        return assertThrows(IllegalArgumentException.class, action).getMessage();
    }

    // ----------------------------------------------------------------- adding

    @Test
    void addingAValidClientGivesItAnId() throws SQLException {
        HouseholdClient client = household("Amara Silva", "amara@gmail.com", "0712345678");

        service.addClient(client);

        assertEquals(1, client.getClientId());
        assertEquals(1, service.findAll().size());
    }

    @Test
    void addingTidiesUpWhatWasTyped() throws SQLException {
        HouseholdClient client = new HouseholdClient(0, "  Amara Silva ",
                new ContactDetails(" amara@gmail.com ", "071 234-5678", "42 Lake Road", "   ", "Colombo", ""),
                null, "Any time");

        service.addClient(client);

        assertEquals("Amara Silva", client.getName());
        assertEquals("amara@gmail.com", client.getEmail());
        assertEquals("0712345678", client.getPhone());                 // spaces and dashes removed
        assertNull(client.getContactDetails().getAddressLine2());       // empty -> null
        assertNull(client.getContactDetails().getPostalCode());
    }

    @Test
    void nameIsRequired() {
        String message = rejected(() -> service.addClient(household(" ", "a@gmail.com", "0712345678")));
        assertTrue(message.contains("name"));
    }

    @Test
    void emailMustLookLikeAnEmail() {
        String message = rejected(() -> service.addClient(household("Amara", "amara.gmail.com", "0712345678")));
        assertTrue(message.contains("valid email"));
    }

    @Test
    void phoneMustBeTenDigitsStartingWithZero() {
        assertTrue(rejected(() -> service.addClient(household("Amara", "a@gmail.com", "12345"))).contains("Phone"));
        assertTrue(rejected(() -> service.addClient(household("Amara", "a@gmail.com", "7123456789"))).contains("Phone"));
    }

    @Test
    void postalCodeMustBeFiveDigitsIfGiven() {
        HouseholdClient client = household("Amara", "a@gmail.com", "0712345678");
        client.getContactDetails().setPostalCode("123");

        assertTrue(rejected(() -> service.addClient(client)).contains("Postal code"));
    }

    @Test
    void householdSizeMustBeSensible() {
        HouseholdClient client = household("Amara", "a@gmail.com", "0712345678");
        client.setHouseholdSize(0);

        assertTrue(rejected(() -> service.addClient(client)).contains("Household size"));
    }

    @Test
    void deliverySlotMustBeOneOfTheChoices() {
        HouseholdClient client = household("Amara", "a@gmail.com", "0712345678");
        client.setPreferredDeliverySlot("Midnight");

        assertTrue(rejected(() -> service.addClient(client)).contains("delivery slot"));
    }

    @Test
    void officeClientNeedsAContactPerson() {
        assertTrue(rejected(() -> service.addClient(office("  "))).contains("contact person"));
        assertDoesNotThrow(() -> service.addClient(office("Rashmi Gunawardena")));
    }

    @Test
    void twoClientsCannotShareAnEmail() throws SQLException {
        service.addClient(household("Amara Silva", "amara@gmail.com", "0712345678"));

        // Same email in capitals is still the same email
        String message = rejected(() -> service.addClient(household("Someone Else", "AMARA@gmail.com", "0779999999")));
        assertTrue(message.contains("already uses"));
    }

    // ----------------------------------------------------------------- updating

    @Test
    void updatingAClientCanKeepItsOwnEmail() throws SQLException {
        HouseholdClient client = household("Amara Silva", "amara@gmail.com", "0712345678");
        service.addClient(client);

        client.setName("Amara Perera");
        service.updateClient(client);

        assertEquals("Amara Perera", service.findById(client.getClientId()).orElseThrow().getName());
    }

    @Test
    void updateNeedsASelectedClient() {
        assertTrue(rejected(() -> service.updateClient(household("Amara", "a@gmail.com", "0712345678")))
                .contains("Select a client"));
    }

    @Test
    void updatingAClientThatWasDeletedIsReported() {
        HouseholdClient ghost = household("Amara", "a@gmail.com", "0712345678");
        ghost.setClientId(99);

        assertTrue(rejected(() -> service.updateClient(ghost)).contains("no longer exists"));
    }

    // ----------------------------------------------------------------- deleting

    @Test
    void clientWithOrdersCannotBeDeleted() throws SQLException {
        Client client = household("Amara Silva", "amara@gmail.com", "0712345678");
        service.addClient(client);
        dao.setOrderCount(client.getClientId(), 2);

        assertTrue(rejected(() -> service.deleteClient(client.getClientId())).contains("2 order(s)"));
        assertEquals(1, service.findAll().size());   // still there
    }

    @Test
    void clientWithoutOrdersCanBeDeleted() throws SQLException {
        Client client = household("Amara Silva", "amara@gmail.com", "0712345678");
        service.addClient(client);

        service.deleteClient(client.getClientId());

        assertTrue(service.findAll().isEmpty());
    }

    // ----------------------------------------------------------------- counting

    @Test
    void countClientsCountsEveryone() throws SQLException {
        assertEquals(0, service.countClients());
        service.addClient(household("Amara Silva", "amara@gmail.com", "0712345678"));
        service.addClient(office("Rashmi Gunawardena"));

        assertEquals(2, service.countClients());
    }

    // ----------------------------------------------------------------- searching

    @Test
    void blankSearchShowsEveryone() throws SQLException {
        service.addClient(household("Amara Silva", "amara@gmail.com", "0712345678"));
        service.addClient(household("Dinuka Jayawardena", "dinuka@gmail.com", "0769876543"));

        assertEquals(2, service.search("   ").size());
        assertEquals(1, service.search("dinuka").size());
    }
}
