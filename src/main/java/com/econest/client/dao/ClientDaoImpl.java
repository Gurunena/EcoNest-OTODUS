package com.econest.client.dao;

import com.econest.client.model.Client;
import com.econest.client.model.ClientType;
import com.econest.client.model.ContactDetails;
import com.econest.client.model.HouseholdClient;
import com.econest.client.model.OfficeClient;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL version of {@link ClientDao}.
 *
 * Every query is a PreparedStatement (the values go in as ? placeholders, which
 * stops SQL injection) inside try-with-resources (the connection closes itself).
 * HouseholdClient and OfficeClient are both stored in the one clients table;
 * client_type says which one a row is.
 */
public class ClientDaoImpl implements ClientDao {

    // -----------------------------------------------------------------
    // SQL
    // -----------------------------------------------------------------

    private static final String SQL_FIND_ALL =
            "SELECT * FROM clients ORDER BY client_id";

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM clients WHERE client_id = ?";

    private static final String SQL_SEARCH = """
            SELECT * FROM clients
            WHERE name LIKE ? OR email LIKE ? OR phone LIKE ?
               OR city LIKE ? OR contact_person LIKE ?
            ORDER BY client_id
            """;

    private static final String SQL_EMAIL_EXISTS =
            "SELECT COUNT(*) FROM clients WHERE LOWER(email) = LOWER(?) AND client_id <> ?";

    private static final String SQL_INSERT = """
            INSERT INTO clients (client_type, name, email, phone,
                                 address_line1, address_line2, city, postal_code,
                                 household_size, preferred_delivery_slot,
                                 company_registration_no, contact_person, department)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_UPDATE = """
            UPDATE clients
            SET client_type = ?, name = ?, email = ?, phone = ?,
                address_line1 = ?, address_line2 = ?, city = ?, postal_code = ?,
                household_size = ?, preferred_delivery_slot = ?,
                company_registration_no = ?, contact_person = ?, department = ?
            WHERE client_id = ?
            """;

    private static final String SQL_DELETE =
            "DELETE FROM clients WHERE client_id = ?";

    private static final String SQL_COUNT_ORDERS =
            "SELECT COUNT(*) FROM orders WHERE client_id = ?";

    // -----------------------------------------------------------------
    // Reading
    // -----------------------------------------------------------------

    @Override
    public List<Client> findAll() throws SQLException {
        List<Client> clients = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clients.add(mapRow(rs));
            }
        }
        return clients;
    }

    @Override
    public Optional<Client> findById(int clientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<Client> search(String keyword) throws SQLException {
        String pattern = "%" + keyword.trim() + "%";   // % means "anything" in LIKE
        List<Client> clients = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            for (int i = 1; i <= 5; i++) {               // the same pattern for all 5 columns
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(mapRow(rs));
                }
            }
        }
        return clients;
    }

    @Override
    public boolean emailExists(String email, int excludeClientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EMAIL_EXISTS)) {
            ps.setString(1, email.trim());
            ps.setInt(2, excludeClientId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public int countOrders(int clientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_ORDERS)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // -----------------------------------------------------------------
    // Writing
    // -----------------------------------------------------------------

    @Override
    public void add(Client client) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            setClientValues(ps, client);
            ps.executeUpdate();
            // Ask MySQL which id it gave the new row, and store it in the object
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    client.setClientId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public boolean update(Client client) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            setClientValues(ps, client);
            ps.setInt(14, client.getClientId());
            return ps.executeUpdate() > 0;              // 0 rows changed = no such client
        }
    }

    @Override
    public boolean delete(int clientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, clientId);
            return ps.executeUpdate() > 0;
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /**
     * Turns the current row into the right kind of Client.
     * client_type decides whether we build an OfficeClient or a HouseholdClient.
     */
    private Client mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("client_id");
        String name = rs.getString("name");
        ContactDetails contact = new ContactDetails(
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address_line1"),
                rs.getString("address_line2"),
                rs.getString("city"),
                rs.getString("postal_code"));

        Client client;
        ClientType type = ClientType.valueOf(rs.getString("client_type"));   // "OFFICE" -> ClientType.OFFICE
        if (type == ClientType.OFFICE) {
            client = new OfficeClient(id, name, contact,
                    rs.getString("company_registration_no"),
                    rs.getString("contact_person"),
                    rs.getString("department"));
        } else {
            Integer householdSize = rs.getInt("household_size");
            if (rs.wasNull()) {                   // getInt gives 0 for NULL, so check
                householdSize = null;
            }
            client = new HouseholdClient(id, name, contact, householdSize,
                    rs.getString("preferred_delivery_slot"));
        }

        Timestamp registered = rs.getTimestamp("registered_at");
        if (registered != null) {
            client.setRegisteredAt(registered.toLocalDateTime());
        }
        return client;
    }

    /**
     * Fills in placeholders 1-13, which INSERT and UPDATE share.
     * The columns that don't belong to this type of client are set to NULL.
     */
    private void setClientValues(PreparedStatement ps, Client client) throws SQLException {
        ContactDetails contact = client.getContactDetails();
        ps.setString(1, client.getClientType().name());   // "HOUSEHOLD" or "OFFICE"
        ps.setString(2, client.getName());
        ps.setString(3, contact.getEmail());
        ps.setString(4, contact.getPhone());
        ps.setString(5, contact.getAddressLine1());
        ps.setString(6, contact.getAddressLine2());
        ps.setString(7, contact.getCity());
        ps.setString(8, contact.getPostalCode());

        if (client instanceof HouseholdClient) {
            HouseholdClient household = (HouseholdClient) client;
            ps.setObject(9, household.getHouseholdSize(), Types.INTEGER);   // setObject allows null
            ps.setString(10, household.getPreferredDeliverySlot());
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.VARCHAR);
            ps.setNull(13, Types.VARCHAR);
        } else {
            OfficeClient office = (OfficeClient) client;
            ps.setNull(9, Types.INTEGER);
            ps.setNull(10, Types.VARCHAR);
            ps.setString(11, office.getCompanyRegistrationNo());
            ps.setString(12, office.getContactPerson());
            ps.setString(13, office.getDepartment());
        }
    }
}
