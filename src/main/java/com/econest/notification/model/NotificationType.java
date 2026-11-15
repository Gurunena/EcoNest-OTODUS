package com.econest.notification.model;

/**
 * The kinds of email EcoNest sends.
 *
 * The constant names match the notifications.notification_type column in
 * the database exactly, so a value can be saved with name() and read back
 * with valueOf().
 */
public enum NotificationType {

    ORDER_CONFIRMATION("Order confirmation"),
    ORDER_STATUS_CHANGE("Order dispatched"),     // Task 8: client emails
    DELIVERY_ASSIGNED("Delivery assigned"),      // Task 9: agent emails
    DELIVERY_COMPLETED("Delivery completed"),
    LOW_STOCK_ALERT("Low stock alert");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    /** Friendly text for the screen, e.g. "Delivery assigned". */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
