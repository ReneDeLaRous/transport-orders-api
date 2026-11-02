package com.transportes.api.assignment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDriverRequest(
        @NotNull(message = "el id del conductor es obligatorio")
        UUID driverId) {
}
