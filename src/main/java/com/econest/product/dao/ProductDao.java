package econest.dao;

import econest.db.DBConnection;
import econest.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    // ADD PRODUCT
    public boolean addProduct(Product product) {

        String sql = "INSERT INTO products " +
                "(product_id, product_name, category, price, eco_rating, description, quantity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, product.getProductId());
            statement.setString(2, product.getProductName());
            statement.setString(3, product.getCategory());
            statement.setDouble(4, product.getPrice());
            statement.setInt(5, product.getEcoRating());
            statement.setString(6, product.getDescription());
            statement.setInt(7, product.getQuantity());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // GET ALL PRODUCTS
    public List<Product> getAllProducts() {

        List<Product> products = new ArrayList<>();

        String sql = "SELECT * FROM products";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Product product = new Product(
                        resultSet.getInt("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("category"),
                        resultSet.getDouble("price"),
                        resultSet.getInt("eco_rating"),
                        resultSet.getString("description"),
                        resultSet.getInt("quantity")
                );

                products.add(product);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }

    // GET PRODUCT BY ID
    public Product getProductById(int productId) {

        String sql =
                "SELECT * FROM products WHERE product_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {

                return new Product(
                        resultSet.getInt("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("category"),
                        resultSet.getDouble("price"),
                        resultSet.getInt("eco_rating"),
                        resultSet.getString("description"),
                        resultSet.getInt("quantity")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE PRODUCT
    public boolean updateProduct(Product product) {

        String sql =
                "UPDATE products SET " +
                        "product_name=?, category=?, price=?, " +
                        "eco_rating=?, description=?, quantity=? " +
                        "WHERE product_id=?";

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, product.getProductName());
            statement.setString(2, product.getCategory());
            statement.setDouble(3, product.getPrice());
            statement.setInt(4, product.getEcoRating());
            statement.setString(5, product.getDescription());
            statement.setInt(6, product.getQuantity());
            statement.setInt(7, product.getProductId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // DELETE PRODUCT
    public boolean deleteProduct(int productId) {

        String sql =
                "DELETE FROM products WHERE product_id=?";

        try (Connection connection =
                     DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
