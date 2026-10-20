package com.econest.product.service;

import com.econest.product.dao.ProductDao;
import com.econest.product.model.Product;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** Business rules for the product catalogue; the UI talks to this, not to the DAO. */
public class ProductService {

    /** Values stored in products.category (see database/schema.sql). */
    public static final String[] CATEGORIES = {"PLANT", "PLANTER", "GARDENING_KIT"};

    private static final Set<String> CATEGORY_SET = Set.of(CATEGORIES);

    private final ProductDao dao = new ProductDao();

    public void addProduct(Product product) throws SQLException {
        validate(product);
        dao.addProduct(product);
    }

    public boolean updateProduct(Product product) throws SQLException {
        validate(product);
        return dao.updateProduct(product);
    }

    public boolean deleteProduct(int productId) throws SQLException {
        return dao.deleteProduct(productId);
    }

    public List<Product> getAllProducts() throws SQLException {
        return dao.getAllProducts();
    }

    public List<Product> search(String keyword) throws SQLException {
        return keyword == null || keyword.isBlank() ? dao.getAllProducts() : dao.searchProducts(keyword.trim());
    }

    /** @throws IllegalArgumentException with a message that is safe to show the user */
    public static void validate(Product p) {
        if (p.getProductName() == null || p.getProductName().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (!CATEGORY_SET.contains(p.getCategory())) {
            throw new IllegalArgumentException("Category must be PLANT, PLANTER or GARDENING_KIT.");
        }
        if (p.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (p.getEcoRating() < 1 || p.getEcoRating() > 5) {
            throw new IllegalArgumentException("Eco rating must be between 1 and 5.");
        }
        if (p.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }
}
