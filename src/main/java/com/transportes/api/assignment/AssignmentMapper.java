package com.transportes.api.assignment;

import com.transportes.api.assignment.dto.AssignmentResponse;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMapper {

    public AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getOrder().getId(),
                assignment.getDriver().getId(),
                assignment.getDriver().getName(),
                assignment.getAssignedAt(),
                assignment.getPdfFile(),
                assignment.getImageFile());
    }
}
