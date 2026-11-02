package com.transportes.api.order;

import com.transportes.api.common.exception.BadRequestException;
import com.transportes.api.common.exception.BusinessRuleException;
import com.transportes.api.common.exception.ResourceNotFoundException;
import com.transportes.api.order.dto.CreateOrderRequest;
import com.transportes.api.order.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = orderMapper.toEntity(request);
        if (order.getOrigin().equalsIgnoreCase(order.getDestination())) {
            throw new BadRequestException("El origen y el destino no pueden ser el mismo");
        }

        Order saved = orderRepository.save(order);
        log.atInfo()
                .addKeyValue("orderId", saved.getId())
                .log("order created");
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return orderMapper.toResponse(findOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(OrderStatus status, LocalDate createdFrom, LocalDate createdTo,
                                    String origin, String destination) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BadRequestException("La fecha inicial no puede ser posterior a la fecha final");
        }

        LocalDateTime from = createdFrom == null ? null : createdFrom.atStartOfDay();
        LocalDateTime to = createdTo == null ? null : createdTo.plusDays(1).atStartOfDay();

        Specification<Order> filters = OrderSpecifications.withFilters(
                status, from, to, blankToNull(origin), blankToNull(destination));
        List<Order> orders = orderRepository.findAll(filters, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderMapper.toResponseList(orders);
    }

    @Transactional
    public OrderResponse changeStatus(UUID id, OrderStatus newStatus) {
        Order order = findOrder(id);
        OrderStatus current = order.getStatus();

        if (!current.canMoveTo(newStatus)) {
            throw new BusinessRuleException(
                    "No se puede cambiar una orden de " + current + " a " + newStatus);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.atInfo()
                .addKeyValue("orderId", saved.getId())
                .addKeyValue("from", current)
                .addKeyValue("to", newStatus)
                .log("order status changed");
        return orderMapper.toResponse(saved);
    }

    private Order findOrder(UUID id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            throw new ResourceNotFoundException("No existe la orden con id " + id);
        }
        return order.get();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
