package com.transportes.api.driver;

import com.transportes.api.driver.dto.CreateDriverRequest;
import com.transportes.api.driver.dto.DriverResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DriverMapper {

    public Driver toEntity(CreateDriverRequest request) {
        boolean active = request.active() == null || request.active();
        return new Driver(request.name().trim(), request.licenseNumber().trim().toUpperCase(), active);
    }

    public DriverResponse toResponse(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getName(), driver.getLicenseNumber(), driver.isActive());
    }

    public List<DriverResponse> toResponseList(List<Driver> drivers) {
        List<DriverResponse> responses = new ArrayList<DriverResponse>();
        for (Driver driver : drivers) {
            responses.add(toResponse(driver));
        }
        return responses;
    }
}
