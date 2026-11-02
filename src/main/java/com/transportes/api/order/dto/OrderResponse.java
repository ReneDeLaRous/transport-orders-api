package com.transportes.api.order.dto;

import com.transportes.api.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        String origin,
        String destination,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
