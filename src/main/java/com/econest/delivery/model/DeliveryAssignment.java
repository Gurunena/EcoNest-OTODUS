package com.econest.delivery.model;

import com.econest.agent.model.DeliveryAgent;
import com.econest.order.model.Order;

import java.time.LocalDateTime;

/** Links an order to the agent delivering it. */
public class DeliveryAssignment {

    private int id;
    private Order order;
    private DeliveryAgent agent;
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;

    public DeliveryAssignment() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public DeliveryAgent getAgent() { return agent; }
    public void setAgent(DeliveryAgent agent) { this.agent = agent; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
