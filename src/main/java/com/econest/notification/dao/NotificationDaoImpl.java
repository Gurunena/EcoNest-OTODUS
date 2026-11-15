package com.econest.notification.dao;

import com.econest.notification.model.EmailNotification;
import com.econest.notification.model.NotificationType;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** MySQL version of {@link NotificationDao}. */
public class NotificationDaoImpl implements NotificationDao {

    private static final String SQL_INSERT = """
            INSERT INTO notifications (notification_type, recipient_email, subject, body, status,
                                       order_id, sent_at, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_SELECT = """
            SELECT notification_id, notification_type, recipient_email, subject, body, status,
                   order_id, created_at, sent_at, error_message
            FROM notifications
            """;

    private static final String SQL_FIND_ALL = SQL_SELECT + " ORDER BY notification_id DESC";

    private static final String SQL_SEARCH = SQL_SELECT + """
             WHERE recipient_email LIKE ? OR subject LIKE ? OR notification_type LIKE ? OR status LIKE ?
            ORDER BY notification_id DESC
            """;

    private static final String SQL_COUNT_SENT = "SELECT COUNT(*) FROM notifications WHERE status = 'SENT'";

    @Override
    public void save(EmailNotification n) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getType().name());
            ps.setString(2, n.getRecipientEmail());
            ps.setString(3, shorten(n.getSubject(), 200));
            ps.setString(4, n.getBody());
            ps.setString(5, n.getStatus());
            ps.setObject(6, n.getOrderId(), Types.INTEGER);   // works when the order id is null too
            ps.setTimestamp(7, n.getSentAt() == null ? null : Timestamp.valueOf(n.getSentAt()));
            ps.setString(8, shorten(n.getErrorMessage(), 255));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    n.setNotificationId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public List<EmailNotification> findAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL)) {
            return readAll(ps);
        }
    }

    @Override
    public List<EmailNotification> search(String text) throws SQLException {
        String like = "%" + text + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, like);
            }
            return readAll(ps);
        }
    }

    @Override
    public int countSent() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_SENT);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private List<EmailNotification> readAll(PreparedStatement ps) throws SQLException {
        List<EmailNotification> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Turns the current row into an EmailNotification object. */
    private EmailNotification mapRow(ResultSet rs) throws SQLException {
        int orderId = rs.getInt("order_id");
        Integer orderIdOrNull = rs.wasNull() ? null : orderId;

        return new EmailNotification(
                rs.getInt("notification_id"),
                NotificationType.valueOf(rs.getString("notification_type")),
                rs.getString("recipient_email"),
                rs.getString("subject"),
                rs.getString("body"),
                rs.getString("status"),
                orderIdOrNull,
                toDateTime(rs.getTimestamp("created_at")),
                toDateTime(rs.getTimestamp("sent_at")),
                rs.getString("error_message"));
    }

    private static LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /** The table's columns have a maximum length, so cut long text down to fit. */
    private static String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }
}
