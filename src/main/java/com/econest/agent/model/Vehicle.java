package com.econest.agent.model;

public class Vehicle {

    private String registrationNumber;
    private String type;
    private double capacityKg;

    public Vehicle() {
    }

    public Vehicle(String registrationNumber, String type, double capacityKg) {
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.capacityKg = capacityKg;
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(double capacityKg) { this.capacityKg = capacityKg; }
}
