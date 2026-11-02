package com.transportes.api.assignment;

import com.transportes.api.assignment.dto.AssignDriverRequest;
import com.transportes.api.assignment.dto.AssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/assignment")
@Tag(name = "Asignaciones", description = "Asignación de conductores y archivos de la orden")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asignar un conductor a la orden",
            description = "El conductor debe estar activo y la orden en estado CREATED")
    public AssignmentResponse assign(@PathVariable UUID orderId, @Valid @RequestBody AssignDriverRequest request) {
        return assignmentService.assignDriver(orderId, request.driverId());
    }

    @GetMapping
    @Operation(summary = "Consultar la asignación de una orden")
    public AssignmentResponse get(@PathVariable UUID orderId) {
        return assignmentService.getByOrder(orderId);
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adjuntar un PDF a la asignación", description = "Si ya había uno, se reemplaza")
    public AssignmentResponse uploadPdf(@PathVariable UUID orderId, @RequestParam("file") MultipartFile file) {
        return assignmentService.attachPdf(orderId, file);
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adjuntar una imagen (.png, .jpg) a la asignación", description = "Si ya había una, se reemplaza")
    public AssignmentResponse uploadImage(@PathVariable UUID orderId, @RequestParam("file") MultipartFile file) {
        return assignmentService.attachImage(orderId, file);
    }
}
