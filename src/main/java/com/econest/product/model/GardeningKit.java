
package com.econest.product.model;

public class GardeningKit extends Product {

    public GardeningKit(int productId, String productName,
                        double price, int ecoRating,
                        String description, int quantity) {

        super(productId, productName, "Gardening Kit",
                price, ecoRating, description, quantity);
    }
}
