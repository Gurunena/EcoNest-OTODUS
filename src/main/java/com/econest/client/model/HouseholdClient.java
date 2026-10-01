package com.econest.client.model;

public class HouseholdClient extends Client {

    public HouseholdClient() {
    }

    public HouseholdClient(String name, ContactDetails contactDetails) {
        super(name, contactDetails);
    }

    @Override
    public String getClientType() {
        return "Household";
    }
}
