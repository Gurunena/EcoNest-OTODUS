package com.econest.delivery.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An order that is waiting for a delivery agent: status PENDING or
 * CONFIRMED and no row yet in {@code delivery_assignments}.
 *
 * <p>A read-only view built for the assignment screen, so it carries the
 * client name rather than a client id.</p>
 */
public class PendingOrder {

    private int orderId;
    private String clientName;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String status;
    private String deliveryAddress;

    public PendingOrder() {
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    /** The order's status as stored, e.g. "PENDING" or "CONFIRMED". */
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}
