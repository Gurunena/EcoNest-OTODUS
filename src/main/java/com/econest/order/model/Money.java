package com.econest.order.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Money rounding and formatting for the order module.
 *
 * <p>Every amount in this module is a {@link BigDecimal} scaled to 2 decimal
 * places with {@link RoundingMode#HALF_UP}, never a {@code double}. A price
 * like 0.1 has no exact binary representation, so adding up ten of them in
 * {@code double} arithmetic gives 0.9999999999999999 - which is the wrong
 * total to charge a client.</p>
 *
 * <p>Amounts read in from {@code double} (the product module's
 * {@code Product.getPrice()}) go through {@link #of(double)}, which converts
 * via the shortest decimal representation rather than the raw binary value.</p>
 */
public final class Money {

    /** Every amount in the module carries this scale. */
    public static final int SCALE = 2;

    /** Zero, already at the module's scale. */
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

    private Money() {
        // Utility class.
    }

    /** Rounds to 2dp, half up. Null becomes zero. */
    public static BigDecimal scale(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Converts a {@code double} price from another module into a scaled
     * {@code BigDecimal}. Uses {@link BigDecimal#valueOf(double)}, so 1250.0
     * becomes exactly 1250.00 and not 1250.000000000000045...
     */
    public static BigDecimal of(double amount) {
        return scale(BigDecimal.valueOf(amount));
    }

    /** Rounds a whole-number amount. */
    public static BigDecimal of(long amount) {
        return scale(BigDecimal.valueOf(amount));
    }

    /** {@code unitPrice * quantity}, rounded once at the end. */
    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return scale(scale(unitPrice).multiply(BigDecimal.valueOf(quantity)));
    }

    /**
     * Formats an amount the way every EcoNest screen shows money:
     * {@code Rs. 1,250.00}. Null renders as {@code Rs. 0.00}.
     */
    public static String format(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.UK));
        return "Rs. " + format.format(scale(amount));
    }
}
