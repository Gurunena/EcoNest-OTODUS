package com.econest.notification.model;

public enum NotificationType {
    /** Sent to the client when their order is dispatched. */
    ORDER_DISPATCHED,
    /** Sent to a delivery agent when an order is assigned to them. */
    AGENT_ASSIGNED
}
