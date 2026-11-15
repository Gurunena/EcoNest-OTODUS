
package com.econest.product.model;

public class EcoRating {

    private int rating;

    public EcoRating(int rating) {
        setRating(rating);
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Eco rating must be between 1 and 5"
            );
        }

        this.rating = rating;
    }
}
