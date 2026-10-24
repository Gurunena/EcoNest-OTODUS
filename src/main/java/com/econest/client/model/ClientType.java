package com.econest.client.model;

/**
 * The two kinds of client EcoNest sells to.
 * The constant names (HOUSEHOLD, OFFICE) are what the clients table stores;
 * the label is what the screen shows.
 */
public enum ClientType {

    HOUSEHOLD("Household"),
    OFFICE("Office");

    private final String label;

    ClientType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
