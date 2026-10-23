package com.econest.delivery.dao;

import com.econest.delivery.model.AvailableAgent;
import com.econest.delivery.model.DeliveryAssignment;
import com.econest.delivery.model.DeliveryStatus;
import com.econest.delivery.model.PendingOrder;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link DeliveryAssignmentDao}.
 *
 * <p>Every statement is a {@link PreparedStatement} in try-with-resources.
 * The three write operations run inside an explicit transaction, because
 * each one has to keep {@code delivery_assignments}, {@code orders.status}
 * and {@code delivery_agents.available} consistent with each other.</p>
 */
public class DeliveryAssignmentDaoImpl implements DeliveryAssignmentDao {

    /**
     * The order status that means "out for delivery".
     *
     * <p>{@code orders.status} is
     * {@code ENUM('PENDING','CONFIRMED','PACKED','DISPATCHED','DELIVERED','CANCELLED')}
     * and has no OUT_FOR_DELIVERY member. DISPATCHED is the existing value
     * for that state, so this module uses it rather than changing an enum
     * the order module shares.</p>
     */
    public static final String STATUS_OUT_FOR_DELIVERY = "DISPATCHED";

    /** Where an order goes back to when its assignment is removed. */
    public static final String STATUS_UNASSIGNED = "CONFIRMED";

    // -----------------------------------------------------------------
    // SQL
    // -----------------------------------------------------------------

    private static final String SQL_UNASSIGNED_ORDERS = """
            SELECT o.order_id, o.order_date, o.status, o.total_amount,
                   o.delivery_address, c.name AS client_name
            FROM orders o
                     JOIN clients c ON c.client_id = o.client_id
                     LEFT JOIN delivery_assignments d ON d.order_id = o.order_id
            WHERE o.status IN ('PENDING', 'CONFIRMED')
              AND d.assignment_id IS NULL
            ORDER BY o.order_date
            """;

    private static final String SQL_AGENTS_BASE = """
            SELECT agent_id, name, phone, area_covered, available
            FROM delivery_agents
            """;

    private static final String SQL_AVAILABLE_AGENTS =
            SQL_AGENTS_BASE + " WHERE available = TRUE ORDER BY name";

    private static final String SQL_ALL_AGENTS =
            SQL_AGENTS_BASE + " ORDER BY name";

    private static final String SQL_ALL_ASSIGNMENTS = """
            SELECT d.assignment_id, d.order_id, d.agent_id, d.status,
                   d.assigned_at, d.delivered_at,
                   a.name AS agent_name,
                   c.name AS client_name,
                   o.total_amount, o.delivery_address
            FROM delivery_assignments d
                     JOIN delivery_agents a ON a.agent_id = d.agent_id
                     JOIN orders o          ON o.order_id = d.order_id
                     JOIN clients c         ON c.client_id = o.client_id
            ORDER BY d.assigned_at DESC
            """;

    /**
     * Inserts only when the agent is still available. If someone else grabbed
     * them first the SELECT yields no row, nothing is inserted, and the
     * caller reports it - this closes the gap between reading the combo box
     * and pressing Assign.
     */
    private static final String SQL_INSERT_ASSIGNMENT = """
            INSERT INTO delivery_assignments (order_id, agent_id, status)
            SELECT ?, agent_id, 'ASSIGNED'
            FROM delivery_agents
            WHERE agent_id = ? AND available = TRUE
            """;

    private static final String SQL_UPDATE_ORDER_STATUS =
            "UPDATE orders SET status = ? WHERE order_id = ?";

    private static final String SQL_SET_AGENT_AVAILABILITY =
            "UPDATE delivery_agents SET available = ? WHERE agent_id = ?";

    private static final String SQL_FIND_AGENT_OF_ORDER =
            "SELECT agent_id FROM delivery_assignments WHERE order_id = ?";

    private static final String SQL_REASSIGN = """
            UPDATE delivery_assignments d
                JOIN delivery_agents a ON a.agent_id = ?
            SET d.agent_id = a.agent_id, d.status = 'ASSIGNED'
            WHERE d.order_id = ? AND a.available = TRUE
            """;

    private static final String SQL_DELETE_ASSIGNMENT =
            "DELETE FROM delivery_assignments WHERE order_id = ?";

    // -----------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------

