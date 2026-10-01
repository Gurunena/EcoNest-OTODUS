package com.econest.order.model;

import com.econest.product.model.Product;

import java.math.BigDecimal;

/** One product line on an order. */
public class OrderItem {

    private Product product;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem() {
    }

    public OrderItem(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
