
package com.econest.delivery.service;

import com.econest.agent.dao.DeliveryAgentDao;
import com.econest.agent.model.DeliveryAgent;
import com.econest.delivery.dao.DeliveryAssignmentDao;
import com.econest.delivery.dao.DeliveryAssignmentDaoImpl;
import com.econest.delivery.model.AvailableAgent;
import com.econest.delivery.model.DeliveryAssignment;
import com.econest.delivery.model.PendingOrder;
import com.econest.notification.service.NotificationService;

import java.sql.SQLException;
import java.util.List;

public class DeliveryAssignmentService {

    private final DeliveryAssignmentDao dao;
    private final DeliveryAgentDao agentDao;
    private final NotificationService notificationService;

    /** Production constructor. */
    public DeliveryAssignmentService() {
        this(new DeliveryAssignmentDaoImpl(), new DeliveryAgentDao());
    }

    /** Injectable constructor for existing tests. */
    public DeliveryAssignmentService(DeliveryAssignmentDao dao) {
        this(dao, new DeliveryAgentDao());
    }

    public DeliveryAssignmentService(
            DeliveryAssignmentDao dao,
            DeliveryAgentDao agentDao) {
        this.dao = dao;
        this.agentDao = agentDao;
        this.notificationService = new NotificationService();
    }

    // Reads

    public List<PendingOrder> findUnassignedOrders() throws SQLException {
        return dao.findUnassignedOrders();
    }

    public List<AvailableAgent> findAvailableAgents() throws SQLException {
        return dao.findAvailableAgents();
    }

    public List<DeliveryAssignment> findAllAssignments() throws SQLException {
        return dao.findAllAssignments();
    }

    // Writes

    public void assign(int orderId, int agentId) throws SQLException {
        requireOrder(orderId);
        requireAgent(agentId);

        dao.assign(orderId, agentId);

        // Task 9: notify the newly assigned delivery agent.
        notifyAgent(orderId, agentId);
    }

    public void reassign(int orderId, int newAgentId) throws SQLException {
        requireOrder(orderId);
        requireAgent(newAgentId);

        dao.reassign(orderId, newAgentId);

        // Notify the new agent after reassignment succeeds.
        notifyAgent(orderId, newAgentId);
    }

    public void unassign(int orderId) throws SQLException {
        requireOrder(orderId);
        dao.unassign(orderId);
    }

    // Task 9 email notification


    private void notifyAgent(int orderId, int agentId) {
        try {
            String details = "Order ID: " + orderId
                    + "\nDelivery Agent ID: " + agentId;

            notificationService.notifyDeliveryAgent(
                    agentId, details
            );

        } catch (Exception ex) {
            // Do not undo an assignment if email sending fails.
            System.err.println(
                    "Assignment saved, but notification failed: "
                            + ex.getMessage()
            );
        }
    }


    // Validation

    private static void requireOrder(int orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("Select an order first.");
        }
    }

    private static void requireAgent(int agentId) {
        if (agentId <= 0) {
            throw new IllegalArgumentException(
                    "Choose a delivery agent. If the list is empty, every agent is "
                            + "currently busy."
            );
        }
    }
}
