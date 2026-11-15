package com.econest.ui;

import java.util.List;

/**
 * The one place that says which cards the Dashboard shows.
 *
 * <p><b>ONE LINE PER MODULE</b>, in sidebar order, each separated by a blank
 * line so two people editing different modules never collide in the same
 * diff hunk - the same arrangement as
 * {@code MainFrame.registerPages()}.</p>
 *
 * <p>To plug your card in: replace the {@code StatCard.placeholder(...)} on
 * <b>your</b> line with your own card. Do not reorder or reformat anyone
 * else's line, and do not touch {@link DashboardPanel} - it reads this list
 * and knows nothing else about any card.</p>
 *
 * @see DashboardCard
 * @see StatCard
 */
public final class DashboardCards {

    private DashboardCards() {
        // Registry only: there is nothing to instantiate.
    }

    /** The cards, in the order they appear in the grid. */
    public static List<DashboardCard> register() {
        return register(new DashboardStats());
    }

    /**
     * The same list against a given stats source, so a test can show fixed
     * figures without a database.
     *
     * @param stats supplies the numbers for the cards that have a source
     */
    public static List<DashboardCard> register(DashboardStats stats) {
        return List.of(

                // Products module
                StatCard.placeholder("Products", "products",
                        Theme.TINT_SUCCESS, Theme.STATUS_SUCCESS),

                // Clients module
                StatCard.placeholder("Clients", "clients",
                        Theme.TINT_INFO, Theme.STATUS_INFO),

                // Stock / Inventory module
                new StatCard("Low stock items", "inventory",
                        Theme.TINT_DANGER, Theme.STATUS_DANGER, stats::lowStockItems),

                // Delivery Agents module
                StatCard.placeholder("Delivery agents", "agents",
                        Theme.TINT_INFO, Theme.STATUS_INFO),

                // Orders module
                new StatCard("Pending orders", "orders",
                        Theme.TINT_PENDING, Theme.STATUS_PENDING, stats::pendingOrders),

                // Delivery Assignment module
                new StatCard("Deliveries today", "assignment",
                        Theme.TINT_SUCCESS, Theme.STATUS_SUCCESS, stats::deliveriesToday),

                // Reports module
                new StatCard("Revenue this month", "revenue",
                        Theme.TINT_INFO, Theme.STATUS_INFO, stats::revenueThisMonth),

                // Notifications module
                new StatCard("Emails sent", "notifications", Theme.TINT_PENDING, Theme.STATUS_PENDING,
                        () -> String.valueOf(new com.econest.notification.dao.NotificationDaoImpl().countSent())));
    }
}
