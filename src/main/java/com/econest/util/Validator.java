package com.econest.util;

import java.util.regex.Pattern;

/** Shared input validation helpers for forms. */
public final class Validator {

    /** Sri Lankan NIC: 9 digits plus V/X, or the newer 12 digits. */
    private static final Pattern NIC = Pattern.compile("\d{9}[VvXx]|\d{12}");

    /** Local mobile / land line: 10 digits starting with 0. */
    private static final Pattern PHONE = Pattern.compile("0\d{9}");

    /** Deliberately lenient: one @, a dot in the domain, no spaces. */
    private static final Pattern EMAIL =
            Pattern.compile("[^@\s]+@[^@\s.]+(\.[^@\s.]+)+");

    private Validator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** True for "199012345V" or "199012345678". Blank is not valid. */
    public static boolean isValidNic(String value) {
        return !isBlank(value) && NIC.matcher(value.trim()).matches();
    }

    /** True for "0712345678". Blank is not valid. */
    public static boolean isValidPhone(String value) {
        return !isBlank(value) && PHONE.matcher(value.trim()).matches();
    }

    /** True for "user@example.com". Blank is not valid. */
    public static boolean isValidEmail(String value) {
        return !isBlank(value) && EMAIL.matcher(value.trim()).matches();
    }

    // TODO: price, quantity checks
}
