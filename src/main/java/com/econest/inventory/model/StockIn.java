package com.econest.inventory.model;

import com.econest.product.model.Product;

import java.time.LocalDate;

/** A delivery of stock received from a supplier. */
public class StockIn {

    private int id;
    private Product product;
    private Supplier supplier;
    private int quantity;
    private LocalDate receivedDate;

    public StockIn() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
}
