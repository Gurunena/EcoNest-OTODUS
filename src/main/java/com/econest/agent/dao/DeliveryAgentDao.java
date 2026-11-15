
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
            "SELECT a.agent_id, a.name, a.licence_number, " +
                    "a.phone, a.email, a.available, " +
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
                        "(name, licence_number, phone, email, vehicle_id, available) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, agent.getName());
                    ps.setString(2, agent.getNic());
                    ps.setString(3, agent.getPhone());
                    ps.setString(4, agent.getEmail());

                    if (vehicleId == null) {
                        ps.setNull(5, Types.INTEGER);
                    } else {
                        ps.setInt(5, vehicleId);
                    }

                    ps.setBoolean(6,
                            agent.getStatus() == AgentStatus.AVAILABLE);

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

                Integer oldVehicleId =
                        findVehicleId(conn, agent.getAgentId());

                Integer newVehicleId;

                if (agent.getVehicle() == null) {
                    newVehicleId = null;
                } else if (oldVehicleId == null) {
                    newVehicleId = insertVehicle(conn, agent.getVehicle());
                } else {
                    updateVehicle(conn, oldVehicleId, agent.getVehicle());
                    newVehicleId = oldVehicleId;
                }

                String sql = "UPDATE delivery_agents SET " +
                        "name=?, licence_number=?, phone=?, email=?, " +
                        "vehicle_id=?, available=? WHERE agent_id=?";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setString(1, agent.getName());
                    ps.setString(2, agent.getNic());
                    ps.setString(3, agent.getPhone());
                    ps.setString(4, agent.getEmail());

                    if (newVehicleId == null) {
                        ps.setNull(5, Types.INTEGER);
                    } else {
                        ps.setInt(5, newVehicleId);
                    }

                    ps.setBoolean(6,
                            agent.getStatus() == AgentStatus.AVAILABLE);

                    ps.setInt(7, agent.getAgentId());

                    ps.executeUpdate();
                }

                // Remove old vehicle when agent no longer has one
                if (agent.getVehicle() == null && oldVehicleId != null) {
                    deleteVehicleRow(conn, oldVehicleId);
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

                if (rows > 0) {
                    deleteVehicleRow(conn, vehicleId);
                }

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
    public List<DeliveryAgent> searchAgents(String keyword)
            throws SQLException {

        String sql = SELECT +
                "WHERE a.name LIKE ? " +
                "OR a.licence_number LIKE ? " +
                "OR v.registration_number LIKE ? " +
                "ORDER BY a.agent_id";

        return query(sql, "%" + keyword + "%");
    }

    // GET AVAILABLE AGENTS
    public List<DeliveryAgent> getAvailableAgents() throws SQLException {
        return query(
                SELECT + "WHERE a.available = TRUE ORDER BY a.name",
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
                try {
                    type = VehicleType.valueOf(dbType);
                } catch (IllegalArgumentException e) {
                    throw new SQLException(
                            "Unsupported vehicle type: " + dbType, e);
                }
            }

            vehicle = new Vehicle(
                    registration,
                    type,
                    (int) Math.round(rs.getDouble("capacity_kg"))
            );
        }

        AgentStatus status = rs.getBoolean("available")
                ? AgentStatus.AVAILABLE
                : AgentStatus.OFF_DUTY;

        return new DeliveryAgent(
                rs.getInt("agent_id"),
                rs.getString("name"),
                rs.getString("licence_number"),
                rs.getString("phone"),
                rs.getString("email"),
                vehicle,
                status
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

        String sql = "UPDATE vehicles SET " +
                "registration_number=?, vehicle_type=?, " +
                "capacity_kg=? WHERE vehicle_id=?";

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

        String sql =
                "SELECT vehicle_id FROM delivery_agents WHERE agent_id=?";

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
