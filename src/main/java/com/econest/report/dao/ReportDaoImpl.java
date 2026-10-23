package com.econest.report.dao;

import com.econest.inventory.model.StockItem;
import com.econest.product.model.Product;
import com.econest.report.model.AgentDeliveryCount;
import com.econest.report.model.LowStockAlert;
import com.econest.report.model.MonthlySalesSummary;
import com.econest.report.model.TopSellingProduct;
import com.econest.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link ReportDao}.
 *
 * <p>Every statement is a {@link PreparedStatement} inside try-with-resources,
 * so nothing is string-concatenated into SQL and no connection is left open.
 * All queries are read-only.</p>
 */
public class ReportDaoImpl implements ReportDao {

    // -----------------------------------------------------------------
    // SQL
    // -----------------------------------------------------------------

    /**
     * Monthly totals. Mirrors the v_monthly_sales_summary view but filtered
     * to one month, so the database does the grouping rather than Java.
     */
    private static final String SQL_MONTHLY_SUMMARY = """
            SELECT COUNT(DISTINCT o.order_id)                     AS order_count,
                   COALESCE(SUM(oi.quantity), 0)                  AS units_sold,
                   COALESCE(SUM(oi.quantity * oi.unit_price), 0)  AS total_revenue
            FROM orders o
                     LEFT JOIN order_items oi ON oi.order_id = o.order_id
            WHERE o.status <> 'CANCELLED'
              AND YEAR(o.order_date) = ?
              AND MONTH(o.order_date) = ?
            """;

    /**
     * Low stock. Joined against products directly rather than through
     * v_low_stock_items, because the view does not expose price, category or
     * eco rating and the Product model wants them.
     */
    private static final String SQL_LOW_STOCK = """
            SELECT p.product_id,
                   p.product_name,
                   p.category,
                   p.price,
                   p.eco_rating,
                   p.description,
                   p.quantity,
                   s.reorder_level,
                   s.last_updated
            FROM stock_items s
                     JOIN products p ON p.product_id = s.product_id
            WHERE p.quantity <= s.reorder_level
            ORDER BY (p.quantity - s.reorder_level), p.product_name
            """;

    /** Best sellers in a month, by units sold. */
    private static final String SQL_TOP_PRODUCTS = """
            SELECT p.product_id,
                   p.product_name,
                   p.category,
                   SUM(oi.quantity)                  AS units_sold,
                   SUM(oi.quantity * oi.unit_price)  AS revenue
            FROM order_items oi
                     JOIN orders o   ON o.order_id = oi.order_id
                     JOIN products p ON p.product_id = oi.product_id
            WHERE o.status <> 'CANCELLED'
              AND YEAR(o.order_date) = ?
              AND MONTH(o.order_date) = ?
            GROUP BY p.product_id, p.product_name, p.category
            ORDER BY units_sold DESC, revenue DESC
            LIMIT ?
            """;

    /** Deliveries per agent in a month, counted by assignment date. */
    private static final String SQL_DELIVERIES_PER_AGENT = """
            SELECT a.agent_id,
                   a.name                                                     AS agent_name,
                   COUNT(*)                                                   AS assigned,
                   SUM(CASE WHEN d.status = 'DELIVERED' THEN 1 ELSE 0 END)    AS delivered,
                   SUM(CASE WHEN d.status = 'FAILED'    THEN 1 ELSE 0 END)    AS failed
            FROM delivery_assignments d
                     JOIN delivery_agents a ON a.agent_id = d.agent_id
            WHERE YEAR(d.assigned_at) = ?
              AND MONTH(d.assigned_at) = ?
            GROUP BY a.agent_id, a.name
            ORDER BY assigned DESC, agent_name
            """;

    // -----------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------

    @Override
    public MonthlySalesSummary getMonthlySalesSummary(YearMonth month) throws SQLException {
        MonthlySalesSummary summary = new MonthlySalesSummary();
        summary.setMonth(month);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_MONTHLY_SUMMARY)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.setOrderCount(rs.getInt("order_count"));
                    summary.setUnitsSold(rs.getInt("units_sold"));
                    summary.setTotalRevenue(money(rs.getBigDecimal("total_revenue")));
                }
            }
        }
        return summary;
    }

    @Override
    public List<LowStockAlert> findLowStockItems() throws SQLException {
        List<LowStockAlert> alerts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOW_STOCK);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("eco_rating"),
                        rs.getString("description"),
                        rs.getInt("quantity"));

                StockItem stockItem = new StockItem(
                        product,
                        rs.getInt("quantity"),
                        rs.getInt("reorder_level"));

                alerts.add(new LowStockAlert(stockItem, toDateTime(rs.getTimestamp("last_updated"))));
            }
        }
        return alerts;
    }

    @Override
    public List<TopSellingProduct> findTopSellingProducts(YearMonth month, int limit)
            throws SQLException {
        List<TopSellingProduct> rows = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOP_PRODUCTS)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());
            ps.setInt(3, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TopSellingProduct(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category"),
                            rs.getInt("units_sold"),
                            money(rs.getBigDecimal("revenue"))));
                }
            }
        }
        return rows;
    }

    @Override
    public List<AgentDeliveryCount> findDeliveriesPerAgent(YearMonth month) throws SQLException {
        List<AgentDeliveryCount> rows = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELIVERIES_PER_AGENT)) {

            ps.setInt(1, month.getYear());
            ps.setInt(2, month.getMonthValue());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AgentDeliveryCount(
                            rs.getInt("agent_id"),
                            rs.getString("agent_name"),
                            rs.getInt("assigned"),
                            rs.getInt("delivered"),
                            rs.getInt("failed")));
                }
            }
        }
        return rows;
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /** Never hand a null BigDecimal upwards; an empty month means zero. */
    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** SQL timestamps are nullable; map them without a NullPointerException. */
    private static LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
