package com.econest.order.dao;

import com.econest.order.model.Money;
import com.econest.order.model.Order;
import com.econest.order.model.OrderItem;
import com.econest.order.model.OrderStatus;
import com.econest.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access for {@code orders} and {@code order_items}.
 *
 * <p>This is a rewrite of the earlier flat OrderDao, which inserted
 * {@code (client_id, product_id, quantity, order_date, total_amount,
 * order_status, delivery_agent_id)} into {@code orders} and could never have
 * worked against the normalised schema - {@code database/README.md} logged it
 * as this module's open item. The three fixes it asked for are all here:
 * {@code order_status} is {@code status}, the product and quantity became one
 * {@code order_items} row per product, and the delivery agent belongs to
 * {@code delivery_assignments}, which is the delivery module's table and is
 * not touched from here.</p>
 *
 * <h2>Writing stock from an order</h2>
 * <p>Saving an order has to insert the header, insert every line <b>and</b>
 * deduct {@code products.quantity}, with all of it landing or none of it. A
 * call to {@code InventoryService} or {@code ProductDao} would open its own
 * connection and so could not join this transaction - a half-saved order with
 * stock already gone is exactly the failure a transaction exists to prevent.
 * The stock write therefore happens on this connection, as the one statement
 * {@code database/README.md} prescribes for the on-hand count:
 * {@code UPDATE products SET quantity = ? WHERE product_id = ?}. Reads of the
 * catalogue still go through {@code ProductService}.</p>
 *
 * <p>Every method uses try-with-resources and {@link PreparedStatement}, and
 * every multi-statement method sets {@code autoCommit(false)}, commits at the
 * end and rolls back on any throw.</p>
 */
public class OrderDao {

    /** The header columns, in the order {@link #readOrder} expects them. */
    private static final String ORDER_COLUMNS =
            "o.order_id, o.client_id, o.order_date, o.delivery_date, o.status, "
                    + "o.total_amount, o.delivery_address, o.notes";

    /** Header plus the client's name, which every list on screen shows. */
    private static final String SELECT_ORDERS =
            "SELECT " + ORDER_COLUMNS + ", c.name AS client_name "
                    + "FROM orders o JOIN clients c ON c.client_id = o.client_id";

    private static final String INSERT_ORDER =
            "INSERT INTO orders (client_id, order_date, delivery_date, status, "
                    + "total_amount, delivery_address, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ITEM =
            "INSERT INTO order_items (order_id, product_id, quantity, unit_price) "
                    + "VALUES (?, ?, ?, ?)";

    /**
     * Takes stock off the shelf. The {@code quantity >= ?} guard is what makes
     * this safe: if someone else sold the last unit between the stock check
     * and this write, the update matches no row, the count comes back 0 and
     * the whole transaction rolls back instead of driving the column negative
     * (which {@code chk_products_quantity} would reject anyway, less clearly).
     */
    private static final String DEDUCT_STOCK =
            "UPDATE products SET quantity = quantity - ? "
                    + "WHERE product_id = ? AND quantity >= ?";

    /** Puts stock back, for a cancellation, an edit or a delete. */
    private static final String RESTORE_STOCK =
            "UPDATE products SET quantity = quantity + ? WHERE product_id = ?";

    // =====================================================================
    // Reads
    // =====================================================================

    /**
     * Orders matching a search and a status, newest first.
     *
     * @param query  matches the client name or the order number; blank for all
     * @param status the only status to show, or null for every status
     */
    public List<Order> search(String query, OrderStatus status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ORDERS);
        List<Object> params = new ArrayList<>();

