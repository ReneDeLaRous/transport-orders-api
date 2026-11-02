package com.transportes.api.driver;

import com.transportes.api.common.exception.DuplicateResourceException;
import com.transportes.api.driver.dto.CreateDriverRequest;
import com.transportes.api.driver.dto.DriverResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    private DriverService driverService;

    @BeforeEach
    void setUp() {
        driverService = new DriverService(driverRepository, new DriverMapper());
    }

    @Test
    void createDefaultsToActiveAndNormalizesData() {
        when(driverRepository.existsByLicenseNumber("ABC123")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(new Driver("Ana Pérez", "ABC123", true));

        DriverResponse response = driverService.create(new CreateDriverRequest(" Ana Pérez ", " abc123 ", null));

        ArgumentCaptor<Driver> captor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(captor.capture());
        assertEquals("Ana Pérez", captor.getValue().getName());
        assertEquals("ABC123", captor.getValue().getLicenseNumber());
        assertTrue(captor.getValue().isActive());
        assertTrue(response.active());
    }

    @Test
    void createKeepsInactiveFlagWhenProvided() {
        when(driverRepository.existsByLicenseNumber("XYZ987")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(new Driver("Luis Gómez", "XYZ987", false));

        driverService.create(new CreateDriverRequest("Luis Gómez", "XYZ987", false));

        ArgumentCaptor<Driver> captor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    void createRejectsDuplicatedLicense() {
        when(driverRepository.existsByLicenseNumber("ABC123")).thenReturn(true);

        try {
            driverService.create(new CreateDriverRequest("Ana Pérez", "abc123", true));
            fail("Se esperaba DuplicateResourceException");
        } catch (DuplicateResourceException e) {
            assertTrue(e.getMessage().contains("ABC123"));
        }
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void listActiveReturnsOnlyWhatTheRepositoryGives() {
        List<Driver> drivers = Arrays.asList(
                new Driver("Ana Pérez", "ABC123", true),
                new Driver("Luis Gómez", "XYZ987", true));
        when(driverRepository.findByActiveTrueOrderByNameAsc()).thenReturn(drivers);

        List<DriverResponse> result = driverService.listActive();

        assertEquals(2, result.size());
        assertEquals("Ana Pérez", result.get(0).name());
        assertEquals("XYZ987", result.get(1).licenseNumber());
    }
}
