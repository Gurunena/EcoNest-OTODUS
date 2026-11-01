
package com.econest.agent.dao;

import com.econest.agent.model.AgentStatus;
import com.econest.agent.model.DeliveryAgent;
import com.econest.agent.model.Vehicle;
import com.econest.agent.model.VehicleType;
import com.econest.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeliveryAgentDao {

    private static final String SELECT =
            "SELECT a.agent_id, a.name, a.nic, a.phone, a.email, a.status, " +
                    "v.registration_number, v.vehicle_type, v.capacity_kg " +
                    "FROM delivery_agents a " +
                    "LEFT JOIN vehicles v ON v.vehicle_id = a.vehicle_id ";

    // ADD AGENT
    public void addAgent(DeliveryAgent agent) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Integer vehicleId = insertVehicle(conn, agent.getVehicle());

                String sql = "INSERT INTO delivery_agents " +
                        "(name, nic, licence_number, phone, email, vehicle_id, status, available) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, agent.getName());
                    ps.setString(2, agent.getNic());
                    ps.setString(3, agent.getNic());
                    ps.setString(4, agent.getPhone());
                    ps.setString(5, agent.getEmail());
                    ps.setObject(6, vehicleId);
                    ps.setString(7, agent.getStatus().name());
                    ps.setBoolean(8, agent.getStatus() == AgentStatus.AVAILABLE);

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            agent.setAgentId(keys.getInt(1));
                        }
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // UPDATE AGENT
    public boolean updateAgent(DeliveryAgent agent) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (!exists(conn, agent.getAgentId())) {
                    conn.rollback();
                    return false;
                }

                Integer oldVehicleId = findVehicleId(conn, agent.getAgentId());
                Integer newVehicleId;

                if (oldVehicleId == null) {
                    newVehicleId = insertVehicle(conn, agent.getVehicle());
                } else {
                    updateVehicle(conn, oldVehicleId, agent.getVehicle());
                    newVehicleId = oldVehicleId;
                }

                String sql = "UPDATE delivery_agents SET " +
                        "name=?, nic=?, licence_number=?, phone=?, email=?, " +
                        "vehicle_id=?, status=?, available=? WHERE agent_id=?";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setString(1, agent.getName());
                    ps.setString(2, agent.getNic());
                    ps.setString(3, agent.getNic());
                    ps.setString(4, agent.getPhone());
                    ps.setString(5, agent.getEmail());
                    ps.setObject(6, newVehicleId);
                    ps.setString(7, agent.getStatus().name());
                    ps.setBoolean(8, agent.getStatus() == AgentStatus.AVAILABLE);
                    ps.setInt(9, agent.getAgentId());

                    ps.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // DELETE AGENT
    public boolean deleteAgent(int agentId) throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Integer vehicleId = findVehicleId(conn, agentId);

                int rows;
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM delivery_agents WHERE agent_id=?")) {

                    ps.setInt(1, agentId);
                    rows = ps.executeUpdate();
                }

                deleteVehicleRow(conn, vehicleId);

                conn.commit();
                return rows > 0;

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // GET ALL AGENTS
    public List<DeliveryAgent> getAllAgents() throws SQLException {
        return query(SELECT + "ORDER BY a.agent_id", null);
    }

    // SEARCH AGENTS
    public List<DeliveryAgent> searchAgents(String keyword) throws SQLException {

        String sql = SELECT +
                "WHERE a.name LIKE ? OR a.nic LIKE ? " +
                "OR v.registration_number LIKE ? ORDER BY a.agent_id";

        return query(sql, "%" + keyword + "%");
    }

    // GET AVAILABLE AGENTS
    public List<DeliveryAgent> getAvailableAgents() throws SQLException {
        return query(
                SELECT + "WHERE a.status='AVAILABLE' ORDER BY a.name",
                null
        );
    }

    // GET AGENT BY ID
    public DeliveryAgent getAgentById(int agentId) throws SQLException {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     SELECT + "WHERE a.agent_id=?")) {

            ps.setInt(1, agentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // COMMON QUERY
    private List<DeliveryAgent> query(String sql, String keyword)
            throws SQLException {

        List<DeliveryAgent> agents = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (keyword != null) {
                ps.setString(1, keyword);
                ps.setString(2, keyword);
                ps.setString(3, keyword);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    agents.add(map(rs));
                }
            }
        }

        return agents;
    }

    // CONVERT DATABASE ROW TO JAVA OBJECT
    private DeliveryAgent map(ResultSet rs) throws SQLException {

        Vehicle vehicle = null;
        String registration = rs.getString("registration_number");

        if (registration != null) {
            String dbType = rs.getString("vehicle_type");

            VehicleType type;
            if ("TRUCK".equalsIgnoreCase(dbType)) {
                type = VehicleType.LORRY;
            } else {
                type = VehicleType.valueOf(dbType);
            }

            vehicle = new Vehicle(
                    registration,
                    type,
                    (int) Math.round(rs.getDouble("capacity_kg"))
            );
        }

        return new DeliveryAgent(
                rs.getInt("agent_id"),
                rs.getString("name"),
                rs.getString("nic"),
                rs.getString("phone"),
                rs.getString("email"),
                vehicle,
                AgentStatus.valueOf(rs.getString("status"))
        );
    }

    // INSERT VEHICLE
    private Integer insertVehicle(Connection conn, Vehicle vehicle)
            throws SQLException {

        if (vehicle == null) {
            return null;
        }

        String sql = "INSERT INTO vehicles " +
                "(registration_number, vehicle_type, capacity_kg) " +
                "VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, vehicle.getRegistrationNumber());

            String dbType = vehicle.getType() == VehicleType.LORRY
                    ? "TRUCK"
                    : vehicle.getType().name();

            ps.setString(2, dbType);
            ps.setInt(3, vehicle.getMaxLoadKg());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        }
    }

    // UPDATE VEHICLE
    private void updateVehicle(Connection conn, int vehicleId, Vehicle vehicle)
            throws SQLException {

        String sql = "UPDATE vehicles SET registration_number=?, " +
                "vehicle_type=?, capacity_kg=? WHERE vehicle_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vehicle.getRegistrationNumber());

            String dbType = vehicle.getType() == VehicleType.LORRY
                    ? "TRUCK"
                    : vehicle.getType().name();

            ps.setString(2, dbType);
            ps.setInt(3, vehicle.getMaxLoadKg());
            ps.setInt(4, vehicleId);

            ps.executeUpdate();
        }
    }

    // DELETE VEHICLE
    private void deleteVehicleRow(Connection conn, Integer vehicleId)
            throws SQLException {

        if (vehicleId == null) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM vehicles WHERE vehicle_id=?")) {

            ps.setInt(1, vehicleId);
            ps.executeUpdate();
        }
    }

    // FIND VEHICLE ID
    private Integer findVehicleId(Connection conn, int agentId)
            throws SQLException {

        String sql = "SELECT vehicle_id FROM delivery_agents WHERE agent_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, agentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return rs.wasNull() ? null : id;
                }
            }
        }

        return null;
    }

    // CHECK AGENT EXISTS
    private boolean exists(Connection conn, int agentId)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM delivery_agents WHERE agent_id=?")) {

            ps.setInt(1, agentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
