
package com.econest.notification.dao;

import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeliveryAgentEmailDao {

    public String findEmailByAgentId(int agentId) throws SQLException {

        String sql = "SELECT email FROM delivery_agents WHERE agent_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, agentId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getString("email");
                }
            }
        }

        return null;
    }
}
