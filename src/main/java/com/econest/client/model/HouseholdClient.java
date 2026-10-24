package com.econest.client.model;

/** A private customer buying for their home. HouseholdClient IS-A Client. */
public class HouseholdClient extends Client {

    /** Choices for the "Preferred delivery slot" drop-down. */
    public static final String[] DELIVERY_SLOTS = {
            "Any time",
            "Weekday morning",
            "Weekday afternoon",
            "Weekday evening",
            "Saturday morning",
            "Saturday afternoon"
    };

    private Integer householdSize;          // Integer, not int, because it can be left empty (null)
    private String preferredDeliverySlot;

    public HouseholdClient(int clientId, String name, ContactDetails contactDetails,
                           Integer householdSize, String preferredDeliverySlot) {
        super(clientId, name, contactDetails);
        this.householdSize = householdSize;
        this.preferredDeliverySlot = preferredDeliverySlot;
    }

    @Override
    public ClientType getClientType() {
        return ClientType.HOUSEHOLD;
    }

    public Integer getHouseholdSize() {
        return householdSize;
    }

    public void setHouseholdSize(Integer householdSize) {
        this.householdSize = householdSize;
    }

    public String getPreferredDeliverySlot() {
        return preferredDeliverySlot;
    }

    public void setPreferredDeliverySlot(String preferredDeliverySlot) {
        this.preferredDeliverySlot = preferredDeliverySlot;
    }
}
