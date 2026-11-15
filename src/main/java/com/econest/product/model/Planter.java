
package com.econest.product.model;

public class Planter extends Product {

    public Planter(int productId, String productName,
                   double price, int ecoRating,
                   String description, int quantity) {

        super(productId, productName, "Planter",
                price, ecoRating, description, quantity);
    }
}
