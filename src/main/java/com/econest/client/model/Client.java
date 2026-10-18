package com.econest.client.model;

/**
 * Base class for every EcoNest client.
 *
 * It is abstract because there is no "plain" client: every client is either a
 * HouseholdClient or an OfficeClient. Both inherit the fields below.
 */
public abstract class Client {

    private int clientId;                    // 0 until the database gives it an id
    private String name;                     // person's name (household) or company name (office)
    private ContactDetails contactDetails;   // email, phone, address

    protected Client(int clientId, String name, ContactDetails contactDetails) {
        this.clientId = clientId;
        this.name = name;
        this.contactDetails = contactDetails;
    }

    /** Each subclass says which type it is. */
    public abstract ClientType getClientType();

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    // Shortcuts, so teammates can write client.getEmail()
    // instead of client.getContactDetails().getEmail()
    public String getEmail() {
        return contactDetails.getEmail();
    }

    public String getPhone() {
        return contactDetails.getPhone();
    }

    public String getAddress() {
        return contactDetails.getAddress();
    }

    @Override
    public String toString() {
        return "#" + clientId + " " + name + " (" + getClientType() + ")";
    }
}
