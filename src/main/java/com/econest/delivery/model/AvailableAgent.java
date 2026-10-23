package com.econest.delivery.model;

/**
 * A delivery agent as the assignment combo box needs them: just enough to
 * pick one and show who it is.
 *
 * <p>Deliberately not {@code agent.model.DeliveryAgent}: that class models
 * the agent module's own view (NIC, vehicle, AgentStatus) and does not line
 * up with the {@code delivery_agents} columns this screen reads. Picking an
 * agent needs an id, a name and the area they cover.</p>
 */
public class AvailableAgent {

    private int agentId;
    private String name;
    private String phone;
    private String areaCovered;
    private boolean available;

    public AvailableAgent() {
    }

    public AvailableAgent(int agentId, String name, String phone,
                          String areaCovered, boolean available) {
        this.agentId = agentId;
        this.name = name;
        this.phone = phone;
        this.areaCovered = areaCovered;
        this.available = available;
    }

    public int getAgentId() { return agentId; }
    public void setAgentId(int agentId) { this.agentId = agentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAreaCovered() { return areaCovered; }
    public void setAreaCovered(String areaCovered) { this.areaCovered = areaCovered; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    /** What the combo box shows. */
    @Override
    public String toString() {
        return areaCovered == null || areaCovered.isBlank()
                ? name
                : name + " (" + areaCovered + ")";
    }
}
