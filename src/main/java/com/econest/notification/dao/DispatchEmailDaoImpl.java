package com.econest.notification.dao;

import com.econest.notification.model.DispatchedOrder;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySQL version of {@link DispatchEmailDao}. */
public class DispatchEmailDaoImpl implements DispatchEmailDao {

    /** The notifications.notification_type value used for dispatch emails. */
    public static final String TYPE_DISPATCH = "ORDER_STATUS_CHANGE";

    // The order, plus the agent delivering it (LEFT JOIN: still found if no agent yet)
    private static final String SQL_ORDER = """
            SELECT o.order_id, o.client_id, o.status, o.order_date, o.total_amount, o.delivery_address,
                   a.name AS agent_name, a.phone AS agent_phone
            FROM orders o
                     LEFT JOIN delivery_assignments d ON d.order_id = o.order_id
                     LEFT JOIN delivery_agents a ON a.agent_id = d.agent_id
            WHERE o.order_id = ?
            """;

    private static final String SQL_ITEMS = """
            SELECT p.product_name, oi.quantity
            FROM order_items oi
                     JOIN products p ON p.product_id = oi.product_id
            WHERE oi.order_id = ?
            ORDER BY oi.order_item_id
            """;

    private static final String SQL_ALREADY_SENT = """
            SELECT COUNT(*) FROM notifications
            WHERE order_id = ? AND notification_type = 'ORDER_STATUS_CHANGE' AND status = 'SENT'
            """;

    private static final String SQL_AWAITING = """
            SELECT o.order_id FROM orders o
            WHERE o.status = 'DISPATCHED'
              AND NOT EXISTS (SELECT 1 FROM notifications n
                              WHERE n.order_id = o.order_id
                                AND n.notification_type = 'ORDER_STATUS_CHANGE'
                                AND n.status = 'SENT')
            ORDER BY o.order_id
            """;

    private static final String SQL_LOG = """
            INSERT INTO notifications (notification_type, recipient_email, subject, body, status,
                                       client_id, order_id, sent_at, error_message)
            VALUES ('ORDER_STATUS_CHANGE', ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public Optional<DispatchedOrder> findOrder(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DispatchedOrder order;
            try (PreparedStatement ps = conn.prepareStatement(SQL_ORDER)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    Timestamp ordered = rs.getTimestamp("order_date");
                    order = new DispatchedOrder(
                            rs.getInt("order_id"),
                            rs.getInt("client_id"),
                            rs.getString("status"),
                            ordered == null ? null : ordered.toLocalDateTime(),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("delivery_address"),
                            rs.getString("agent_name"),
                            rs.getString("agent_phone"));
                }
            }
            // Second query on the same connection: what was ordered
            try (PreparedStatement ps = conn.prepareStatement(SQL_ITEMS)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        order.addItem(rs.getString("product_name"), rs.getInt("quantity"));
                    }
                }
            }
            return Optional.of(order);
        }
    }

    @Override
    public boolean dispatchEmailAlreadySent(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ALREADY_SENT)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public List<Integer> findOrdersAwaitingDispatchEmail() throws SQLException {
        List<Integer> orderIds = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_AWAITING);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orderIds.add(rs.getInt(1));
            }
        }
        return orderIds;
    }

    @Override
    public void logDispatchEmail(int clientId, int orderId, String recipient, String subject, String body,
                                 boolean sent, String errorMessage) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOG)) {
            ps.setString(1, recipient);
            ps.setString(2, shorten(subject, 200));
            ps.setString(3, body);
            ps.setString(4, sent ? "SENT" : "FAILED");
            ps.setInt(5, clientId);
            ps.setInt(6, orderId);
            if (sent) {
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setString(8, shorten(errorMessage, 255));
            ps.executeUpdate();
        }
    }

    /** The table's columns have a maximum length, so cut long text down to fit. */
    private static String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }
}