    @Override
    public List<PendingOrder> findUnassignedOrders() throws SQLException {
        List<PendingOrder> orders = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UNASSIGNED_ORDERS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                PendingOrder order = new PendingOrder();
                order.setOrderId(rs.getInt("order_id"));
                order.setClientName(rs.getString("client_name"));
                order.setOrderDate(toDateTime(rs.getTimestamp("order_date")));
                order.setTotalAmount(rs.getBigDecimal("total_amount"));
                order.setStatus(rs.getString("status"));
                order.setDeliveryAddress(rs.getString("delivery_address"));
                orders.add(order);
            }
        }
        return orders;
    }

    @Override
    public List<AvailableAgent> findAvailableAgents() throws SQLException {
        return readAgents(SQL_AVAILABLE_AGENTS);
    }

    @Override
    public List<AvailableAgent> findAllAgents() throws SQLException {
        return readAgents(SQL_ALL_AGENTS);
    }

    private List<AvailableAgent> readAgents(String sql) throws SQLException {
        List<AvailableAgent> agents = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                agents.add(new AvailableAgent(
                        rs.getInt("agent_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("area_covered"),
                        rs.getBoolean("available")));
            }
        }
        return agents;
    }

    @Override
    public List<DeliveryAssignment> findAllAssignments() throws SQLException {
        List<DeliveryAssignment> assignments = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ALL_ASSIGNMENTS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                DeliveryAssignment assignment = new DeliveryAssignment();
                assignment.setId(rs.getInt("assignment_id"));
                assignment.setOrderId(rs.getInt("order_id"));
                assignment.setAgentId(rs.getInt("agent_id"));
                assignment.setAgentName(rs.getString("agent_name"));
                assignment.setClientName(rs.getString("client_name"));
                assignment.setOrderTotal(rs.getBigDecimal("total_amount"));
                assignment.setDeliveryAddress(rs.getString("delivery_address"));
                assignment.setStatus(toStatus(rs.getString("status")));
                assignment.setAssignedAt(toDateTime(rs.getTimestamp("assigned_at")));
                assignment.setDeliveredAt(toDateTime(rs.getTimestamp("delivered_at")));
                assignments.add(assignment);
            }
        }
        return assignments;
    }

    // -----------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------

    @Override
    public void assign(int orderId, int agentId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_ASSIGNMENT)) {
                ps.setInt(1, orderId);
                ps.setInt(2, agentId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException(
                            "That agent is no longer available. Refresh and pick another.");
                }
            } catch (SQLIntegrityConstraintViolationException ex) {
                // uq_assignments_order: somebody assigned this order between
                // the list being drawn and Assign being pressed.
                throw new SQLException(
                        "Order " + orderId + " already has a delivery agent. Press Refresh "
                                + "to see who, then use Reassign if you want to change it.", ex);
            }

            setOrderStatus(conn, orderId, STATUS_OUT_FOR_DELIVERY);
            setAgentAvailability(conn, agentId, false);

            conn.commit();
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            close(conn);
        }
    }

    @Override
    public void reassign(int orderId, int newAgentId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int previousAgentId = currentAgentOf(conn, orderId);
            if (previousAgentId == 0) {
                throw new SQLException("Order " + orderId + " has no assignment to move.");
            }
            if (previousAgentId == newAgentId) {
                throw new SQLException("That order is already assigned to this agent.");
            }

            try (PreparedStatement ps = conn.prepareStatement(SQL_REASSIGN)) {
                ps.setInt(1, newAgentId);
                ps.setInt(2, orderId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException(
                            "That agent is no longer available. Refresh and pick another.");
                }
            }

            setAgentAvailability(conn, previousAgentId, true);
            setAgentAvailability(conn, newAgentId, false);
            setOrderStatus(conn, orderId, STATUS_OUT_FOR_DELIVERY);

            conn.commit();
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            close(conn);
        }
    }

    @Override
    public void unassign(int orderId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int agentId = currentAgentOf(conn, orderId);
            if (agentId == 0) {
                throw new SQLException("Order " + orderId + " has no assignment to remove.");
            }

            try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_ASSIGNMENT)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            setAgentAvailability(conn, agentId, true);
            setOrderStatus(conn, orderId, STATUS_UNASSIGNED);

            conn.commit();
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            close(conn);
        }
    }

    // -----------------------------------------------------------------
    // Shared statements, run inside a caller's transaction
    // -----------------------------------------------------------------

    private static void setOrderStatus(Connection conn, int orderId, String status)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_ORDER_STATUS)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No order found with id " + orderId + ".");
            }
        }
    }

    private static void setAgentAvailability(Connection conn, int agentId, boolean available)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SET_AGENT_AVAILABILITY)) {
            ps.setBoolean(1, available);
            ps.setInt(2, agentId);
            ps.executeUpdate();
        }
    }

    /** The agent currently on an order, or 0 when it has no assignment. */
    private static int currentAgentOf(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_AGENT_OF_ORDER)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("agent_id") : 0;
            }
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /** Unknown or null status values fall back to ASSIGNED rather than throwing. */
    private static DeliveryStatus toStatus(String value) {
        if (value == null) {
            return DeliveryStatus.ASSIGNED;
        }
        try {
            return DeliveryStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return DeliveryStatus.ASSIGNED;
        }
    }

    private static LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void rollback(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ex) {
            System.err.println("DeliveryAssignmentDao: rollback failed - " + ex.getMessage());
        }
    }

    private static void close(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException ex) {
            System.err.println("DeliveryAssignmentDao: closing failed - " + ex.getMessage());
        }
    }
}
