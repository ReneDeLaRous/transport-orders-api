package com.transportes.api.driver;

import com.transportes.api.driver.dto.CreateDriverRequest;
import com.transportes.api.driver.dto.DriverResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Conductores", description = "Gestión de conductores")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un conductor", description = "Si no se envía 'active', el conductor queda activo")
    public DriverResponse create(@Valid @RequestBody CreateDriverRequest request) {
        return driverService.create(request);
    }

    @GetMapping("/active")
    @Operation(summary = "Listar los conductores activos")
    public List<DriverResponse> listActive() {
        return driverService.listActive();
    }
}
