package com.econest.client.model;

/**
 * How to reach a client. A Client HAS-A ContactDetails (composition).
 * The fields match the contact columns of the clients table.
 */
public class ContactDetails {

    private String email;
    private String phone;
    private String addressLine1;   // required, e.g. "42 Lake Road"
    private String addressLine2;   // optional, e.g. "Apt 3B"
    private String city;           // required
    private String postalCode;     // optional

    public ContactDetails(String email, String phone, String addressLine1,
                          String addressLine2, String city, String postalCode) {
        this.email = email;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.postalCode = postalCode;
    }

    /** The whole address on one line, skipping the parts that are empty. */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder(addressLine1 == null ? "" : addressLine1);
        for (String part : new String[]{addressLine2, city, postalCode}) {
            if (part != null && !part.isBlank()) {
                address.append(", ").append(part);
            }
        }
        return address.toString();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}
