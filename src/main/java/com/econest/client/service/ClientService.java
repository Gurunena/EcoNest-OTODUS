package com.econest.client.service;

import com.econest.client.dao.ClientDao;
import com.econest.client.dao.ClientDaoImpl;
import com.econest.client.model.Client;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.client.model.OfficeClient;
import com.econest.util.Validator;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business rules for the Clients module (Task 2).
 *
 * The screen calls this class, and this class calls the DAO. Rules live here,
 * not in the screen, so they can't be skipped:
 *  - every client needs a name, a valid email, a Sri Lankan phone number and an address
 *  - no two clients can share an email
 *  - office clients need a contact person
 *  - a client who has orders cannot be deleted (it would break the order history)
 *
 * Each rule throws IllegalArgumentException with a message that can be shown
 * to the user as it is. SQLException means the database itself had a problem.
 */
public class ClientService {

    // something@something.something
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // 10 digits starting with 0, e.g. 0712345678 (spaces and dashes are removed first)
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");

    // Sri Lankan postal codes are 5 digits, e.g. 00500
    private static final Pattern POSTAL_CODE = Pattern.compile("^\\d{5}$");

    private final ClientDao dao;

    /** Normal constructor: uses the real MySQL database. */
    public ClientService() {
        this(new ClientDaoImpl());
    }

    /** Lets a unit test pass in a fake DAO instead of the real database. */
    public ClientService(ClientDao dao) {
        this.dao = dao;
    }

    // -----------------------------------------------------------------
    // Reading
    // -----------------------------------------------------------------

    public List<Client> findAll() throws SQLException {
        return dao.findAll();
    }

    public Optional<Client> findById(int clientId) throws SQLException {
        return dao.findById(clientId);
    }

    /** How many clients there are (shown on the Dashboard). */
    public int countClients() throws SQLException {
        return dao.countAll();
    }

    /** Search by name, email, phone, city or contact person. Blank shows everyone. */
    public List<Client> search(String keyword) throws SQLException {
        if (Validator.isBlank(keyword)) {
            return dao.findAll();
        }
        return dao.search(keyword);
    }

    // -----------------------------------------------------------------
    // Writing
    // -----------------------------------------------------------------

    /** Checks the rules, then saves a new client. The client gets its new id. */
    public void addClient(Client client) throws SQLException {
        tidyUp(client);
        validate(client);
        if (dao.emailExists(client.getEmail(), 0)) {
            throw new IllegalArgumentException("Another client already uses " + client.getEmail() + ".");
        }
        dao.add(client);
    }

    /** Checks the rules, then saves changes to an existing client. */
    public void updateClient(Client client) throws SQLException {
        if (client.getClientId() <= 0) {
            throw new IllegalArgumentException("Select a client in the table first.");
        }
        tidyUp(client);
        validate(client);
        if (dao.emailExists(client.getEmail(), client.getClientId())) {
            throw new IllegalArgumentException("Another client already uses " + client.getEmail() + ".");
        }
        if (!dao.update(client)) {
            throw new IllegalArgumentException("That client no longer exists. Refresh the list.");
        }
    }

    /** Deletes a client, unless they have orders. */
    public void deleteClient(int clientId) throws SQLException {
        int orders = dao.countOrders(clientId);
        if (orders > 0) {
            throw new IllegalArgumentException("This client has " + orders
                    + " order(s), so they can't be deleted. Their order history must be kept.");
        }
        if (!dao.delete(clientId)) {
            throw new IllegalArgumentException("That client no longer exists. Refresh the list.");
        }
    }

    // -----------------------------------------------------------------
    // Rules
    // -----------------------------------------------------------------

    /**
     * Throws IllegalArgumentException for the first rule the client breaks.
     * Public and static so the screen can use the same rules if it wants to.
     */
    public static void validate(Client client) {
        ContactDetails contact = client.getContactDetails();

        if (Validator.isBlank(client.getName()) || client.getName().length() < 2) {
            throw new IllegalArgumentException(client instanceof OfficeClient
                    ? "Enter the company name."
                    : "Enter the client's name (at least 2 letters).");
        }
        if (client.getName().length() > 120) {
            throw new IllegalArgumentException("The name is too long (120 characters at most).");
        }
        if (contact.getEmail() == null || !EMAIL.matcher(contact.getEmail()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address, e.g. name@gmail.com.");
        }
        if (contact.getPhone() == null || !PHONE.matcher(contact.getPhone()).matches()) {
            throw new IllegalArgumentException("Phone must be 10 digits starting with 0, e.g. 0712345678.");
        }
        if (Validator.isBlank(contact.getAddressLine1())) {
            throw new IllegalArgumentException("Enter the first line of the address.");
        }
        if (Validator.isBlank(contact.getCity())) {
            throw new IllegalArgumentException("Enter the city.");
        }
        if (contact.getPostalCode() != null && !POSTAL_CODE.matcher(contact.getPostalCode()).matches()) {
            throw new IllegalArgumentException("Postal code must be 5 digits, e.g. 00500, or left empty.");
        }

        // Rules that depend on the type of client
        if (client instanceof HouseholdClient) {
            HouseholdClient household = (HouseholdClient) client;
            Integer size = household.getHouseholdSize();
            if (size != null && (size < 1 || size > 20)) {
                throw new IllegalArgumentException("Household size must be between 1 and 20, or left empty.");
            }
            if (!Arrays.asList(HouseholdClient.DELIVERY_SLOTS).contains(household.getPreferredDeliverySlot())) {
                throw new IllegalArgumentException("Choose a preferred delivery slot.");
            }
        } else if (client instanceof OfficeClient) {
            OfficeClient office = (OfficeClient) client;
            if (Validator.isBlank(office.getContactPerson())) {
                throw new IllegalArgumentException("Enter the contact person for this office.");
            }
        }
    }

    /**
     * Cleans up what was typed before it is checked and saved:
     * trims spaces, removes spaces/dashes from the phone number,
     * and turns empty optional fields into null (so the database stores NULL, not "").
     */
    private void tidyUp(Client client) {
        ContactDetails contact = client.getContactDetails();
        client.setName(trim(client.getName()));
        contact.setEmail(trim(contact.getEmail()));
        contact.setPhone(contact.getPhone() == null ? null : contact.getPhone().replaceAll("[\\s-]", ""));
        contact.setAddressLine1(trim(contact.getAddressLine1()));
        contact.setAddressLine2(emptyToNull(contact.getAddressLine2()));
        contact.setCity(trim(contact.getCity()));
        contact.setPostalCode(emptyToNull(contact.getPostalCode()));

        if (client instanceof OfficeClient) {
            OfficeClient office = (OfficeClient) client;
            office.setContactPerson(trim(office.getContactPerson()));
            office.setCompanyRegistrationNo(emptyToNull(office.getCompanyRegistrationNo()));
            office.setDepartment(emptyToNull(office.getDepartment()));
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String emptyToNull(String value) {
        return Validator.isBlank(value) ? null : value.trim();
    }
}
