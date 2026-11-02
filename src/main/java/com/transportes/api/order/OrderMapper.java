package com.transportes.api.order;

import com.transportes.api.order.dto.CreateOrderRequest;
import com.transportes.api.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMapper {

    public Order toEntity(CreateOrderRequest request) {
        return new Order(request.origin().trim(), request.destination().trim());
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getOrigin(),
                order.getDestination(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public List<OrderResponse> toResponseList(List<Order> orders) {
        List<OrderResponse> responses = new ArrayList<OrderResponse>();
        for (Order order : orders) {
            responses.add(toResponse(order));
        }
        return responses;
    }
}
