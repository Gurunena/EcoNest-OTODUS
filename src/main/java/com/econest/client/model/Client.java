package com.econest.client.model;

public abstract class Client {

    private int id;
    private String name;
    private ContactDetails contactDetails;

    protected Client() {
    }

    protected Client(String name, ContactDetails contactDetails) {
        this.name = name;
        this.contactDetails = contactDetails;
    }

    /** "Household" or "Office". */
    public abstract String getClientType();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ContactDetails getContactDetails() { return contactDetails; }
    public void setContactDetails(ContactDetails contactDetails) { this.contactDetails = contactDetails; }

    @Override
    public String toString() {
        return name;
    }
}
