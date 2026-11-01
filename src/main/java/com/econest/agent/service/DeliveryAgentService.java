
package com.econest.agent.service;

import com.econest.agent.dao.DeliveryAgentDao;
import com.econest.agent.model.DeliveryAgent;
import com.econest.util.Validator;

import java.sql.SQLException;
import java.util.List;

public class DeliveryAgentService {

    private final DeliveryAgentDao dao = new DeliveryAgentDao();

    // ADD AGENT
    public void addAgent(DeliveryAgent agent) throws SQLException {
        validate(agent);
        dao.addAgent(agent);
    }

    // UPDATE AGENT
    public boolean updateAgent(DeliveryAgent agent) throws SQLException {
        validate(agent);
        return dao.updateAgent(agent);
    }

    // DELETE AGENT
    public boolean deleteAgent(int agentId) throws SQLException {
        return dao.deleteAgent(agentId);
    }

    // VIEW ALL AGENTS
    public List<DeliveryAgent> getAllAgents() throws SQLException {
        return dao.getAllAgents();
    }

    // SEARCH AGENTS
    public List<DeliveryAgent> search(String keyword) throws SQLException {
        return Validator.isBlank(keyword)
                ? dao.getAllAgents()
                : dao.searchAgents(keyword.trim());
    }

    // GET AVAILABLE AGENTS
    public List<DeliveryAgent> getAvailableAgents() throws SQLException {
        return dao.getAvailableAgents();
    }

    // VALIDATION
    public static void validate(DeliveryAgent a) {

        if (a == null) {
            throw new IllegalArgumentException("Agent details are required.");
        }

        if (Validator.isBlank(a.getName())) {
            throw new IllegalArgumentException("Agent name is required.");
        }

        if (!Validator.isValidNic(a.getNic())) {
            throw new IllegalArgumentException(
                    "NIC must be 9 digits + V/X, or 12 digits.");
        }

        if (!Validator.isValidPhone(a.getPhone())) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number.");
        }

        if (!Validator.isValidEmail(a.getEmail())) {
            throw new IllegalArgumentException(
                    "Enter a valid email address.");
        }

        if (a.getStatus() == null) {
            throw new IllegalArgumentException("Select a status.");
        }

        if (a.getVehicle() == null ||
                Validator.isBlank(a.getVehicle().getRegistrationNumber())) {
            throw new IllegalArgumentException(
                    "Vehicle registration number is required.");
        }

        if (a.getVehicle().getType() == null) {
            throw new IllegalArgumentException("Select a vehicle type.");
        }

        if (a.getVehicle().getMaxLoadKg() <= 0) {
            throw new IllegalArgumentException(
                    "Maximum load must be greater than 0 kg.");
        }
    }
}
