package com.econest.client.model;

public class OfficeClient extends Client {

    private String companyName;
    private String contactPerson;

    public OfficeClient() {
    }

    public OfficeClient(String name, ContactDetails contactDetails, String companyName, String contactPerson) {
        super(name, contactDetails);
        this.companyName = companyName;
        this.contactPerson = contactPerson;
    }

    @Override
    public String getClientType() {
        return "Office";
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
}
