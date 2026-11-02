package com.transportes.api.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByActiveTrueOrderByNameAsc();
}
