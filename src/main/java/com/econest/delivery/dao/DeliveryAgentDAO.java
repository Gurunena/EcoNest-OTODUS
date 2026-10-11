package econest.dao;

import econest.db.DBConnection;
import econest.model.DeliveryAgent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DeliveryAgentDAO {

    // ADD
    public boolean addAgent(DeliveryAgent agent) {

        String sql = "INSERT INTO delivery_agents " +
                "(agent_name, phone, email, vehicle_number, vehicle_type) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, agent.getAgentName());
            statement.setString(2, agent.getPhone());
            statement.setString(3, agent.getEmail());
            statement.setString(4, agent.getVehicleNumber());
            statement.setString(5, agent.getVehicleType());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // GET ALL
    public List<DeliveryAgent> getAllAgents() {

        List<DeliveryAgent> agents = new ArrayList<>();

        String sql = "SELECT * FROM delivery_agents";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                DeliveryAgent agent =
                        new DeliveryAgent(
                                resultSet.getInt("agent_id"),
                                resultSet.getString("agent_name"),
                                resultSet.getString("phone"),
                                resultSet.getString("email"),
                                resultSet.getString("vehicle_number"),
                                resultSet.getString("vehicle_type")
                        );

                agents.add(agent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return agents;
    }


    // GET BY ID
    public DeliveryAgent getAgentById(int agentId) {

        String sql =
                "SELECT * FROM delivery_agents WHERE agent_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, agentId);

            ResultSet resultSet =
                    statement.executeQuery();

            if (resultSet.next()) {

                return new DeliveryAgent(
                        resultSet.getInt("agent_id"),
                        resultSet.getString("agent_name"),
                        resultSet.getString("phone"),
                        resultSet.getString("email"),
                        resultSet.getString("vehicle_number"),
                        resultSet.getString("vehicle_type")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    // UPDATE
    public boolean updateAgent(DeliveryAgent agent) {

        String sql =
                "UPDATE delivery_agents SET " +
                        "agent_name=?, phone=?, email=?, " +
                        "vehicle_number=?, vehicle_type=? " +
                        "WHERE agent_id=?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, agent.getAgentName());
            statement.setString(2, agent.getPhone());
            statement.setString(3, agent.getEmail());
            statement.setString(4, agent.getVehicleNumber());
            statement.setString(5, agent.getVehicleType());
            statement.setInt(6, agent.getAgentId());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // DELETE
    public boolean deleteAgent(int agentId) {

        String sql =
                "DELETE FROM delivery_agents WHERE agent_id=?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, agentId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}