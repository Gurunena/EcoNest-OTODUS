package com.econest.notification.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task 8 - everything the "your order is on its way" email needs to say about one order:
 * what was ordered, the total, where it's going and who is delivering it.
 */
public class DispatchedOrder {

    private final int orderId;
    private final int clientId;
    private final String status;              // e.g. "DISPATCHED"
    private final LocalDateTime orderDate;
    private final BigDecimal totalAmount;
    private final String deliveryAddress;     // may be null - then the client's own address is used
    private final String agentName;           // may be null if no agent is assigned yet
    private final String agentPhone;
    private final List<String> items = new ArrayList<>();   // e.g. "Peace Lily x 2"

    public DispatchedOrder(int orderId, int clientId, String status, LocalDateTime orderDate,
                           BigDecimal totalAmount, String deliveryAddress,
                           String agentName, String agentPhone) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.status = status;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.deliveryAddress = deliveryAddress;
        this.agentName = agentName;
        this.agentPhone = agentPhone;
    }

    public void addItem(String productName, int quantity) {
        items.add(productName + " x " + quantity);
    }

    public int getOrderId() {
        return orderId;
    }

    public int getClientId() {
        return clientId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentPhone() {
        return agentPhone;
    }

    public List<String> getItems() {
        return items;
    }
}
