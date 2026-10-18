package com.econest.agent.model;

/** The kinds of vehicle EcoNest's delivery agents use. */
public enum VehicleType {

    MOTORBIKE("Motorbike"),
    THREE_WHEELER("Three-wheeler"),
    VAN("Van"),
    LORRY("Lorry");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Turns the text from the database ("Van") back into the enum. */
    public static VehicleType fromLabel(String label) {
        for (VehicleType type : values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown vehicle type: " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
