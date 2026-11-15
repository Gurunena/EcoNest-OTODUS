
package com.econest.product;

import com.econest.product.model.Product;
import com.econest.product.model.Plant;
import com.econest.product.model.Planter;
import com.econest.product.model.GardeningKit;
import com.econest.product.model.EcoRating;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductModelTest {

    @Test
    void testProductCreation() {
        Product product = new Product(
                1, "Rose Plant", "Plant",
                500.00, 5, "Red Rose", 10
        );

        assertEquals(1, product.getProductId());
        assertEquals("Rose Plant", product.getProductName());
        assertEquals("Plant", product.getCategory());
        assertEquals(500.00, product.getPrice());
        assertEquals(5, product.getEcoRating());
        assertEquals("Red Rose", product.getDescription());
        assertEquals(10, product.getQuantity());
    }

    @Test
    void testProductSetters() {
        Product product = new Product(
                1, "Rose", "Plant",
                500.00, 4, "Flower", 10
        );

        product.setProductName("Jasmine");
        product.setPrice(600.00);
        product.setQuantity(20);

        assertEquals("Jasmine", product.getProductName());
        assertEquals(600.00, product.getPrice());
        assertEquals(20, product.getQuantity());
    }

    @Test
    void testPlantInheritance() {
        Plant plant = new Plant(
                2, "Rose", 500.00, 5, "Flower", 10
        );

        assertInstanceOf(Product.class, plant);
        assertEquals("Plant", plant.getCategory());
    }

    @Test
    void testPlanterInheritance() {
        Planter planter = new Planter(
                3, "Clay Pot", 800.00, 4, "Clay planter", 5
        );

        assertInstanceOf(Product.class, planter);
        assertEquals("Planter", planter.getCategory());
    }

    @Test
    void testGardeningKitInheritance() {
        GardeningKit kit = new GardeningKit(
                4, "Garden Kit", 1500.00, 5, "Basic kit", 3
        );

        assertInstanceOf(Product.class, kit);
        assertEquals("Gardening Kit", kit.getCategory());
    }

    @Test
    void testValidEcoRating() {
        EcoRating rating = new EcoRating(5);

        assertEquals(5, rating.getRating());
    }

    @Test
    void testInvalidEcoRating() {
        assertThrows(IllegalArgumentException.class,
                () -> new EcoRating(6));
    }
}
