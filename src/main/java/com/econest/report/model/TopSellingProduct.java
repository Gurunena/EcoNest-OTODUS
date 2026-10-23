package com.econest.report.model;

import java.math.BigDecimal;

/**
 * One row of the "top selling products" report: how much of a product was
 * sold in the chosen month and what it earned.
 */
public class TopSellingProduct {

    private int productId;
    private String productName;
    private String category;
    private int unitsSold;
    private BigDecimal revenue = BigDecimal.ZERO;

    public TopSellingProduct() {
    }

    public TopSellingProduct(int productId, String productName, String category,
                             int unitsSold, BigDecimal revenue) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.unitsSold = unitsSold;
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getUnitsSold() { return unitsSold; }
    public void setUnitsSold(int unitsSold) { this.unitsSold = unitsSold; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
