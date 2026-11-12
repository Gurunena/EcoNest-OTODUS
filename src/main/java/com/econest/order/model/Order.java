package com.econest.order.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A client order: one {@code orders} row plus its {@code order_items} lines.
 *
 * <p>This replaces the earlier flat version of this class, which had a single
 * {@code productId} / {@code quantity} pair and a {@code double} total. The
 * {@code orders} table is normalised - one header row, many item rows - so an
 * order HAS-A list of {@link OrderItem}, and money is {@link BigDecimal}.
 * {@code database/README.md} flagged that mismatch as an open item for this
 * module.</p>
 *
 * <p>{@code clientName} is filled in from a join when an order is read back,
 * so a table can show who it is for without a second query. It is never
 * written to the database.</p>
 */
public class Order {

    private int orderId;                       // 0 until the database gives it an id
    private int clientId;
    private String clientName;                 // read-only, from a join
    private LocalDateTime orderDate = LocalDateTime.now();
    private LocalDate deliveryDate;            // requested; null means not specified
    private OrderStatus status = OrderStatus.PENDING;
    private BigDecimal totalAmount = Money.ZERO;
    private String deliveryAddress;            // snapshot, may differ from the client's
    private String notes;

    private final List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    /**
     * The order number shown to people, derived from the primary key rather
     * than stored: {@code orders} has no order_no column, and a second
     * identifier that can drift out of step with the key earns nothing.
     * A draft with no id yet reads as "(draft)".
     */
    public String getOrderNo() {
        return orderId <= 0 ? "(draft)" : String.format("ORD-%05d", orderId);
    }

    /** The lines on this order. Add through {@link #addItem(OrderItem)}. */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItem item) {
        if (item != null) {
            items.add(item);
        }
    }

    /** Replaces every line in one go. */
    public void setItems(List<OrderItem> replacements) {
        items.clear();
        if (replacements != null) {
            replacements.stream().filter(java.util.Objects::nonNull).forEach(items::add);
        }
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    public void clearItems() {
        items.clear();
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    /** The delivery date the client asked for, or null if none was given. */
    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status == null ? OrderStatus.PENDING : status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = Money.scale(totalAmount);
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return getOrderNo() + " - " + clientName + " - " + Money.format(totalAmount);
    }
}
