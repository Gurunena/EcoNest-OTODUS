package com.econest.agent.service;

import com.econest.agent.dao.DeliveryAgentDao;
import com.econest.agent.model.DeliveryAgent;
import com.econest.util.Validator;

import java.sql.SQLException;
import java.util.List;

/** Business rules for delivery agents; the UI talks to this, not to the DAO. */
public class DeliveryAgentService {

    private final DeliveryAgentDao dao = new DeliveryAgentDao();

    public void addAgent(DeliveryAgent agent) throws SQLException {
        validate(agent);
        dao.addAgent(agent);
    }

    public boolean updateAgent(DeliveryAgent agent) throws SQLException {
        validate(agent);
        return dao.updateAgent(agent);
    }

    public boolean deleteAgent(int agentId) throws SQLException {
        return dao.deleteAgent(agentId);
    }

    public List<DeliveryAgent> getAllAgents() throws SQLException {
        return dao.getAllAgents();
    }

    public List<DeliveryAgent> search(String keyword) throws SQLException {
        return Validator.isBlank(keyword) ? dao.getAllAgents() : dao.searchAgents(keyword.trim());
    }

    /** For Task 6 (assigning agents to orders). */
    public List<DeliveryAgent> getAvailableAgents() throws SQLException {
        return dao.getAvailableAgents();
    }

    /** @throws IllegalArgumentException with a message that is safe to show the user */
    public static void validate(DeliveryAgent a) {
        if (Validator.isBlank(a.getName())) {
            throw new IllegalArgumentException("Agent name is required.");
        }
        if (!Validator.isValidNic(a.getNic())) {
            throw new IllegalArgumentException("NIC must be 9 digits + V/X, or 12 digits.");
        }
        if (!Validator.isValidPhone(a.getPhone())) {
            throw new IllegalArgumentException("Phone must be 10 digits starting with 0, e.g. 0712345678.");
        }
        if (!Validator.isValidEmail(a.getEmail())) {
            throw new IllegalArgumentException("Enter a valid email address (needed for delivery notifications).");
        }
        if (a.getStatus() == null) {
            throw new IllegalArgumentException("Select a status.");
        }
        if (a.getVehicle() == null || Validator.isBlank(a.getVehicle().getRegistrationNumber())) {
            throw new IllegalArgumentException("Vehicle registration number is required.");
        }
        if (a.getVehicle().getType() == null) {
            throw new IllegalArgumentException("Select a vehicle type.");
        }
        if (a.getVehicle().getMaxLoadKg() <= 0) {
            throw new IllegalArgumentException("Maximum load must be greater than 0 kg.");
        }
    }
}
