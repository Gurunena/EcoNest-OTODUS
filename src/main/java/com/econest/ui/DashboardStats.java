package com.econest.ui;

/**
 * Supplies the four numbers on the {@link DashboardPanel} cards.
 *
 * <h2>Why every number is "-" right now</h2>
 * <p>Each module declares a DAO <i>interface</i> but none of them has an
 * implementation yet, so there is nothing for this class to call. Every
 * method therefore returns {@link #UNAVAILABLE} and carries a TODO naming
 * the owner who should wire it up.</p>
 *
 * <h2>How to wire your number up</h2>
 * <p>Find your method, drop your service call inside the {@code try} block
 * and return a formatted value. That is the only change needed - the
 * Dashboard reads whatever you return.</p>
 *
 * <pre>
 *   public String lowStockItems() {
 *       try {
 *           InventoryService service = new InventoryService(new InventoryDaoImpl());
 *           return String.valueOf(service.countLowStock());
 *       } catch (Exception ex) {
 *           return unavailable("low stock", ex);
 *       }
 *   }
 * </pre>
 *
 * <h2>The one rule</h2>
 * <p>A broken or missing DAO must never take the Dashboard down. Every
 * method catches {@link Exception} and falls back to "-". Keep it that way:
 * the Dashboard is the first thing the user sees, and a database that is not
 * running is a normal state during development.</p>
 */
public class DashboardStats {

    /** Shown on a card when the real number cannot be worked out. */
    public static final String UNAVAILABLE = "-";

    /**
     * How many stock items have dropped to or below their reorder level.
     *
     * TODO (Inventory owner): call InventoryService once an InventoryDao
     * implementation exists. The interface is
     * com.econest.inventory.dao.InventoryDao.
     */
    public String lowStockItems() {
        try {
            // No InventoryDao implementation exists yet, so there is nothing
            // to query. Replace this block with the service call.
            return UNAVAILABLE;
        } catch (Exception ex) {
            return unavailable("low stock items", ex);
        }
    }

    /**
     * How many orders are still waiting to be processed.
     *
     * TODO (Orders owner): call OrderService once com.econest.order.dao.OrderDao
     * compiles and has an implementation.
     */
    public String pendingOrders() {
        try {
            return UNAVAILABLE;
        } catch (Exception ex) {
            return unavailable("pending orders", ex);
        }
    }

    /**
     * How many deliveries are scheduled for today.
     *
     * TODO (Delivery Assignment owner): call DeliveryAssignmentService once a
     * com.econest.delivery.dao.DeliveryAssignmentDao implementation exists.
     */
    public String deliveriesToday() {
        try {
            return UNAVAILABLE;
        } catch (Exception ex) {
            return unavailable("deliveries today", ex);
        }
    }

    /**
     * Total sales value for the current month, formatted for display.
     *
     * TODO (Reports owner): call ReportService.getMonthlySalesSummary once a
     * com.econest.report.dao.ReportDao implementation exists.
     */
    public String revenueThisMonth() {
        try {
            return UNAVAILABLE;
        } catch (Exception ex) {
            return unavailable("monthly revenue", ex);
        }
    }

    /**
     * Logs why a number could not be produced and returns the placeholder.
     * Kept in one place so every card fails the same quiet way.
     *
     * @param what a short label for the number, used in the log line
     * @param ex   what went wrong
     * @return {@link #UNAVAILABLE}, always
     */
    protected String unavailable(String what, Exception ex) {
        System.err.println("Dashboard: could not load " + what + " (" + ex + ")");
        return UNAVAILABLE;
    }

    /**
     * Formats an amount as the app shows money. Use it from
     * {@link #revenueThisMonth()} once real figures arrive.
     */
    protected static String money(double amount) {
        return String.format("Rs. %,.0f", amount);
    }
}
