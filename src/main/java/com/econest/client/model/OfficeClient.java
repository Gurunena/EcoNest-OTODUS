package com.econest.client.model;

/**
 * A business customer. OfficeClient IS-A Client.
 * For office clients, name holds the company name and contactPerson is who EcoNest deals with.
 */
public class OfficeClient extends Client {

    private String contactPerson;

    public OfficeClient(int clientId, String companyName, ContactDetails contactDetails,
                        String contactPerson) {
        super(clientId, companyName, contactDetails);
        this.contactPerson = contactPerson;
    }

    @Override
    public ClientType getClientType() {
        return ClientType.OFFICE;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
}
