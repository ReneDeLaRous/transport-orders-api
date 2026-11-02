package com.transportes.api.order;

import com.transportes.api.order.dto.CreateOrderRequest;
import com.transportes.api.order.dto.OrderResponse;
import com.transportes.api.order.dto.UpdateOrderStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Órdenes", description = "Gestión de órdenes de transporte")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Crear una orden", description = "La orden nace en estado CREATED")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una orden por id")
    public OrderResponse getById(@PathVariable UUID id) {
        return orderService.getById(id);
    }

    @GetMapping
    @Operation(summary = "Listar órdenes",
            description = "Todos los filtros son opcionales. Las fechas usan el formato yyyy-MM-dd y se aplican sobre la fecha de creación.")
    public List<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination) {
        return orderService.list(status, createdFrom, createdTo, origin, destination);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado de una orden",
            description = "Flujo válido: CREATED -> IN_TRANSIT -> DELIVERED. Desde CREATED o IN_TRANSIT también se puede pasar a CANCELLED.")
    public OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.changeStatus(id, request.status());
    }
}
