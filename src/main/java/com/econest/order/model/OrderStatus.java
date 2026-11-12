package com.econest.order.model;

import java.util.Locale;
import java.util.Set;

/**
 * The life of an order, matching the {@code orders.status} ENUM in
 * {@code database/schema.sql} exactly.
 *
 * <p><b>On "out for delivery":</b> the schema's enum is
 * {@code ('PENDING','CONFIRMED','PACKED','DISPATCHED','DELIVERED','CANCELLED')}
 * and has no {@code OUT_FOR_DELIVERY} member. {@link #DISPATCHED} is that
 * state, which is also what the delivery module settled on - see
 * {@code DeliveryAssignmentDaoImpl.STATUS_OUT_FOR_DELIVERY}. Adding a seventh
 * enum member would mean an ALTER on a table three other modules read, so the
 * existing value is reused instead.</p>
 *
 * <p>{@link #label()} is what the UI shows. The words are chosen to match the
 * status palette in {@code TableStyler}, so each one paints the right pill
 * colour without the table having to know anything about orders.</p>
 */
public enum OrderStatus {

    /** Taken, not yet confirmed. The only status in which an order is editable. */
    PENDING("Pending"),

    /** Confirmed with the client; stock is already committed. */
    CONFIRMED("Confirmed"),

    /** Picked and boxed, waiting for an agent. */
    PACKED("Packed"),

    /** Out for delivery - an agent has it. */
    DISPATCHED("Dispatched"),

    /** Handed over to the client. The end of the happy path. */
    DELIVERED("Delivered"),

    /** Called off; the stock it held has been returned to the shelf. */
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** The human spelling shown in tables and dialogs, e.g. "Dispatched". */
    public String label() {
        return label;
    }

    /** Which statuses this one may legally move to. */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            // PENDING may skip straight to DISPATCHED because the delivery
            // module assigns an agent to PENDING and CONFIRMED orders alike.
            case PENDING -> Set.of(CONFIRMED, DISPATCHED, CANCELLED);
            case CONFIRMED -> Set.of(PACKED, DISPATCHED, CANCELLED);
            case PACKED -> Set.of(DISPATCHED, CANCELLED);
            case DISPATCHED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    /** True when {@code next} is a legal move from this status. */
    public boolean canMoveTo(OrderStatus next) {
        return next != null && allowedNext().contains(next);
    }

    /**
     * The natural next step along the happy path, or null at the end of it.
     * Cancelling is deliberately not part of this walk - it is a decision,
     * not a step forward.
     */
    public OrderStatus next() {
        return switch (this) {
            case PENDING -> CONFIRMED;
            case CONFIRMED -> PACKED;
            case PACKED -> DISPATCHED;
            case DISPATCHED -> DELIVERED;
            case DELIVERED, CANCELLED -> null;
        };
    }

    /** True once an agent has the order, so it can no longer be deleted. */
    public boolean isOutOfOurHands() {
        return this == DISPATCHED || this == DELIVERED;
    }

    /** True only for {@link #PENDING}: the one status in which items may change. */
    public boolean isEditable() {
        return this == PENDING;
    }

    /** True when the order still holds stock that a cancellation must give back. */
    public boolean holdsStock() {
        return this != CANCELLED;
    }

    /**
     * Reads a value stored in {@code orders.status}. Accepts the label
     * spelling too, and {@code OUT_FOR_DELIVERY} as an alias for
     * {@link #DISPATCHED}, so a caller from another module is never caught
     * out by the naming.
     *
     * @throws IllegalArgumentException with a message that is safe to show
     */
    public static OrderStatus of(String stored) {
        if (stored == null || stored.isBlank()) {
            throw new IllegalArgumentException("An order status is required.");
        }
        String key = stored.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (key.equals("OUT_FOR_DELIVERY")) {
            return DISPATCHED;
        }
        for (OrderStatus status : values()) {
            if (status.name().equals(key)) {
                return status;
            }
        }
        throw new IllegalArgumentException("\"" + stored + "\" is not an order status.");
    }

    @Override
    public String toString() {
        return label;
    }
}
