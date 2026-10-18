package com.econest.agent.model;

/** The vehicle a delivery agent drives. */
public class Vehicle {

    private String registrationNumber;   // e.g. "WP CAB-1234"
    private VehicleType type;
    private int maxLoadKg;               // how much it can carry

    public Vehicle() {
    }

    public Vehicle(String registrationNumber, VehicleType type, int maxLoadKg) {
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.maxLoadKg = maxLoadKg;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public int getMaxLoadKg() {
        return maxLoadKg;
    }

    public void setMaxLoadKg(int maxLoadKg) {
        this.maxLoadKg = maxLoadKg;
    }

    @Override
    public String toString() {
        return type + " " + registrationNumber;
    }
}
