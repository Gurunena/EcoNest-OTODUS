package com.econest.client.model;

/** The two kinds of client EcoNest sells to. The label is what gets saved in the database. */
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

    /** Turns the text from the database ("Household") back into the enum. */
    public static ClientType fromLabel(String label) {
        for (ClientType type : values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown client type: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
