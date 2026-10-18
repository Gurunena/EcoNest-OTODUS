package com.econest.client.model;

/** A private customer buying for their home. HouseholdClient IS-A Client. */
public class HouseholdClient extends Client {

    /** Choices for the "Preferred delivery time" drop-down. */
    public static final String[] DELIVERY_TIMES = {
            "Any time",
            "Morning (8am - 12pm)",
            "Afternoon (12pm - 4pm)",
            "Evening (4pm - 7pm)"
    };

    private String preferredDeliveryTime;

    public HouseholdClient(int clientId, String name, ContactDetails contactDetails,
                           String preferredDeliveryTime) {
        super(clientId, name, contactDetails);
        this.preferredDeliveryTime = preferredDeliveryTime;
    }

    @Override
    public ClientType getClientType() {
        return ClientType.HOUSEHOLD;
    }

    public String getPreferredDeliveryTime() {
        return preferredDeliveryTime;
    }

    public void setPreferredDeliveryTime(String preferredDeliveryTime) {
        this.preferredDeliveryTime = preferredDeliveryTime;
    }
}
