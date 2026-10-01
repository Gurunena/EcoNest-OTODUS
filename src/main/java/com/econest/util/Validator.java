package com.econest.util;

/** Shared input validation helpers for forms. */
public final class Validator {

    private Validator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // TODO: email, phone, price, quantity checks
}
