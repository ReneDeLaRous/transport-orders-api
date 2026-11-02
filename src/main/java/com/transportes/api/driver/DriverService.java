package com.transportes.api.driver;

import com.transportes.api.common.exception.DuplicateResourceException;
import com.transportes.api.driver.dto.CreateDriverRequest;
import com.transportes.api.driver.dto.DriverResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverService(DriverRepository driverRepository, DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
    }

    @Transactional
    public DriverResponse create(CreateDriverRequest request) {
        Driver driver = driverMapper.toEntity(request);

        if (driverRepository.existsByLicenseNumber(driver.getLicenseNumber())) {
            throw new DuplicateResourceException(
                    "Ya existe un conductor con la licencia " + driver.getLicenseNumber());
        }

        Driver saved = driverRepository.save(driver);
        log.atInfo()
                .addKeyValue("driverId", saved.getId())
                .log("driver created");
        return driverMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> listActive() {
        return driverMapper.toResponseList(driverRepository.findByActiveTrueOrderByNameAsc());
    }
}
