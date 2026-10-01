package com.econest.product.model;

import java.math.BigDecimal;

/** Base type for everything EcoNest sells. */
public abstract class Product {

    private int id;
    private String name;
    private BigDecimal price;
    private EcoRating ecoRating;

    protected Product() {
    }

    protected Product(String name, BigDecimal price, EcoRating ecoRating) {
        this.name = name;
        this.price = price;
        this.ecoRating = ecoRating;
    }

    /** Human-readable category, e.g. "Plant". */
    public abstract String getCategory();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public EcoRating getEcoRating() { return ecoRating; }
    public void setEcoRating(EcoRating ecoRating) { this.ecoRating = ecoRating; }

    @Override
    public String toString() {
        return name;
    }
}
