package com.transportes.api.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato único de error para toda la API.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
