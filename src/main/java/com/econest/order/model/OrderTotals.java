package com.econest.order.model;

import java.math.BigDecimal;

/**
 * What an order adds up to. Produced by
 * {@code OrderService.totalsOf(items)} - the UI never does this arithmetic
 * itself, it only displays the result.
 *
 * <p><b>On discounts and delivery fees:</b> the task asked for them "if the
 * schema has it", and {@code orders} has neither column - only
 * {@code total_amount}. So {@link #grandTotal()} equals {@link #subtotal()}
 * today. The two are kept apart anyway, because that is the one seam a
 * discount or a fee would be added at, and every caller already reads the
 * grand total.</p>
 *
 * @param itemCount   how many lines are on the order
 * @param unitCount   how many units in total across those lines
 * @param subtotal    the line totals added up
 * @param grandTotal  what the client pays
 */
public record OrderTotals(int itemCount, int unitCount,
                          BigDecimal subtotal, BigDecimal grandTotal) {

    /** An order with nothing on it. */
    public static final OrderTotals EMPTY =
            new OrderTotals(0, 0, Money.ZERO, Money.ZERO);

    public OrderTotals {
        subtotal = Money.scale(subtotal);
        grandTotal = Money.scale(grandTotal);
    }
}
