package com.econest.report.model;

/**
 * One row of the "deliveries per agent" report: how many deliveries an agent
 * was given in the chosen month, and how they turned out.
 */
public class AgentDeliveryCount {

    private int agentId;
    private String agentName;
    private int assigned;
    private int delivered;
    private int failed;

    public AgentDeliveryCount() {
    }

    public AgentDeliveryCount(int agentId, String agentName,
                              int assigned, int delivered, int failed) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.assigned = assigned;
        this.delivered = delivered;
        this.failed = failed;
    }

    public int getAgentId() { return agentId; }
    public void setAgentId(int agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    /** Total assignments given to the agent in the month. */
    public int getAssigned() { return assigned; }
    public void setAssigned(int assigned) { this.assigned = assigned; }

    public int getDelivered() { return delivered; }
    public void setDelivered(int delivered) { this.delivered = delivered; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    /** Delivered as a percentage of assigned; 0 when nothing was assigned. */
    public double getSuccessRate() {
        return assigned == 0 ? 0.0 : (delivered * 100.0) / assigned;
    }
}
