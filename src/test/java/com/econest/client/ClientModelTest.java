package com.econest.client;

import com.econest.client.model.Client;
import com.econest.client.model.ClientType;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.client.model.OfficeClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the Client classes on their own (inheritance, polymorphism, composition). */
class ClientModelTest {

    @Test
    void eachSubclassReportsItsOwnType() {
        ContactDetails contact = new ContactDetails("a@b.com", "0712345678", "1 Main St", null, "Colombo", null);

        // Both are stored in a Client variable, but each answers with its own type (polymorphism)
        Client household = new HouseholdClient(1, "Amara Silva", contact, 4, "Any time");
        Client office = new OfficeClient(2, "BlueSky Ltd", contact, null, "Rashmi", null);

        assertEquals(ClientType.HOUSEHOLD, household.getClientType());
        assertEquals(ClientType.OFFICE, office.getClientType());
    }

    @Test
    void fullAddressSkipsTheEmptyParts() {
        ContactDetails full = new ContactDetails("a@b.com", "0712345678", "42 Lake Road", "Apt 3B", "Colombo", "00500");
        ContactDetails partial = new ContactDetails("a@b.com", "0712345678", "17 Temple Lane", null, "Kandy", " ");

        assertEquals("42 Lake Road, Apt 3B, Colombo, 00500", full.getFullAddress());
        assertEquals("17 Temple Lane, Kandy", partial.getFullAddress());
    }

    @Test
    void clientShortcutsReadFromContactDetails() {
        ContactDetails contact = new ContactDetails("amara@gmail.com", "0712345678", "42 Lake Road", null, "Colombo", null);
        Client client = new HouseholdClient(7, "Amara Silva", contact, null, "Any time");

        // client.getEmail() is a shortcut for client.getContactDetails().getEmail() (composition)
        assertEquals("amara@gmail.com", client.getEmail());
        assertEquals("0712345678", client.getPhone());
        assertEquals("42 Lake Road, Colombo", client.getAddress());
        assertEquals("#7 Amara Silva (Household)", client.toString());
    }

    @Test
    void greetingAndDeliveryNoteDependOnTheClientType() {
        ContactDetails contact = new ContactDetails("a@b.com", "0712345678", "1 Main St", null, "Colombo", null);
        Client household = new HouseholdClient(1, "Amara Silva", contact, 4, "Weekday evening");
        Client office = new OfficeClient(2, "BlueSky Ltd", contact, null, "Rashmi Gunawardena", null);

        // Same method, different answer for each subclass (polymorphism)
        assertEquals("Dear Amara Silva,", household.getEmailGreeting());
        assertEquals("Dear Rashmi Gunawardena,", office.getEmailGreeting());
        assertTrue(household.getDeliveryNote().contains("Weekday evening"));
        assertTrue(office.getDeliveryNote().contains("BlueSky Ltd"));
    }

    @Test
    void householdSizeCanBeLeftEmpty() {
        HouseholdClient client = new HouseholdClient(0, "Amara Silva",
                new ContactDetails("a@b.com", "0712345678", "1 Main St", null, "Colombo", null),
                null, "Any time");

        assertNull(client.getHouseholdSize());
    }
}
