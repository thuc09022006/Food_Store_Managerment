package com.foodstore.orders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.SERVED));
        assertTrue(OrderStatus.SERVED.canTransitionTo(OrderStatus.PAID));
    }

    @Test
    void skippingStepsIsRejected() {
        assertFalse(OrderStatus.NEW.canTransitionTo(OrderStatus.SERVED));
        assertFalse(OrderStatus.NEW.canTransitionTo(OrderStatus.PAID));
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.PAID));
    }

    @Test
    void cancelAllowedBeforePaidOnly() {
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SERVED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void terminalStatesGoNowhere() {
        for (OrderStatus next : OrderStatus.values()) {
            assertFalse(OrderStatus.PAID.canTransitionTo(next));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(next));
        }
        assertTrue(OrderStatus.PAID.isTerminal());
    }

    @Test
    void dineInOrderNeedsATable() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order(0, null, null, 3, OrderType.DINE_IN, OrderStatus.NEW, LocalDateTime.now(), null));
    }

    @Test
    void subtotalUsesSnapshotPrices() {
        Order order = new Order(1, null, null, 3, OrderType.TAKEAWAY, OrderStatus.NEW, LocalDateTime.now(), null);
        order.addItem(new OrderItem(1, 1, 1, 2, BigDecimal.valueOf(55000), null));
        order.addItem(new OrderItem(2, 1, 5, 2, BigDecimal.valueOf(5000), null));
        org.junit.jupiter.api.Assertions.assertEquals(0, BigDecimal.valueOf(120000).compareTo(order.subtotal()));
    }
}
