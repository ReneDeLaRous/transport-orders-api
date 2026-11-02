package com.transportes.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "el usuario es obligatorio")
        @Size(min = 3, max = 50, message = "el usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "la contraseña debe tener entre 8 y 100 caracteres")
        String password) {
}
