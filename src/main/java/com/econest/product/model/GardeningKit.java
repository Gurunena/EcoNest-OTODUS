package com.econest.product.model;

import java.math.BigDecimal;

public class GardeningKit extends Product {

    // TODO: kit-specific fields (contents, skill level, ...)

    public GardeningKit() {
    }

    public GardeningKit(String name, BigDecimal price, EcoRating ecoRating) {
        super(name, price, ecoRating);
    }

    @Override
    public String getCategory() {
        return "Gardening Kit";
    }
}
