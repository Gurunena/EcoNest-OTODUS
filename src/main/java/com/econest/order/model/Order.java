package econest.model;

import java.time.LocalDate;

public class Order {

    private int orderId;
    private int clientId;
    private int productId;
    private int quantity;
    private LocalDate orderDate;
    private double totalAmount;
    private String orderStatus;
    private int deliveryAgentId;


    // =========================
    // CONSTRUCTOR
    // =========================

    public Order(int orderId,
                 int clientId,
                 int productId,
                 int quantity,
                 LocalDate orderDate,
                 double totalAmount,
                 String orderStatus,
                 int deliveryAgentId) {

        this.orderId = orderId;
        this.clientId = clientId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.deliveryAgentId = deliveryAgentId;
    }


    // =========================
    // GETTERS
    // =========================

    public int getOrderId() {
        return orderId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public int getDeliveryAgentId() {
        return deliveryAgentId;
    }


    // =========================
    // SETTERS
    // =========================

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void setDeliveryAgentId(int deliveryAgentId) {
        this.deliveryAgentId = deliveryAgentId;
    }
}
