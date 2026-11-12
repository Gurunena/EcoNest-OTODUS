package com.econest.order;

import com.econest.order.model.Money;
import com.econest.order.model.OrderItem;
import com.econest.order.model.OrderTotals;
import com.econest.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * What an order adds up to.
 *
 * <p>{@code OrderService.totalsOf} is static and touches no database, which
 * is the whole reason the arithmetic lives in the service rather than in the
 * dialog: it can be checked here, with no MySQL running.</p>
 */
class OrderTotalsTest {

    private static OrderItem line(String name, int quantity, String unitPrice) {
        return new OrderItem(1, name, quantity, new BigDecimal(unitPrice));
    }

    @Test
    @DisplayName("an empty order is zero, not null")
    void emptyOrderIsZero() {
        assertSame(OrderTotals.EMPTY, OrderService.totalsOf(List.of()));
        assertSame(OrderTotals.EMPTY, OrderService.totalsOf(null));
        assertEquals(0, OrderTotals.EMPTY.grandTotal().signum());
    }

    @Test
    @DisplayName("one line is quantity times unit price")
    void singleLine() {
        OrderTotals totals = OrderService.totalsOf(List.of(line("Peace Lily", 3, "1250.00")));

        assertEquals(new BigDecimal("3750.00"), totals.subtotal());
        assertEquals(new BigDecimal("3750.00"), totals.grandTotal());
        assertEquals(1, totals.itemCount());
        assertEquals(3, totals.unitCount());
    }

    @Test
    @DisplayName("several lines add up, and the units are counted across them")
    void severalLines() {
        OrderTotals totals = OrderService.totalsOf(List.of(
                line("Peace Lily", 3, "1250.00"),
                line("Clay Planter", 2, "899.50"),
                line("Starter Kit", 1, "4499.99")));

        // 3750.00 + 1799.00 + 4499.99
        assertEquals(new BigDecimal("10048.99"), totals.subtotal());
        assertEquals(new BigDecimal("10048.99"), totals.grandTotal());
        assertEquals(3, totals.itemCount());
        assertEquals(6, totals.unitCount());
    }

    @Test
    @DisplayName("totals always carry 2 decimal places")
    void totalsAreAlwaysScaled() {
        OrderTotals totals = OrderService.totalsOf(List.of(line("Trowel", 2, "100")));

        assertEquals(2, totals.subtotal().scale());
        assertEquals(2, totals.grandTotal().scale());
        assertEquals(new BigDecimal("200.00"), totals.subtotal());
    }

    @Test
    @DisplayName("a half-cent line rounds half up, once per line")
    void roundsHalfUpPerLine() {
        // 0.125 * 1 rounds to 0.13, not down to 0.12.
        OrderTotals totals = OrderService.totalsOf(List.of(line("Seed", 1, "0.125")));
        assertEquals(new BigDecimal("0.13"), totals.subtotal());
    }

    @Test
    @DisplayName("ten lines of 0.10 come to exactly 1.00")
    void noFloatingPointDrift() {
        List<OrderItem> tenCents = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenCents.add(new OrderItem(i + 1, "Seed " + i, 1, new BigDecimal("0.10")));
        }

        BigDecimal total = OrderService.totalsOf(tenCents).grandTotal();

        assertEquals(new BigDecimal("1.00"), total);

        // This is the case BigDecimal exists for. The same sum in double
        // arithmetic lands on 0.9999999999999999, which is the wrong amount
        // to charge a client.
        double drifted = 0;
        for (int i = 0; i < 10; i++) {
            drifted += 0.1d;
        }
        assertNotEquals(1.0d, drifted, "repeated double addition really does drift");
    }

    @Test
    @DisplayName("a line total is derived, so changing the quantity changes it")
    void lineTotalFollowsQuantity() {
        OrderItem item = line("Peace Lily", 2, "1250.00");
        assertEquals(new BigDecimal("2500.00"), item.getLineTotal());

        item.setQuantity(5);
        assertEquals(new BigDecimal("6250.00"), item.getLineTotal());
    }

    @Nested
    @DisplayName("Money")
    class MoneyTest {

        @Test
        @DisplayName("formats as Rs. 1,250.00 with a thousands separator")
        void formatsWithSeparator() {
            assertEquals("Rs. 1,250.00", Money.format(new BigDecimal("1250")));
            assertEquals("Rs. 0.00", Money.format(BigDecimal.ZERO));
            assertEquals("Rs. 0.00", Money.format(null));
            assertEquals("Rs. 1,234,567.89", Money.format(new BigDecimal("1234567.891")));
        }

        @Test
        @DisplayName("converts a double price without binary drift")
        void convertsDoubleCleanly() {
            assertEquals(new BigDecimal("1250.00"), Money.of(1250.0d));
            assertEquals(new BigDecimal("0.10"), Money.of(0.1d));
            assertEquals(new BigDecimal("899.50"), Money.of(899.5d));
        }

        @Test
        @DisplayName("multiply rounds once, at the end")
        void multiplyRoundsOnce() {
            assertEquals(new BigDecimal("3750.00"),
                    Money.multiply(new BigDecimal("1250.00"), 3));
            assertEquals(new BigDecimal("0.00"),
                    Money.multiply(new BigDecimal("1250.00"), 0));
        }
    }
}
