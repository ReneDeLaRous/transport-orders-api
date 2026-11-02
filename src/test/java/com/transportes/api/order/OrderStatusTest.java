package com.transportes.api.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void createdCanGoToInTransitOrCancelled() {
        assertTrue(OrderStatus.CREATED.canMoveTo(OrderStatus.IN_TRANSIT));
        assertTrue(OrderStatus.CREATED.canMoveTo(OrderStatus.CANCELLED));
    }

    @Test
    void createdCannotJumpToDelivered() {
        assertFalse(OrderStatus.CREATED.canMoveTo(OrderStatus.DELIVERED));
    }

    @Test
    void inTransitCanGoToDeliveredOrCancelled() {
        assertTrue(OrderStatus.IN_TRANSIT.canMoveTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.IN_TRANSIT.canMoveTo(OrderStatus.CANCELLED));
    }

    @Test
    void inTransitCannotGoBackToCreated() {
        assertFalse(OrderStatus.IN_TRANSIT.canMoveTo(OrderStatus.CREATED));
    }

    @Test
    void finalStatesCannotMoveAnywhere() {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.DELIVERED.canMoveTo(target));
            assertFalse(OrderStatus.CANCELLED.canMoveTo(target));
        }
    }

    @Test
    void aStatusCannotMoveToItself() {
        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(status.canMoveTo(status));
        }
    }
}
