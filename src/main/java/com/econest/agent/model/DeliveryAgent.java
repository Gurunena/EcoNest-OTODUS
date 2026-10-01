package com.econest.agent.model;

public class DeliveryAgent {

    private int id;
    private String name;
    private String phone;
    private String email;
    private Vehicle vehicle;

    public DeliveryAgent() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    @Override
    public String toString() {
        return name;
    }
}
