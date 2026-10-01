package com.econest.notification.service;

import com.econest.agent.model.DeliveryAgent;
import com.econest.delivery.model.DeliveryAssignment;
import com.econest.order.model.Order;

/** Sends outbound emails. SMTP settings come from AppConfig (mail.*). */
public class EmailService {

    /** Notify the client that their order has been dispatched. */
    public void notifyClientOfDispatch(Order order) {
        // TODO: build and send ORDER_DISPATCHED email
    }

    /** Notify the delivery agent that an order has been assigned to them. */
    public void notifyAgentOfAssignment(DeliveryAgent agent, DeliveryAssignment assignment) {
        // TODO: build and send AGENT_ASSIGNED email
    }
}
