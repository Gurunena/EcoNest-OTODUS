package com.econest.inventory.dao;

import com.econest.inventory.model.StockItem;
import com.econest.inventory.model.Supplier;
import com.econest.product.model.Product;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link InventoryDao}.
 *
 * <p>Every statement is a {@link PreparedStatement} inside try-with-resources.
 * The two write operations - restock and adjust - each touch two tables, so
 * they run inside an explicit transaction: either both rows land or neither
 * does, and the stock count can never drift from its history.</p>
 */
public class InventoryDaoImpl implements InventoryDao {

    /** MySQL SQLState for "table does not exist". */
    private static final String SQLSTATE_NO_SUCH_TABLE = "42S02";

    // -----------------------------------------------------------------
    // SQL
    // -----------------------------------------------------------------

    /**
     * Stock level per product. LEFT JOIN so a product with no stock_items
     * row still appears, with a reorder level of 0.
     */
    private static final String SQL_FIND_ALL = """
            SELECT p.product_id, p.product_name, p.category, p.price,
                   p.eco_rating, p.description, p.quantity,
                   COALESCE(s.reorder_level, 0) AS reorder_level
            FROM products p
                     LEFT JOIN stock_items s ON s.product_id = p.product_id
            ORDER BY p.product_name
            """;

    private static final String SQL_FIND_BY_ID = """
            SELECT p.product_id, p.product_name, p.category, p.price,
                   p.eco_rating, p.description, p.quantity,
                   COALESCE(s.reorder_level, 0) AS reorder_level
            FROM products p
                     LEFT JOIN stock_items s ON s.product_id = p.product_id
            WHERE p.product_id = ?
            """;

    /** Name or category match. The caller supplies the %wildcards%. */
    private static final String SQL_SEARCH = """
            SELECT p.product_id, p.product_name, p.category, p.price,
                   p.eco_rating, p.description, p.quantity,
                   COALESCE(s.reorder_level, 0) AS reorder_level
            FROM products p
                     LEFT JOIN stock_items s ON s.product_id = p.product_id
            WHERE p.product_name LIKE ? OR p.category LIKE ?
            ORDER BY p.product_name
            """;

    private static final String SQL_SUPPLIERS =
            "SELECT supplier_id, name, contact FROM suppliers ORDER BY name";

    private static final String SQL_INSERT_STOCK_IN = """
            INSERT INTO stock_in (product_id, supplier_id, quantity, received_date)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SQL_INSERT_ADJUSTMENT = """
            INSERT INTO stock_adjustments (product_id, quantity_delta, reason)
            VALUES (?, ?, ?)
            """;

    /**
     * Adds a signed delta to the stock count. The {@code quantity + ? >= 0}
     * guard means a write-off that would go negative changes no rows, which
     * the caller turns into a clear error - belt and braces alongside the
     * chk_products_quantity constraint.
     */
    private static final String SQL_ADD_QUANTITY = """
            UPDATE products SET quantity = quantity + ?
            WHERE product_id = ? AND quantity + ? >= 0
            """;

    /** Upsert, so setting a level works whether or not the row exists yet. */
    private static final String SQL_UPSERT_REORDER = """
            INSERT INTO stock_items (product_id, reorder_level) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE reorder_level = VALUES(reorder_level)
            """;

    // -----------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------

    @Override
    public List<StockItem> findAll() throws SQLException {
        List<StockItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(mapStockItem(rs));
            }
        }
        return items;
    }

    @Override
    public Optional<StockItem> findById(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapStockItem(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<StockItem> search(String query) throws SQLException {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String pattern = "%" + query.trim() + "%";

        List<StockItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapStockItem(rs));
                }
            }
        }
        return items;
    }

    @Override
    public List<Supplier> findAllSuppliers() throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SUPPLIERS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Supplier supplier = new Supplier();
                supplier.setId(rs.getInt("supplier_id"));
                supplier.setName(rs.getString("name"));
                supplier.setContact(rs.getString("contact"));
                suppliers.add(supplier);
            }
        }
        return suppliers;
    }

    // -----------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------

    @Override
    public void restock(int productId, int supplierId, int quantity, LocalDate receivedDate)
            throws SQLException {

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psIn = conn.prepareStatement(SQL_INSERT_STOCK_IN)) {
                psIn.setInt(1, productId);
                psIn.setInt(2, supplierId);
                psIn.setInt(3, quantity);
                psIn.setDate(4, java.sql.Date.valueOf(receivedDate));
                psIn.executeUpdate();
            }

            try (PreparedStatement psQty = conn.prepareStatement(SQL_ADD_QUANTITY)) {
                psQty.setInt(1, quantity);
                psQty.setInt(2, productId);
                psQty.setInt(3, quantity);
                if (psQty.executeUpdate() == 0) {
                    throw new SQLException("No product found with id " + productId + ".");
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            close(conn);
        }
    }

    @Override
    public void adjustStock(int productId, int quantityDelta, String reason) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Apply the change first: if it would go negative, no row is
            // updated and we stop before writing a misleading history row.
            try (PreparedStatement psQty = conn.prepareStatement(SQL_ADD_QUANTITY)) {
                psQty.setInt(1, quantityDelta);
                psQty.setInt(2, productId);
                psQty.setInt(3, quantityDelta);
                if (psQty.executeUpdate() == 0) {
                    throw new SQLException(
                            "That adjustment would take the stock below zero, "
                                    + "or the product no longer exists.");
                }
            }

            try (PreparedStatement psLog = conn.prepareStatement(SQL_INSERT_ADJUSTMENT)) {
                psLog.setInt(1, productId);
                psLog.setInt(2, quantityDelta);
                psLog.setString(3, reason);
                psLog.executeUpdate();
            } catch (SQLSyntaxErrorException ex) {
                // The table ships as a migration, so spell out the fix
                // instead of leaking "Table 'econest.stock_adjustments'
                // doesn't exist" at the user.
                if (SQLSTATE_NO_SUCH_TABLE.equals(ex.getSQLState())) {
                    throw new SQLException(
                            "The stock_adjustments table is missing, so the adjustment was "
                                    + "not saved. Run database/migrations/"
                                    + "001_stock_adjustments.sql against the econest database, "
                                    + "then try again.", ex);
                }
                throw ex;
            }

            conn.commit();
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            close(conn);
        }
    }

    @Override
    public void setReorderLevel(int productId, int reorderLevel) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPSERT_REORDER)) {

            ps.setInt(1, productId);
            ps.setInt(2, reorderLevel);
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /** Builds the StockItem (and its Product) from the current result row. */
    private static StockItem mapStockItem(ResultSet rs) throws SQLException {
        Product product = new Product(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("eco_rating"),
                rs.getString("description"),
                rs.getInt("quantity"));

        return new StockItem(product, rs.getInt("quantity"), rs.getInt("reorder_level"));
    }

    private static void rollback(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ex) {
            System.err.println("InventoryDao: rollback failed - " + ex.getMessage());
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
            System.err.println("InventoryDao: closing the connection failed - " + ex.getMessage());
        }
    }
}
