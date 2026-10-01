package com.econest.product;

import com.econest.product.model.EcoRating;
import com.econest.product.model.Plant;
import com.econest.product.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductModelTest {

    @Test
    void plantIsAProduct() {
        Product p = new Plant("Snake Plant", new BigDecimal("2500.00"), EcoRating.A);

        assertEquals("Plant", p.getCategory());
        assertEquals("Snake Plant", p.getName());
        assertEquals(EcoRating.A, p.getEcoRating());
    }
}
