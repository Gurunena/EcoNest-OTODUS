package com.econest.agent.model;

/** Whether an agent can be given a new delivery (used by Task 6, Assign Delivery Agents). */
public enum AgentStatus {

    AVAILABLE("Available"),
    ON_DELIVERY("On delivery"),
    OFF_DUTY("Off duty");

    private final String label;

    AgentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Turns the text from the database ("Available") back into the enum. */
    public static AgentStatus fromLabel(String label) {
        for (AgentStatus status : values()) {
            if (status.label.equalsIgnoreCase(label)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown agent status: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
