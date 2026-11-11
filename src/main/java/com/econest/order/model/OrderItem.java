package com.econest.order.model;

import java.math.BigDecimal;

/**
 * One product line on an order, matching the {@code order_items} table.
 *
 * <p>{@code unitPrice} is the price charged <b>at the time of the order</b>,
 * copied from {@code products.price} when the line is added. It is stored on
 * the row rather than looked up later, so re-pricing the catalogue never
 * rewrites the history of what a client was charged.</p>
 *
 * <p>{@code productName} is carried alongside the id purely so a table or a
 * printed summary can show the line without a second query. It is not
 * written back to the database.</p>
 */
public class OrderItem {

    private int orderItemId;          // 0 until the database gives it an id
    private int orderId;              // 0 while the order is still a draft
    private int productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice = Money.ZERO;

    public OrderItem() {
    }

    /** The usual way to build a draft line: product, how many, at what price. */
    public OrderItem(int productId, String productName, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        setUnitPrice(unitPrice);
    }

    /**
     * {@code unitPrice * quantity}, rounded to 2dp.
     *
     * <p>Derived, never stored: a line total kept in a field is one more
     * thing that can disagree with the two numbers it comes from.</p>
     */
    public BigDecimal getLineTotal() {
        return Money.multiply(unitPrice, quantity);
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** Always stored at the module's money scale, so totals add up exactly. */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = Money.scale(unitPrice);
    }

    @Override
    public String toString() {
        return quantity + " x " + productName + " @ " + Money.format(unitPrice)
                + " = " + Money.format(getLineTotal());
    }
}
