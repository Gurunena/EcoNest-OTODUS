package com.econest.agent.model;

/**
 * A delivery person. A DeliveryAgent HAS-A Vehicle (composition).
 * Task 6 uses status to pick an available agent; Task 9 emails the agent at email.
 */
public class DeliveryAgent {

    private int agentId;                 // 0 until the database gives it an id
    private String name;
    private String nic;                  // National Identity Card number
    private String phone;
    private String email;
    private Vehicle vehicle;
    private AgentStatus status = AgentStatus.AVAILABLE;

    public DeliveryAgent() {
    }

    public DeliveryAgent(int agentId, String name, String nic, String phone, String email,
                         Vehicle vehicle, AgentStatus status) {
        this.agentId = agentId;
        this.name = name;
        this.nic = nic;
        this.phone = phone;
        this.email = email;
        this.vehicle = vehicle;
        this.status = status;
    }

    public int getAgentId() {
        return agentId;
    }

    public void setAgentId(int agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    /** Convenience check for Task 6. */
    public boolean isAvailable() {
        return status == AgentStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "#" + agentId + " " + name;
    }
}
