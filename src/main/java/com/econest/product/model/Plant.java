
package com.econest.product.model;

public class Plant extends Product {

    public Plant(int productId, String productName,
                 double price, int ecoRating,
                 String description, int quantity) {

        super(productId, productName, "Plant",
                price, ecoRating, description, quantity);
    }
}
