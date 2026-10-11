package econest.dao;

import econest.db.DBConnection;
import econest.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // =========================
    // ADD ORDER
    // =========================

    public boolean addOrder(Order order) {

        String sql =
                "INSERT INTO orders " +
                        "(client_id, product_id, quantity, order_date, " +
                        "total_amount, order_status, delivery_agent_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection =
                     DBConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    order.getClientId()
            );

            statement.setInt(
                    2,
                    order.getProductId()
            );

            statement.setInt(
                    3,
                    order.getQuantity()
            );

            statement.setDate(
                    4,
                    java.sql.Date.valueOf(
                            order.getOrderDate()
                    )
            );

            statement.setDouble(
                    5,
                    order.getTotalAmount()
            );

            statement.setString(
                    6,
                    order.getOrderStatus()
            );

            if (order.getDeliveryAgentId() == 0) {

                statement.setNull(
                        7,
                        java.sql.Types.INTEGER
                );

            } else {

                statement.setInt(
                        7,
                        order.getDeliveryAgentId()
                );
            }

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }


    // =========================
    // GET ALL ORDERS
    // =========================

    public List<Order> getAllOrders() {

        List<Order> orders =
                new ArrayList<>();

        String sql =
                "SELECT * FROM orders";

        try (Connection connection =
                     DBConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet resultSet =
                     statement.executeQuery()) {


            while (resultSet.next()) {

                int deliveryAgentId =
                        resultSet.getInt(
                                "delivery_agent_id"
                        );

                if (resultSet.wasNull()) {
                    deliveryAgentId = 0;
                }


                Order order =
                        new Order(

                                resultSet.getInt(
                                        "order_id"
                                ),

                                resultSet.getInt(
                                        "client_id"
                                ),

                                resultSet.getInt(
                                        "product_id"
                                ),

                                resultSet.getInt(
                                        "quantity"
                                ),

                                resultSet.getDate(
                                        "order_date"
                                ).toLocalDate(),

                                resultSet.getDouble(
                                        "total_amount"
                                ),

                                resultSet.getString(
                                        "order_status"
                                ),

                                deliveryAgentId
                        );


                orders.add(order);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return orders;
    }


    // =========================
    // GET ORDER BY ID
    // =========================

    public Order getOrderById(int orderId) {

        String sql =
                "SELECT * FROM orders " +
                        "WHERE order_id = ?";


        try (Connection connection =
                     DBConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {


            statement.setInt(
                    1,
                    orderId
            );


            ResultSet resultSet =
                    statement.executeQuery();


            if (resultSet.next()) {

                int deliveryAgentId =
                        resultSet.getInt(
                                "delivery_agent_id"
                        );

                if (resultSet.wasNull()) {
                    deliveryAgentId = 0;
                }


                return new Order(

                        resultSet.getInt(
                                "order_id"
                        ),

                        resultSet.getInt(
                                "client_id"
                        ),

                        resultSet.getInt(
                                "product_id"
                        ),

                        resultSet.getInt(
                                "quantity"
                        ),

                        resultSet.getDate(
                                "order_date"
                        ).toLocalDate(),

                        resultSet.getDouble(
                                "total_amount"
                        ),

                        resultSet.getString(
                                "order_status"
                        ),

                        deliveryAgentId
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }


        return null;
    }


    // =========================
    // UPDATE ORDER
    // =========================

    public boolean updateOrder(Order order) {

        String sql =
                "UPDATE orders SET " +
                        "client_id=?, " +
                        "product_id=?, " +
                        "quantity=?, " +
                        "order_date=?, " +
                        "total_amount=?, " +
                        "order_status=?, " +
                        "delivery_agent_id=? " +
                        "WHERE order_id=?";


        try (Connection connection =
                     DBConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {


            statement.setInt(
                    1,
                    order.getClientId()
            );

            statement.setInt(
                    2,
                    order.getProductId()
            );

            statement.setInt(
                    3,
                    order.getQuantity()
            );

            statement.setDate(
                    4,
                    java.sql.Date.valueOf(
                            order.getOrderDate()
                    )
            );

            statement.setDouble(
                    5,
                    order.getTotalAmount()
            );

            statement.setString(
                    6,
                    order.getOrderStatus()
            );


            if (order.getDeliveryAgentId() == 0) {

                statement.setNull(
                        7,
                        java.sql.Types.INTEGER
                );

            } else {

                statement.setInt(
                        7,
                        order.getDeliveryAgentId()
                );
            }


            statement.setInt(
                    8,
                    order.getOrderId()
            );


            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }


    // =========================
    // DELETE ORDER
    // =========================

    public boolean deleteOrder(int orderId) {

        String sql =
                "DELETE FROM orders " +
                        "WHERE order_id = ?";


        try (Connection connection =
                     DBConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {


            statement.setInt(
                    1,
                    orderId
            );


            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}
