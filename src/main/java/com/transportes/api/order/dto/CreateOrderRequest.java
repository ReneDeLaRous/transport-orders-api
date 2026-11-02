package com.transportes.api.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank(message = "el origen es obligatorio")
        @Size(max = 150, message = "el origen no puede pasar de 150 caracteres")
        String origin,

        @NotBlank(message = "el destino es obligatorio")
        @Size(max = 150, message = "el destino no puede pasar de 150 caracteres")
        String destination) {
}
