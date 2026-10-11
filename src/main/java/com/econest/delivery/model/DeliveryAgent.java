package econest.model;

public class DeliveryAgent {

    private int agentId;
    private String agentName;
    private String phone;
    private String email;
    private String vehicleNumber;
    private String vehicleType;

    public DeliveryAgent(int agentId,
                         String agentName,
                         String phone,
                         String email,
                         String vehicleNumber,
                         String vehicleType) {

        this.agentId = agentId;
        this.agentName = agentName;
        this.phone = phone;
        this.email = email;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
    }

    public int getAgentId() {
        return agentId;
    }

    public void setAgentId(int agentId) {
        this.agentId = agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
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

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}