package com.econest.report.service;

import com.econest.report.dao.ReportDao;
import com.econest.report.dao.ReportDaoImpl;
import com.econest.report.model.AgentDeliveryCount;
import com.econest.report.model.LowStockAlert;
import com.econest.report.model.MonthlySalesSummary;
import com.econest.report.model.TopSellingProduct;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Business logic for the Reports module.
 *
 * <p>Sits between {@code ReportPanel} and {@link ReportDao}: it validates the
 * month, applies the report rules (how many "top" products count as top) and
 * turns a whole month into a CSV file. The panel never touches JDBC.</p>
 */
public class ReportService {

    /** How many products the "top selling" table shows. */
    public static final int TOP_PRODUCT_LIMIT = 10;

    /** Earliest month the UI will accept, as a sanity check on typed input. */
    private static final int MIN_YEAR = 2000;

    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final ReportDao dao;

    /** Production constructor: talks to MySQL. */
    public ReportService() {
        this(new ReportDaoImpl());
    }

    /** Injectable constructor, so a test can pass a fake DAO. */
    public ReportService(ReportDao dao) {
        this.dao = dao;
    }

    // -----------------------------------------------------------------
    // Reports
    // -----------------------------------------------------------------

    public MonthlySalesSummary getMonthlySummary(YearMonth month) throws SQLException {
        return dao.getMonthlySalesSummary(requireValid(month));
    }

    public List<TopSellingProduct> getTopSellingProducts(YearMonth month) throws SQLException {
        return dao.findTopSellingProducts(requireValid(month), TOP_PRODUCT_LIMIT);
    }

    public List<AgentDeliveryCount> getDeliveriesPerAgent(YearMonth month) throws SQLException {
        return dao.findDeliveriesPerAgent(requireValid(month));
    }

    /** Low stock is a snapshot of right now, so it takes no month. */
    public List<LowStockAlert> getLowStockItems() throws SQLException {
        return dao.findLowStockItems();
    }

    /**
     * Rejects a month the reports cannot sensibly cover.
     *
     * @throws IllegalArgumentException with a message fit to show the user
     */
    public YearMonth requireValid(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Choose a month and a year.");
        }
        if (month.getYear() < MIN_YEAR) {
            throw new IllegalArgumentException("Choose a year from " + MIN_YEAR + " onwards.");
        }
        if (month.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException(
                    "That month is in the future. Choose " + label(YearMonth.now()) + " or earlier.");
        }
        return month;
    }

    /** "October 2026", for headings and the CSV. */
    public static String label(YearMonth month) {
        return month == null ? "" : month.format(MONTH_LABEL);
    }

    // -----------------------------------------------------------------
    // CSV export
    // -----------------------------------------------------------------

    /**
     * Writes the whole month - summary, top products, deliveries per agent
     * and low stock - into one CSV file, each report under its own heading.
     *
     * @param month  the month being exported
     * @param target where to write; an existing file is overwritten
     * @throws SQLException if a report could not be read
     * @throws IOException  if the file could not be written
     */
    public void exportMonthToCsv(YearMonth month, Path target) throws SQLException, IOException {
        requireValid(month);

        MonthlySalesSummary summary = getMonthlySummary(month);
        List<TopSellingProduct> topProducts = getTopSellingProducts(month);
        List<AgentDeliveryCount> agents = getDeliveriesPerAgent(month);
        List<LowStockAlert> lowStock = getLowStockItems();

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
             PrintWriter out = new PrintWriter(writer)) {

            out.println("EcoNest monthly report," + csv(label(month)));
            out.println();

            out.println("Summary");
            out.println("Metric,Value");
            out.println("Total orders," + summary.getOrderCount());
            out.println("Units sold," + summary.getUnitsSold());
            out.println("Total revenue," + plain(summary.getTotalRevenue()));
            out.println();

            out.println("Top selling products");
            out.println("Product ID,Product,Category,Units sold,Revenue");
            for (TopSellingProduct row : topProducts) {
                out.println(String.join(",",
                        String.valueOf(row.getProductId()),
                        csv(row.getProductName()),
                        csv(row.getCategory()),
                        String.valueOf(row.getUnitsSold()),
                        plain(row.getRevenue())));
            }
            out.println();

            out.println("Deliveries per agent");
            out.println("Agent ID,Agent,Assigned,Delivered,Failed,Success rate %");
            for (AgentDeliveryCount row : agents) {
                out.println(String.join(",",
                        String.valueOf(row.getAgentId()),
                        csv(row.getAgentName()),
                        String.valueOf(row.getAssigned()),
                        String.valueOf(row.getDelivered()),
                        String.valueOf(row.getFailed()),
                        String.format(Locale.ROOT, "%.1f", row.getSuccessRate())));
            }
            out.println();

            out.println("Low stock items (as at export time)");
            out.println("Product ID,Product,Quantity on hand,Reorder level");
            for (LowStockAlert alert : lowStock) {
                out.println(String.join(",",
                        String.valueOf(alert.getStockItem().getProduct().getProductId()),
                        csv(alert.getStockItem().getProduct().getProductName()),
                        String.valueOf(alert.getStockItem().getQuantityOnHand()),
                        String.valueOf(alert.getStockItem().getReorderLevel())));
            }
        }
    }

    /**
     * Quotes a value for CSV: wraps it in quotes and doubles any quote
     * inside, so a product name with a comma cannot break the column layout.
     */
    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    /** Money without a thousands separator, so spreadsheets read it as a number. */
    private static String plain(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).toPlainString();
    }
}
