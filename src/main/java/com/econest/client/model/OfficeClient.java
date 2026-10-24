package com.econest.client.model;

/**
 * A business customer. OfficeClient IS-A Client.
 * For office clients, name holds the company name and contactPerson is who EcoNest deals with.
 */
public class OfficeClient extends Client {

    private String companyRegistrationNo;   // optional, e.g. "PV00123456"
    private String contactPerson;
    private String department;              // optional, e.g. "Facilities"

    public OfficeClient(int clientId, String companyName, ContactDetails contactDetails,
                        String companyRegistrationNo, String contactPerson, String department) {
        super(clientId, companyName, contactDetails);
        this.companyRegistrationNo = companyRegistrationNo;
        this.contactPerson = contactPerson;
        this.department = department;
    }

    @Override
    public ClientType getClientType() {
        return ClientType.OFFICE;
    }

    public String getCompanyRegistrationNo() {
        return companyRegistrationNo;
    }

    public void setCompanyRegistrationNo(String companyRegistrationNo) {
        this.companyRegistrationNo = companyRegistrationNo;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
