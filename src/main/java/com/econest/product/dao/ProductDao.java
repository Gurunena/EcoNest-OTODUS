package com.econest.product.dao;

import com.econest.product.model.Product;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Database access for the products table (Task 1: Manage Product Catalogue). */
public class ProductDao {

    private static final String COLUMNS =
            "product_id, product_name, category, price, eco_rating, description, quantity";

    /** Inserts a product. The database assigns the id, which is copied back into the object. */
    public void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products "
                + "(product_name, category, price, eco_rating, description, quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getEcoRating());
            ps.setString(5, product.getDescription());
            ps.setInt(6, product.getQuantity());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setProductId(keys.getInt(1));
                }
            }
        }
    }

    public List<Product> getAllProducts() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM products ORDER BY product_id", null);
    }

    /** Case-insensitive search on product name or category. */
    public List<Product> searchProducts(String keyword) throws SQLException {
        return query("SELECT " + COLUMNS + " FROM products "
                        + "WHERE product_name LIKE ? OR category LIKE ? ORDER BY product_id",
                "%" + keyword + "%");
    }

    public Product getProductById(int productId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** @return true if a row was updated */
    public boolean updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET product_name=?, category=?, price=?, "
                + "eco_rating=?, description=?, quantity=? WHERE product_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getEcoRating());
            ps.setString(5, product.getDescription());
            ps.setInt(6, product.getQuantity());
            ps.setInt(7, product.getProductId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * @return true if a row was deleted
     * @throws SQLException e.g. when the product is already used by an order or stock-in record
     */
    public boolean deleteProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE product_id=?")) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    private List<Product> query(String sql, String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (keyword != null) {
                ps.setString(1, keyword);
                ps.setString(2, keyword);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    products.add(map(rs));
                }
            }
        }
        return products;
    }

    private Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("eco_rating"),
                rs.getString("description"),
                rs.getInt("quantity"));
    }
}
