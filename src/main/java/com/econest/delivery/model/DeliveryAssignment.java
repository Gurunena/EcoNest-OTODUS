package com.econest.delivery.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Links an order to the agent delivering it - one row of
 * {@code delivery_assignments}.
 *
 * <h2>Why this holds ids, not objects</h2>
 * <p>It used to hold {@code order.model.Order} and
 * {@code agent.model.DeliveryAgent}. It no longer does, for two reasons:</p>
 * <ul>
 *   <li>{@code com.econest.order.model.Order} does not compile - the order
 *       module declares it {@code package econest.model} - so importing it
 *       broke this whole module.</li>
 *   <li>{@code agent.model.DeliveryAgent} carries {@code nic} and an
 *       {@code AgentStatus} that do not line up with the
 *       {@code delivery_agents} table, so mapping rows into it would mean
 *       guessing at another module's model.</li>
 * </ul>
 * <p>The screen only ever shows the order and agent, so the ids plus a few
 * display fields are enough. When the order module compiles, this can hold
 * a real {@code Order} again.</p>
 */
public class DeliveryAssignment {

    private int id;

    private int orderId;
    private String clientName;
    private BigDecimal orderTotal = BigDecimal.ZERO;
    private String deliveryAddress;

    private int agentId;
    private String agentName;

    private DeliveryStatus status = DeliveryStatus.ASSIGNED;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;

    public DeliveryAssignment() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public BigDecimal getOrderTotal() { return orderTotal; }
    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal == null ? BigDecimal.ZERO : orderTotal;
    }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public int getAgentId() { return agentId; }
    public void setAgentId(int agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    /** True while the delivery is still in progress. */
    public boolean isOpen() {
        return status != DeliveryStatus.DELIVERED && status != DeliveryStatus.FAILED;
    }
}
