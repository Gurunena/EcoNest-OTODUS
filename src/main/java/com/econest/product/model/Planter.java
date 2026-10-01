package com.econest.product.model;

import java.math.BigDecimal;

public class Planter extends Product {

    // TODO: planter-specific fields (material, size, ...)

    public Planter() {
    }

    public Planter(String name, BigDecimal price, EcoRating ecoRating) {
        super(name, price, ecoRating);
    }

    @Override
    public String getCategory() {
        return "Planter";
    }
}
