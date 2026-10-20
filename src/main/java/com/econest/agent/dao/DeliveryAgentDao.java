package com.econest.agent.dao;

import com.econest.agent.model.AgentStatus;
import com.econest.agent.model.DeliveryAgent;
import com.econest.agent.model.Vehicle;
import com.econest.agent.model.VehicleType;
import com.econest.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access for delivery_agents and their vehicles (Task 4).
 * An agent and its vehicle are always written together in one transaction.
 */
public class DeliveryAgentDao {

    private static final String SELECT =
            "SELECT a.agent_id, a.name, a.nic, a.phone, a.email, a.status, "
                    + "v.registration_number, v.vehicle_type, v.capacity_kg "
                    + "FROM delivery_agents a LEFT JOIN vehicles v ON v.vehicle_id = a.vehicle_id ";

    public void addAgent(DeliveryAgent agent) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer vehicleId = insertVehicle(conn, agent.getVehicle());

                String sql = "INSERT INTO delivery_agents (name, nic, phone, email, vehicle_id, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, agent.getName());
                    ps.setString(2, agent.getNic());
                    ps.setString(3, agent.getPhone());
                    ps.setString(4, agent.getEmail());
                    ps.setObject(5, vehicleId);
                    ps.setString(6, agent.getStatus().name());
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

    /** @return true if the agent existed and was updated */
    public boolean updateAgent(DeliveryAgent agent) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!exists(conn, agent.getAgentId())) {
                    conn.rollback();
                    return false;
                }

                Integer oldVehicleId = findVehicleId(conn, agent.getAgentId());
                Integer newVehicleId = null;
                if (agent.getVehicle() != null) {
                    if (oldVehicleId == null) {
                        newVehicleId = insertVehicle(conn, agent.getVehicle());
                    } else {
                        updateVehicle(conn, oldVehicleId, agent.getVehicle());
                        newVehicleId = oldVehicleId;
                    }
                }

                String sql = "UPDATE delivery_agents SET name=?, nic=?, phone=?, email=?, vehicle_id=?, status=? "
                        + "WHERE agent_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, agent.getName());
                    ps.setString(2, agent.getNic());
                    ps.setString(3, agent.getPhone());
                    ps.setString(4, agent.getEmail());
                    ps.setObject(5, newVehicleId);
                    ps.setString(6, agent.getStatus().name());
                    ps.setInt(7, agent.getAgentId());
                    ps.executeUpdate();
                }
                if (agent.getVehicle() == null) {
                    deleteVehicleRow(conn, oldVehicleId);   // agent no longer has a vehicle
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Removes the agent and their vehicle.
     *
     * @return true if the agent existed
     * @throws SQLException e.g. when the agent already has delivery assignments
     */
    public boolean deleteAgent(int agentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer vehicleId = findVehicleId(conn, agentId);
                int rows;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM delivery_agents WHERE agent_id=?")) {
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

    public List<DeliveryAgent> getAllAgents() throws SQLException {
        return query(SELECT + "ORDER BY a.agent_id", null);
    }

    /** Search by name, NIC or vehicle registration number. */
    public List<DeliveryAgent> searchAgents(String keyword) throws SQLException {
        return query(SELECT + "WHERE a.name LIKE ? OR a.nic LIKE ? OR v.registration_number LIKE ? "
                + "ORDER BY a.agent_id", "%" + keyword + "%");
    }

    /** Agents that can take a new delivery (used by Task 6). */
    public List<DeliveryAgent> getAvailableAgents() throws SQLException {
        return query(SELECT + "WHERE a.status = 'AVAILABLE' ORDER BY a.name", null);
    }

    public DeliveryAgent getAgentById(int agentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE a.agent_id = ?")) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ------------------------------------------------------------ helpers

    private List<DeliveryAgent> query(String sql, String keyword) throws SQLException {
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

    private DeliveryAgent map(ResultSet rs) throws SQLException {
        Vehicle vehicle = null;
        String reg = rs.getString("registration_number");
        if (reg != null) {
            vehicle = new Vehicle(reg, VehicleType.valueOf(rs.getString("vehicle_type")),
                    (int) Math.round(rs.getDouble("capacity_kg")));
        }
        return new DeliveryAgent(rs.getInt("agent_id"), rs.getString("name"), rs.getString("nic"),
                rs.getString("phone"), rs.getString("email"), vehicle,
                AgentStatus.valueOf(rs.getString("status")));
    }

    private Integer insertVehicle(Connection conn, Vehicle v) throws SQLException {
        if (v == null) {
            return null;
        }
        String sql = "INSERT INTO vehicles (registration_number, vehicle_type, capacity_kg) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getRegistrationNumber());
            ps.setString(2, v.getType().name());
            ps.setInt(3, v.getMaxLoadKg());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : null;
            }
        }
    }

    private void updateVehicle(Connection conn, int vehicleId, Vehicle v) throws SQLException {
        String sql = "UPDATE vehicles SET registration_number=?, vehicle_type=?, capacity_kg=? WHERE vehicle_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getRegistrationNumber());
            ps.setString(2, v.getType().name());
            ps.setInt(3, v.getMaxLoadKg());
            ps.setInt(4, vehicleId);
            ps.executeUpdate();
        }
    }

    private void deleteVehicleRow(Connection conn, Integer vehicleId) throws SQLException {
        if (vehicleId == null) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM vehicles WHERE vehicle_id=?")) {
            ps.setInt(1, vehicleId);
            ps.executeUpdate();
        }
    }

    private Integer findVehicleId(Connection conn, int agentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT vehicle_id FROM delivery_agents WHERE agent_id=?")) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return rs.wasNull() ? null : id;
                }
                return null;
            }
        }
    }

    private boolean exists(Connection conn, int agentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM delivery_agents WHERE agent_id=?")) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
