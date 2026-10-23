package com.econest.ui;

import com.econest.delivery.model.DeliveryAssignment;
import com.econest.delivery.model.PendingOrder;
import com.econest.delivery.service.DeliveryAssignmentService;
import com.econest.report.model.LowStockAlert;
import com.econest.report.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Supplies the numbers and the two list cards on the {@link DashboardPanel}.
 *
 * <p>Everything goes through the existing service layer - this class never
 * opens a connection or writes SQL of its own.</p>
 *
 * <h2>When something is unavailable</h2>
 * <p>A failure here must never take the Dashboard down, so every method
 * catches {@link Exception} and falls back to a zero or an empty list. A
 * database that is not running is a normal state during development.</p>
 */
public class DashboardStats {

    /**
     * Shown when a number cannot be worked out. Zero rather than a dash, so
     * a card never looks broken.
     */
    public static final String UNAVAILABLE = "0";

    private final ReportService reportService;
    private final DeliveryAssignmentService deliveryService;

    /** Production constructor: the real services. */
    public DashboardStats() {
        this(new ReportService(), new DeliveryAssignmentService());
    }

    /** Injectable constructor, so a test can pass fakes. */
    public DashboardStats(ReportService reportService,
                          DeliveryAssignmentService deliveryService) {
        this.reportService = reportService;
        this.deliveryService = deliveryService;
    }

    // -----------------------------------------------------------------
    // The four card numbers
    // -----------------------------------------------------------------

    /** Products at or below their reorder level. */
    public String lowStockItems() {
        try {
            return String.valueOf(reportService.getLowStockItems().size());
        } catch (Exception ex) {
            return unavailable("low stock items", ex);
        }
    }

    /**
     * Orders still waiting to be dealt with.
     *
     * <p>Counted as orders with status PENDING that have no delivery agent,
     * because that is what the delivery service exposes. A dedicated order
     * service would be able to count every open order; this is the honest
     * approximation available today.</p>
     */
    public String pendingOrders() {
        try {
            long count = deliveryService.findUnassignedOrders().stream()
                    .filter(order -> "PENDING".equalsIgnoreCase(order.getStatus()))
                    .count();
            return String.valueOf(count);
        } catch (Exception ex) {
            return unavailable("pending orders", ex);
        }
    }

    /** Deliveries assigned today. */
    public String deliveriesToday() {
        try {
            LocalDate today = LocalDate.now();
            long count = deliveryService.findAllAssignments().stream()
                    .map(DeliveryAssignment::getAssignedAt)
                    .filter(when -> when != null && when.toLocalDate().equals(today))
                    .count();
            return String.valueOf(count);
        } catch (Exception ex) {
            return unavailable("deliveries today", ex);
        }
    }

    /** Total sales value for the current month, formatted for display. */
    public String revenueThisMonth() {
        try {
            BigDecimal revenue = reportService
                    .getMonthlySummary(YearMonth.now())
                    .getTotalRevenue();
            return money(revenue);
        } catch (Exception ex) {
            return unavailable("monthly revenue", ex);
        }
    }

    // -----------------------------------------------------------------
    // The two list cards
    // -----------------------------------------------------------------

    /**
     * The products most in need of restocking, worst first.
     *
     * @param limit how many rows the card shows
     */
    public List<LowStockAlert> topLowStock(int limit) {
        try {
            List<LowStockAlert> alerts = reportService.getLowStockItems();
            return alerts.size() <= limit ? alerts : alerts.subList(0, limit);
        } catch (Exception ex) {
            unavailable("the low stock list", ex);
            return List.of();
        }
    }

    /**
     * The most recent orders, newest first.
     *
     * <p>Assembled from the two sources the service layer exposes: orders
     * still waiting for an agent (dated by when they were placed) and orders
     * that have one (dated by when they were assigned). An order that was
     * delivered or cancelled without ever being assigned will not appear -
     * showing those needs an order service, which does not exist yet.</p>
     *
     * @param limit how many rows the card shows
     */
    public List<OrderSummary> recentOrders(int limit) {
        List<OrderSummary> orders = new ArrayList<>();
        try {
            for (PendingOrder order : deliveryService.findUnassignedOrders()) {
                orders.add(new OrderSummary(
                        order.getOrderId(),
                        order.getClientName(),
                        order.getOrderDate(),
                        order.getTotalAmount(),
                        titleCase(order.getStatus())));
            }
        } catch (Exception ex) {
            unavailable("waiting orders", ex);
        }
        try {
            for (DeliveryAssignment assignment : deliveryService.findAllAssignments()) {
                orders.add(new OrderSummary(
                        assignment.getOrderId(),
                        assignment.getClientName(),
                        assignment.getAssignedAt(),
                        assignment.getOrderTotal(),
                        titleCase(assignment.getStatus().name())));
            }
        } catch (Exception ex) {
            unavailable("assigned orders", ex);
        }

        orders.sort(Comparator.comparing(
                OrderSummary::when,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return orders.size() <= limit ? orders : orders.subList(0, limit);
    }

    /** One row of the "Recent orders" card. */
    public record OrderSummary(int orderId, String client, LocalDateTime when,
                               BigDecimal total, String status) {
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /**
     * Logs why a number could not be produced and returns the placeholder.
     * Kept in one place so every card fails the same quiet way.
     */
    protected String unavailable(String what, Exception ex) {
        System.err.println("Dashboard: could not load " + what + " (" + ex + ")");
        return UNAVAILABLE;
    }

    /** Formats an amount as the app shows money. */
    protected static String money(double amount) {
        return String.format(Locale.ENGLISH, "Rs. %,.0f", amount);
    }

    /** Formats an amount as the app shows money. */
    protected static String money(BigDecimal amount) {
        return money(amount == null ? 0d : amount.doubleValue());
    }

    /** "IN_TRANSIT" -> "In transit", which is what the status pills expect. */
    private static String titleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String spaced = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
