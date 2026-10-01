package com.econest.product.model;

import java.math.BigDecimal;

public class Plant extends Product {

    // TODO: plant-specific fields (species, light needs, indoor/outdoor, ...)

    public Plant() {
    }

    public Plant(String name, BigDecimal price, EcoRating ecoRating) {
        super(name, price, ecoRating);
    }

    @Override
    public String getCategory() {
        return "Plant";
    }
}
