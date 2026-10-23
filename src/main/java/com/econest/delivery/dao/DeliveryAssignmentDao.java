package com.econest.delivery.dao;

import com.econest.delivery.model.AvailableAgent;
import com.econest.delivery.model.DeliveryAssignment;
import com.econest.delivery.model.PendingOrder;

import java.sql.SQLException;
import java.util.List;

/**
 * Persistence operations for delivery assignments.
 *
 * <p>Assigning touches three tables at once - {@code delivery_assignments},
 * {@code orders.status} and {@code delivery_agents.available} - so the three
 * write methods are transactional: all of it happens, or none of it.</p>
 */
public interface DeliveryAssignmentDao {

    /** Orders that are PENDING or CONFIRMED and have no agent yet. */
    List<PendingOrder> findUnassignedOrders() throws SQLException;

    /** Agents currently marked available. */
    List<AvailableAgent> findAvailableAgents() throws SQLException;

    /** Every agent, available or not - needed to show a busy agent by name. */
    List<AvailableAgent> findAllAgents() throws SQLException;

    /** All current assignments, newest first. */
    List<DeliveryAssignment> findAllAssignments() throws SQLException;

    /**
     * Assigns an order to an agent: writes the assignment, moves the order to
     * the dispatched status and marks the agent busy.
     *
     * @throws SQLException if the order already has an agent, or the agent is
     *                      no longer available
     */
    void assign(int orderId, int agentId) throws SQLException;

    /**
     * Moves an existing assignment to a different agent, freeing the old one
     * and marking the new one busy.
     */
    void reassign(int orderId, int newAgentId) throws SQLException;

    /**
     * Removes the assignment, frees the agent and puts the order back to
     * CONFIRMED so it shows up as unassigned again.
     */
    void unassign(int orderId) throws SQLException;
}
