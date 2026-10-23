package com.econest.report.dao;

import com.econest.report.model.AgentDeliveryCount;
import com.econest.report.model.LowStockAlert;
import com.econest.report.model.MonthlySalesSummary;
import com.econest.report.model.TopSellingProduct;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

/**
 * Read-only aggregate queries backing the reports screen.
 *
 * <p>Every method throws {@link SQLException} rather than swallowing it: the
 * service layer decides what the user is told, and the panel never sees a
 * stack trace.</p>
 */
public interface ReportDao {

    /**
     * Order count, units sold and revenue for one month. Cancelled orders are
     * excluded.
     *
     * @param month the month to summarise
     * @return the summary; zeroed (never null) when the month had no orders
     */
    MonthlySalesSummary getMonthlySalesSummary(YearMonth month) throws SQLException;

    /** Products currently at or below their reorder level. */
    List<LowStockAlert> findLowStockItems() throws SQLException;

    /**
     * Best-selling products in a month, highest units first.
     *
     * @param month the month to look at
     * @param limit how many rows to return at most
     */
    List<TopSellingProduct> findTopSellingProducts(YearMonth month, int limit) throws SQLException;

    /** Deliveries assigned to each agent in a month, busiest agent first. */
    List<AgentDeliveryCount> findDeliveriesPerAgent(YearMonth month) throws SQLException;
}