        boolean hasQuery = query != null && !query.isBlank();
        if (hasQuery) {
            // The order number is derived from the id rather than stored, so
            // it is matched by formatting the id the same way in SQL. That
            // lets "ORD-00012", "12" and "ord-12" all find order 12.
            sql.append(" WHERE (c.name LIKE ? OR LPAD(o.order_id, 5, '0') LIKE ?)");
            params.add("%" + query.trim() + "%");
            params.add("%" + digitsOf(query) + "%");
        }
        if (status != null) {
            sql.append(hasQuery ? " AND" : " WHERE").append(" o.status = ?");
            params.add(status.name());
        }
        sql.append(" ORDER BY o.order_date DESC, o.order_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return readOrders(rs);
            }
        }
    }

    /** One order with its items loaded, or null when there is no such order. */
    public Order findById(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     SELECT_ORDERS + " WHERE o.order_id = ?")) {

            ps.setInt(1, orderId);
            Order order;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                order = readOrder(rs);
            }
            order.setItems(findItems(conn, orderId));
            return order;
        }
    }

    /** The lines on an order, with each product's current name. */
    public List<OrderItem> findItems(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findItems(conn, orderId);
        }
    }

    /** Just the stored status, or null when there is no such order. */
    public OrderStatus findStatus(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT status FROM orders WHERE order_id = ?")) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? OrderStatus.of(rs.getString(1)) : null;
            }
        }
    }

    /**
     * Orders that are waiting for a delivery agent: PENDING or CONFIRMED and
     * with no row in {@code delivery_assignments} yet. This is the list the
     * delivery module's assignment screen works from.
     */
    public List<Order> findPendingUnassigned() throws SQLException {
        String sql = SELECT_ORDERS
                + " LEFT JOIN delivery_assignments d ON d.order_id = o.order_id"
                + " WHERE o.status IN ('PENDING', 'CONFIRMED') AND d.order_id IS NULL"
                + " ORDER BY o.order_date";
        return list(sql);
    }

    /** Every order placed in one calendar month, cancelled ones included. */
    public List<Order> findByMonth(int year, int month) throws SQLException {
        String sql = SELECT_ORDERS
                + " WHERE YEAR(o.order_date) = ? AND MONTH(o.order_date) = ?"
                + " ORDER BY o.order_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                return readOrders(rs);
            }
        }
    }

    /** How many orders sit in one status right now. */
    public int countByStatus(OrderStatus status) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM orders WHERE status = ?")) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * What one month actually earned: the sum of {@code total_amount} over
     * every order that was not cancelled. Cancelled orders are excluded the
     * same way {@code v_monthly_sales_summary} excludes them, so the order
     * screen and the reports screen cannot disagree.
     */
    public BigDecimal revenueForMonth(int year, int month) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders "
                + "WHERE YEAR(order_date) = ? AND MONTH(order_date) = ? "
                + "AND status <> 'CANCELLED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                return Money.scale(rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO);
            }
        }
    }

    // =====================================================================
    // Writes - each one a single transaction
    // =====================================================================

    /**
     * Saves a new order: the {@code orders} row, every {@code order_items}
     * row and the stock deduction for each line, in <b>one</b> transaction.
     * Any failure - a bad product id, stock sold out from under us, a
     * constraint - rolls the whole thing back, so there is never an order
     * without its items, nor stock gone without an order.
     *
     * @return the generated {@code order_id}
     * @throws SQLException if anything failed; nothing was written
     */
    public int insert(Order order) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId = insertHeader(conn, order);
                insertItems(conn, orderId, order.getItems());
                deductStock(conn, order.getItems());

                conn.commit();
                order.setOrderId(orderId);
                return orderId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Rewrites a PENDING order's header and lines.
     *
     * <p>Stock is squared up inside the same transaction: the old lines give
     * their stock back first, then the new lines take theirs. Doing it in
     * that order means raising a quantity on a product that is down to its
     * last few units still works, because the units this order already held
     * are back on the shelf before the new figure is taken.</p>
     *
     * @throws SQLException if the order is no longer PENDING, or any write
     *                      failed; nothing was changed
     */
    public void update(Order order) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Re-read the status inside the transaction. Checking it in
                // the service is for the user's benefit; checking it here is
                // what actually stops a dispatched order being rewritten by
                // a window that was left open.
                OrderStatus stored = lockStatus(conn, order.getOrderId());
                if (stored == null) {
                    throw new SQLException("Order " + order.getOrderNo()
                            + " no longer exists.");
                }
                if (!stored.isEditable()) {
                    throw new SQLException("Order " + order.getOrderNo() + " is "
                            + stored.label().toLowerCase()
                            + " and can no longer be edited.");
                }

                restoreStock(conn, findItems(conn, order.getOrderId()));
                deleteItems(conn, order.getOrderId());

                updateHeader(conn, order);
                insertItems(conn, order.getOrderId(), order.getItems());
                deductStock(conn, order.getItems());

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Moves an order to a new status. One statement, so no explicit
     * transaction is needed - but the {@code status = ?} guard on the current
     * value makes it safe against two screens moving the same order at once:
     * the second update matches nothing and is reported rather than silently
     * overwriting the first.
     *
     * @throws SQLException if the order's status was not {@code from} any more
     */
    public void updateStatus(int orderId, OrderStatus from, OrderStatus to)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE orders SET status = ? WHERE order_id = ? AND status = ?")) {

            ps.setString(1, to.name());
            ps.setInt(2, orderId);
            ps.setString(3, from.name());

            if (ps.executeUpdate() == 0) {
                throw new SQLException("Order #" + orderId + " is no longer "
                        + from.label().toLowerCase()
                        + ". Refresh the list and try again.");
            }
        }
    }

    /**
     * Cancels an order and puts every unit it held back on the shelf, in one
     * transaction. Cancelling an already-cancelled order is rejected rather
     * than quietly handing the stock back twice.
     */
    public void cancel(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderStatus stored = lockStatus(conn, orderId);
                if (stored == null) {
                    throw new SQLException("Order #" + orderId + " no longer exists.");
                }
                if (stored == OrderStatus.CANCELLED) {
                    throw new SQLException("Order #" + orderId + " is already cancelled.");
                }

                restoreStock(conn, findItems(conn, orderId));
                setStatus(conn, orderId, OrderStatus.CANCELLED);

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Deletes an order outright, returning any stock it still held.
     *
     * <p>Refused once the order is dispatched or delivered: those have left
     * the building, the delivery and reports modules both read them, and the
     * record of what a client was sent is not ours to erase. Cancel instead.
     * {@code order_items} goes with it through {@code ON DELETE CASCADE}.</p>
     */
    public void delete(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                OrderStatus stored = lockStatus(conn, orderId);
                if (stored == null) {
                    throw new SQLException("Order #" + orderId + " no longer exists.");
                }
                if (stored.isOutOfOurHands()) {
                    throw new SQLException("Order #" + orderId + " is "
                            + stored.label().toLowerCase()
                            + " and cannot be deleted. Cancel it instead.");
                }

                if (stored.holdsStock()) {
                    restoreStock(conn, findItems(conn, orderId));
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM orders WHERE order_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // =====================================================================
    // Statement helpers - all take the caller's connection, so they join
    // whatever transaction is already open.
    // =====================================================================

    private int insertHeader(Connection conn, Order order) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                INSERT_ORDER, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getClientId());
            ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
            setDate(ps, 3, order.getDeliveryDate());
            ps.setString(4, order.getStatus().name());
            ps.setBigDecimal(5, Money.scale(order.getTotalAmount()));
            ps.setString(6, order.getDeliveryAddress());
            ps.setString(7, order.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return an order id.");
                }
                return keys.getInt(1);
            }
        }
    }

    private void updateHeader(Connection conn, Order order) throws SQLException {
        String sql = "UPDATE orders SET client_id = ?, order_date = ?, delivery_date = ?, "
                + "total_amount = ?, delivery_address = ?, notes = ? WHERE order_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getClientId());
            ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
            setDate(ps, 3, order.getDeliveryDate());
            ps.setBigDecimal(4, Money.scale(order.getTotalAmount()));
            ps.setString(5, order.getDeliveryAddress());
            ps.setString(6, order.getNotes());
            ps.setInt(7, order.getOrderId());
            ps.executeUpdate();
        }
    }

    /** One batched insert for every line on the order. */
    private void insertItems(Connection conn, int orderId, List<OrderItem> items)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
            for (OrderItem item : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, item.getProductId());
                ps.setInt(3, item.getQuantity());
                ps.setBigDecimal(4, Money.scale(item.getUnitPrice()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void deleteItems(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM order_items WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * Takes each line's quantity off {@code products.quantity}. A line that
     * matches no row means the stock went while we were deciding, which fails
     * the whole transaction with a sentence the user can act on.
     */
    private void deductStock(Connection conn, List<OrderItem> items) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DEDUCT_STOCK)) {
            for (OrderItem item : items) {
                ps.setInt(1, item.getQuantity());
                ps.setInt(2, item.getProductId());
                ps.setInt(3, item.getQuantity());

                if (ps.executeUpdate() == 0) {
                    throw new SQLException("There is no longer enough stock of "
                            + nameOf(item) + " for " + item.getQuantity()
                            + " unit(s). Someone else may have just sold it. "
                            + "Nothing was saved.");
                }
            }
        }
    }

    /** Puts each line's quantity back on the shelf. */
    private void restoreStock(Connection conn, List<OrderItem> items) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(RESTORE_STOCK)) {
            for (OrderItem item : items) {
                ps.setInt(1, item.getQuantity());
                ps.setInt(2, item.getProductId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Reads a status {@code FOR UPDATE}, so the row stays put for the rest of
     * the transaction. Without the lock, two cancellations racing each other
     * would both read PENDING and both hand the stock back.
     */
    private OrderStatus lockStatus(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM orders WHERE order_id = ? FOR UPDATE")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? OrderStatus.of(rs.getString(1)) : null;
            }
        }
    }

    private void setStatus(Connection conn, int orderId, OrderStatus status)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE orders SET status = ? WHERE order_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private List<OrderItem> findItems(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT i.order_item_id, i.order_id, i.product_id, i.quantity, "
                + "i.unit_price, p.product_name "
                + "FROM order_items i JOIN products p ON p.product_id = i.product_id "
                + "WHERE i.order_id = ? ORDER BY i.order_item_id";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    items.add(item);
                }
                return items;
            }
        }
    }

    private List<Order> list(String sql) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return readOrders(rs);
        }
    }

    private static List<Order> readOrders(ResultSet rs) throws SQLException {
        List<Order> orders = new ArrayList<>();
        while (rs.next()) {
            orders.add(readOrder(rs));
        }
        return orders;
    }

    private static Order readOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setClientId(rs.getInt("client_id"));
        order.setClientName(rs.getString("client_name"));

        Timestamp placed = rs.getTimestamp("order_date");
        if (placed != null) {
            order.setOrderDate(placed.toLocalDateTime());
        }
        Date wanted = rs.getDate("delivery_date");
        if (wanted != null) {
            order.setDeliveryDate(wanted.toLocalDate());
        }

        order.setStatus(OrderStatus.of(rs.getString("status")));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setNotes(rs.getString("notes"));
        return order;
    }

    private static void setDate(PreparedStatement ps, int index, LocalDate date)
            throws SQLException {
        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /**
     * Only the digits of a search term, so "ORD-00012" can match id 12. A
     * term with no digits in it returns a string no id can contain, rather
     * than an empty one that LIKE '%%' would match against every row.
     */
    private static String digitsOf(String query) {
        String digits = query.replaceAll("\\D", "");
        return digits.isEmpty() ? "\u0000" : digits;
    }

    private static String nameOf(OrderItem item) {
        return item.getProductName() == null || item.getProductName().isBlank()
                ? "product #" + item.getProductId()
                : item.getProductName();
    }
}
