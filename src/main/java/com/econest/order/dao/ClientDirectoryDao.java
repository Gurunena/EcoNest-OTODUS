package com.econest.order.dao;

import com.econest.order.model.ClientRef;
import com.econest.order.service.ClientLookup;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A read-only client list for the order screen's client picker.
 *
 * <p><b>This is a stand-in, not a ClientDao.</b>
 * {@code com.econest.client.service.ClientService} is an empty class and the
 * client module has no DAO yet, so there is no public method to call. This
 * class reads the two things an order needs - the name to show and the
 * address to snapshot - and writes nothing, ever. When the client module
 * ships its own DAO, implement {@link ClientLookup} over it and delete this
 * file; see the note on {@link ClientLookup}.</p>
 *
 * <p>The {@code clients} table splits the address into
 * {@code address_line1 / address_line2 / city / postal_code} while
 * {@code ContactDetails} holds one string, so the lines are joined on read -
 * which is one of the two options {@code database/README.md} suggested.</p>
 */
public class ClientDirectoryDao implements ClientLookup {

    /**
     * CONCAT_WS skips NULLs, so a client with no address_line2 or postal_code
     * does not come back with stray commas in the middle of the address.
     */
    private static final String ADDRESS =
            "CONCAT_WS(', ', address_line1, address_line2, city, postal_code)";

    private static final String SELECT =
            "SELECT client_id, name, " + ADDRESS + " AS address FROM clients";

    @Override
    public List<ClientRef> search(String query) throws SQLException {
        boolean all = query == null || query.isBlank();
        String sql = all
                ? SELECT + " ORDER BY name"
                : SELECT + " WHERE name LIKE ? ORDER BY name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!all) {
                ps.setString(1, "%" + query.trim() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ClientRef> clients = new ArrayList<>();
                while (rs.next()) {
                    clients.add(read(rs));
                }
                return clients;
            }
        }
    }

    @Override
    public ClientRef findById(int clientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + " WHERE client_id = ?")) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        }
    }

    private static ClientRef read(ResultSet rs) throws SQLException {
        return new ClientRef(rs.getInt("client_id"), rs.getString("name"),
                rs.getString("address"));
    }
}
