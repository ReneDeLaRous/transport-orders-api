package com.transportes.api.order.dto;

import com.transportes.api.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "el estado es obligatorio")
        OrderStatus status) {
}
