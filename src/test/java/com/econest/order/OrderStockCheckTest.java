package com.econest.order;

import com.econest.order.model.Order;
import com.econest.order.model.OrderItem;
import com.econest.order.model.OrderStatus;
import com.econest.order.model.ProductRef;
import com.econest.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules that stop a bad order being saved: stock, quantities, dates and
 * status moves.
 *
 * <p>All of these are static and database-free, which is what lets the
 * create-order dialog run them while the user is still typing.</p>
 */
class OrderStockCheckTest {

    private static final ProductRef LILY =
            new ProductRef(1, "Peace Lily", new BigDecimal("1250.00"), 5);

    private static final ProductRef SOLD_OUT =
            new ProductRef(2, "Clay Planter", new BigDecimal("899.50"), 0);

    // =====================================================================
    // checkStock
    // =====================================================================

    @Test
    @DisplayName("a quantity within stock passes")
    void withinStockPasses() {
        assertDoesNotThrow(() -> OrderService.checkStock(LILY, 5));
        assertDoesNotThrow(() -> OrderService.checkStock(LILY, 1));
    }

    @Test
    @DisplayName("one more than stock is refused, and says how many there are")
    void aboveStockIsRefused() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> OrderService.checkStock(LILY, 6));

        assertTrue(thrown.getMessage().contains("Only 5"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("Peace Lily"), thrown.getMessage());
        // No stack trace wording, no SQL - just a sentence the user can act on.
        assertFalse(thrown.getMessage().toLowerCase().contains("exception"));
    }

    @Test
    @DisplayName("zero and negative quantities are refused")
    void quantityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> OrderService.checkStock(LILY, 0));
        assertThrows(IllegalArgumentException.class, () -> OrderService.checkStock(LILY, -3));
    }

    @Test
    @DisplayName("an out-of-stock product says so rather than quoting 0 left")
    void outOfStockIsNamed() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> OrderService.checkStock(SOLD_OUT, 1));

        assertEquals("Clay Planter is out of stock.", thrown.getMessage());
    }

    @Test
    @DisplayName("no product chosen is refused before anything else")
    void productIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> OrderService.checkStock(null, 1));
    }

    @Test
    @DisplayName("units already on the order share the same shelf")
    void unitsAlreadyOnTheOrderCount() {
        // 5 on hand, 3 already spoken for on this order, so 2 are left.
        assertDoesNotThrow(() -> OrderService.checkStock(LILY, 2, 3));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> OrderService.checkStock(LILY, 3, 3));
        assertTrue(thrown.getMessage().contains("Only 2"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("already on this order"),
                thrown.getMessage());
    }

    @Test
    @DisplayName("an order already holding everything leaves nothing to add")
    void nothingLeftOnceTheOrderHoldsItAll() {
        assertThrows(IllegalArgumentException.class, () -> OrderService.checkStock(LILY, 1, 5));
    }

    // =====================================================================
    // validate
    // =====================================================================

    @Nested
    @DisplayName("validate")
    class ValidateTest {

        private Order draft() {
            Order order = new Order();
            order.setClientId(7);
            order.setOrderDate(LocalDateTime.of(2026, 3, 10, 9, 30));
            order.addItem(new OrderItem(1, "Peace Lily", 2, new BigDecimal("1250.00")));
            return order;
        }

        @Test
        @DisplayName("a complete draft passes")
        void completeDraftPasses() {
            assertDoesNotThrow(() -> OrderService.validate(draft()));
        }

        @Test
        @DisplayName("a client is required")
        void clientIsRequired() {
            Order order = draft();
            order.setClientId(0);

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> OrderService.validate(order));
            assertTrue(thrown.getMessage().contains("client"), thrown.getMessage());
        }

        @Test
        @DisplayName("at least one item is required")
        void atLeastOneItem() {
            Order order = draft();
            order.clearItems();

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> OrderService.validate(order));
            assertTrue(thrown.getMessage().contains("at least one item"),
                    thrown.getMessage());
        }

        @Test
        @DisplayName("every quantity must be above zero")
        void quantityAboveZero() {
            Order order = draft();
            order.clearItems();
            order.addItem(new OrderItem(1, "Peace Lily", 0, new BigDecimal("1250.00")));

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> OrderService.validate(order));
            assertTrue(thrown.getMessage().contains("more than zero"), thrown.getMessage());
        }

        @Test
        @DisplayName("the delivery date cannot be before the order date")
        void deliveryNotBeforeOrder() {
            Order order = draft();
            order.setDeliveryDate(LocalDate.of(2026, 3, 9));

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> OrderService.validate(order));
            assertTrue(thrown.getMessage().contains("cannot be before"), thrown.getMessage());
        }

        @Test
        @DisplayName("the delivery date may be the order date itself")
        void sameDayDeliveryIsFine() {
            Order order = draft();
            order.setDeliveryDate(LocalDate.of(2026, 3, 10));

            assertDoesNotThrow(() -> OrderService.validate(order));
        }

        @Test
        @DisplayName("no delivery date at all is fine - the column is nullable")
        void deliveryDateIsOptional() {
            Order order = draft();
            order.setDeliveryDate(null);

            assertDoesNotThrow(() -> OrderService.validate(order));
        }

        @Test
        @DisplayName("the same product twice is caught here, not by the unique key")
        void duplicateProductIsRefused() {
            Order order = draft();
            order.addItem(new OrderItem(1, "Peace Lily", 1, new BigDecimal("1250.00")));

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> OrderService.validate(order));
            assertTrue(thrown.getMessage().contains("more than once"), thrown.getMessage());
            assertTrue(thrown.getMessage().contains("Peace Lily"), thrown.getMessage());
        }
    }

    // =====================================================================
    // Status transitions
    // =====================================================================

    @Nested
    @DisplayName("status transitions")
    class StatusTest {

        @Test
        @DisplayName("the happy path walks pending to delivered")
        void happyPath() {
            assertEquals(OrderStatus.CONFIRMED, OrderStatus.PENDING.next());
            assertEquals(OrderStatus.PACKED, OrderStatus.CONFIRMED.next());
            assertEquals(OrderStatus.DISPATCHED, OrderStatus.PACKED.next());
            assertEquals(OrderStatus.DELIVERED, OrderStatus.DISPATCHED.next());
            assertEquals(null, OrderStatus.DELIVERED.next());
            assertEquals(null, OrderStatus.CANCELLED.next());
        }

        @Test
        @DisplayName("an order cannot go backwards")
        void noGoingBackwards() {
            assertFalse(OrderStatus.DELIVERED.canMoveTo(OrderStatus.PENDING));
            assertFalse(OrderStatus.DISPATCHED.canMoveTo(OrderStatus.CONFIRMED));
            assertFalse(OrderStatus.PACKED.canMoveTo(OrderStatus.PENDING));
        }

        @Test
        @DisplayName("a delivered or cancelled order is the end of the line")
        void terminalStatuses() {
            assertTrue(OrderStatus.DELIVERED.allowedNext().isEmpty());
            assertTrue(OrderStatus.CANCELLED.allowedNext().isEmpty());
            assertFalse(OrderStatus.DELIVERED.canMoveTo(OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("pending may be dispatched directly, which is what task 6 does")
        void deliveryModuleCanDispatchAPendingOrder() {
            assertTrue(OrderStatus.PENDING.canMoveTo(OrderStatus.DISPATCHED));
            assertTrue(OrderStatus.CONFIRMED.canMoveTo(OrderStatus.DISPATCHED));
        }

        @Test
        @DisplayName("only a pending order is editable")
        void onlyPendingIsEditable() {
            assertTrue(OrderStatus.PENDING.isEditable());
            for (OrderStatus status : OrderStatus.values()) {
                if (status != OrderStatus.PENDING) {
                    assertFalse(status.isEditable(), status + " should not be editable");
                }
            }
        }

        @Test
        @DisplayName("dispatched and delivered orders cannot be deleted")
        void dispatchedAndDeliveredAreOutOfOurHands() {
            assertTrue(OrderStatus.DISPATCHED.isOutOfOurHands());
            assertTrue(OrderStatus.DELIVERED.isOutOfOurHands());
            assertFalse(OrderStatus.PENDING.isOutOfOurHands());
            assertFalse(OrderStatus.CONFIRMED.isOutOfOurHands());
            assertFalse(OrderStatus.CANCELLED.isOutOfOurHands());
        }

        @Test
        @DisplayName("OUT_FOR_DELIVERY reads back as DISPATCHED")
        void outForDeliveryIsAnAlias() {
            assertEquals(OrderStatus.DISPATCHED, OrderStatus.of("OUT_FOR_DELIVERY"));
            assertEquals(OrderStatus.DISPATCHED, OrderStatus.of("DISPATCHED"));
            assertEquals(OrderStatus.DISPATCHED, OrderStatus.of("dispatched"));
            assertEquals(OrderStatus.PENDING, OrderStatus.of("Pending"));
        }

        @Test
        @DisplayName("every schema enum value maps to a status")
        void everySchemaValueMaps() {
            for (String stored : new String[]{"PENDING", "CONFIRMED", "PACKED",
                    "DISPATCHED", "DELIVERED", "CANCELLED"}) {
                assertEquals(stored, OrderStatus.of(stored).name());
            }
        }

        @Test
        @DisplayName("an unknown status is refused with a readable message")
        void unknownStatusIsRefused() {
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> OrderStatus.of("SHIPPED"));
            assertTrue(thrown.getMessage().contains("SHIPPED"), thrown.getMessage());
        }

        @Test
        @DisplayName("only a cancelled order has already given its stock back")
        void cancelledHoldsNoStock() {
            assertFalse(OrderStatus.CANCELLED.holdsStock());
            assertTrue(OrderStatus.PENDING.holdsStock());
            assertTrue(OrderStatus.DELIVERED.holdsStock());
        }
    }
}
