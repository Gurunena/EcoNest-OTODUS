package com.econest.delivery.service;

import com.econest.delivery.dao.DeliveryAssignmentDao;
import com.econest.delivery.dao.DeliveryAssignmentDaoImpl;
import com.econest.delivery.model.AvailableAgent;
import com.econest.delivery.model.DeliveryAssignment;
import com.econest.delivery.model.PendingOrder;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for the Delivery Assignment module.
 *
 * <p>Checks the things the screen must not be trusted with - that an order
 * and an agent were actually chosen, and that an order is not assigned twice
 * - then hands off to {@link DeliveryAssignmentDao}, which does the
 * multi-table work in a transaction.</p>
 *
 * <p>Validation failures are {@link IllegalArgumentException} with messages
 * written to be shown to the user unchanged.</p>
 */
public class DeliveryAssignmentService {

    private final DeliveryAssignmentDao dao;

    /** Production constructor: talks to MySQL. */
    public DeliveryAssignmentService() {
        this(new DeliveryAssignmentDaoImpl());
    }

    /** Injectable constructor, so a test can pass a fake DAO. */
    public DeliveryAssignmentService(DeliveryAssignmentDao dao) {
        this.dao = dao;
    }

    // -----------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------

    /** Orders waiting for an agent. */
    public List<PendingOrder> findUnassignedOrders() throws SQLException {
        return dao.findUnassignedOrders();
    }

    /** Agents free to take a delivery right now. */
    public List<AvailableAgent> findAvailableAgents() throws SQLException {
        return dao.findAvailableAgents();
    }

    /** Every assignment, newest first. */
    public List<DeliveryAssignment> findAllAssignments() throws SQLException {
        return dao.findAllAssignments();
    }

    // -----------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------

    /**
     * Assigns an order to an agent. The order moves to the dispatched status
     * and the agent is marked busy.
     */
    public void assign(int orderId, int agentId) throws SQLException {
        requireOrder(orderId);
        requireAgent(agentId);
        dao.assign(orderId, agentId);
    }

    /** Moves an order to a different agent, freeing the previous one. */
    public void reassign(int orderId, int newAgentId) throws SQLException {
        requireOrder(orderId);
        requireAgent(newAgentId);
        dao.reassign(orderId, newAgentId);
    }

    /** Takes the agent off an order and puts the order back in the queue. */
    public void unassign(int orderId) throws SQLException {
        requireOrder(orderId);
        dao.unassign(orderId);
    }

    // -----------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------

    private static void requireOrder(int orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("Select an order first.");
        }
    }

    private static void requireAgent(int agentId) {
        if (agentId <= 0) {
            throw new IllegalArgumentException(
                    "Choose a delivery agent. If the list is empty, every agent is "
                            + "currently busy.");
        }
    }
}
