package com.transportes.api.order;

import com.transportes.api.common.exception.BadRequestException;
import com.transportes.api.common.exception.BusinessRuleException;
import com.transportes.api.common.exception.ResourceNotFoundException;
import com.transportes.api.order.dto.CreateOrderRequest;
import com.transportes.api.order.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, new OrderMapper());
    }

    @Test
    void createSavesOrderInCreatedStatus() {
        when(orderRepository.save(any(Order.class))).thenReturn(new Order("Puebla", "CDMX"));

        OrderResponse response = orderService.create(new CreateOrderRequest("  Puebla ", "CDMX"));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals("Puebla", captor.getValue().getOrigin());
        assertEquals(OrderStatus.CREATED, captor.getValue().getStatus());
        assertEquals(OrderStatus.CREATED, response.status());
    }

    @Test
    void createRejectsSameOriginAndDestination() {
        try {
            orderService.create(new CreateOrderRequest("Puebla", " puebla "));
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("el mismo"));
        }
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getByIdReturnsTheOrder() {
        UUID id = UUID.randomUUID();
        Order order = new Order("Puebla", "CDMX");
        ReflectionTestUtils.setField(order, "id", id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getById(id);

        assertEquals(id, response.id());
        assertEquals("CDMX", response.destination());
    }

    @Test
    void getByIdFailsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        try {
            orderService.getById(id);
            fail("Se esperaba ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains(id.toString()));
        }
    }

    @Test
    void changeStatusAppliesValidTransition() {
        UUID id = UUID.randomUUID();
        Order order = new Order("Puebla", "CDMX");
        ReflectionTestUtils.setField(order, "id", id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.changeStatus(id, OrderStatus.IN_TRANSIT);

        assertEquals(OrderStatus.IN_TRANSIT, response.status());
        verify(orderRepository).save(order);
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        UUID id = UUID.randomUUID();
        Order order = new Order("Puebla", "CDMX");
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        try {
            orderService.changeStatus(id, OrderStatus.DELIVERED);
            fail("Se esperaba BusinessRuleException");
        } catch (BusinessRuleException e) {
            assertTrue(e.getMessage().contains("CREATED"));
            assertTrue(e.getMessage().contains("DELIVERED"));
        }
        assertEquals(OrderStatus.CREATED, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void changeStatusFailsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        try {
            orderService.changeStatus(id, OrderStatus.CANCELLED);
            fail("Se esperaba ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains(id.toString()));
        }
    }

    @Test
    void listReturnsMappedOrders() {
        Order order = new Order("Puebla", "CDMX");
        when(orderRepository.findAll(ArgumentMatchers.<Specification<Order>>any(), any(Sort.class)))
                .thenReturn(Collections.singletonList(order));

        List<OrderResponse> result = orderService.list(OrderStatus.CREATED, null, null, " Puebla ", "");

        assertEquals(1, result.size());
        assertEquals("Puebla", result.get(0).origin());
    }

    @Test
    void listRejectsInvertedDateRange() {
        try {
            orderService.list(null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null);
            fail("Se esperaba BadRequestException");
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("fecha inicial"));
        }
        verify(orderRepository, never()).findAll(ArgumentMatchers.<Specification<Order>>any(), any(Sort.class));
    }
}
