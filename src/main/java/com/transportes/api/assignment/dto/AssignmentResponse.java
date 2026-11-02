package com.transportes.api.assignment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID orderId,
        UUID driverId,
        String driverName,
        LocalDateTime assignedAt,
        String pdfFile,
        String imageFile) {
}
