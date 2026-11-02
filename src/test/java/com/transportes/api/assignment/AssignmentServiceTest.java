package com.transportes.api.assignment;

import com.transportes.api.assignment.dto.AssignmentResponse;
import com.transportes.api.common.exception.BusinessRuleException;
import com.transportes.api.common.exception.ResourceNotFoundException;
import com.transportes.api.driver.Driver;
import com.transportes.api.driver.DriverRepository;
import com.transportes.api.order.Order;
import com.transportes.api.order.OrderRepository;
import com.transportes.api.order.OrderStatus;
import com.transportes.api.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private FileStorageService fileStorageService;

    private AssignmentService assignmentService;

    private UUID orderId;
    private UUID driverId;
    private Order order;
    private Driver activeDriver;

    @BeforeEach
    void setUp() {
        assignmentService = new AssignmentService(assignmentRepository, orderRepository, driverRepository,
                new AssignmentMapper(), fileStorageService);

        orderId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        order = new Order("Puebla", "CDMX");
        ReflectionTestUtils.setField(order, "id", orderId);

        activeDriver = new Driver("Ana Pérez", "ABC123", true);
        ReflectionTestUtils.setField(activeDriver, "id", driverId);
    }

    @Test
    void assignDriverCreatesTheAssignment() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
        when(assignmentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(new Assignment(order, activeDriver));

        AssignmentResponse response = assignmentService.assignDriver(orderId, driverId);

        assertEquals(orderId, response.orderId());
        assertEquals(driverId, response.driverId());
        assertEquals("Ana Pérez", response.driverName());
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void assignDriverFailsWhenOrderDoesNotExist() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        try {
            assignmentService.assignDriver(orderId, driverId);
            fail("Se esperaba ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains(orderId.toString()));
        }
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void assignDriverFailsWhenDriverDoesNotExist() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverRepository.findById(driverId)).thenReturn(Optional.empty());

        try {
            assignmentService.assignDriver(orderId, driverId);
            fail("Se esperaba ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains(driverId.toString()));
        }
    }

    @Test
    void assignDriverFailsWhenOrderIsNotCreated() {
        order.setStatus(OrderStatus.IN_TRANSIT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));

        try {
            assignmentService.assignDriver(orderId, driverId);
            fail("Se esperaba BusinessRuleException");
        } catch (BusinessRuleException e) {
            assertTrue(e.getMessage().contains("CREATED"));
        }
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void assignDriverFailsWhenDriverIsInactive() {
        Driver inactive = new Driver("Luis Gómez", "XYZ987", false);
        ReflectionTestUtils.setField(inactive, "id", driverId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(inactive));

        try {
            assignmentService.assignDriver(orderId, driverId);
            fail("Se esperaba BusinessRuleException");
        } catch (BusinessRuleException e) {
            assertTrue(e.getMessage().contains("no está activo"));
        }
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void assignDriverFailsWhenOrderAlreadyHasAssignment() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(activeDriver));
        when(assignmentRepository.existsByOrderId(orderId)).thenReturn(true);

        try {
            assignmentService.assignDriver(orderId, driverId);
            fail("Se esperaba BusinessRuleException");
        } catch (BusinessRuleException e) {
            assertTrue(e.getMessage().contains("ya tiene un conductor"));
        }
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void attachPdfStoresTheFileAndKeepsItsName() {
        Assignment assignment = new Assignment(order, activeDriver);
        MockMultipartFile file = new MockMultipartFile("file", "guia.pdf", "application/pdf", "%PDF-1.4".getBytes());
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(assignment));
        when(fileStorageService.storePdf(file)).thenReturn("nuevo.pdf");
        when(assignmentRepository.save(assignment)).thenReturn(assignment);

        AssignmentResponse response = assignmentService.attachPdf(orderId, file);

        assertEquals("nuevo.pdf", response.pdfFile());
        assertNull(response.imageFile());
        verify(fileStorageService).delete(null);
    }

    @Test
    void attachPdfReplacesThePreviousOne() {
        Assignment assignment = new Assignment(order, activeDriver);
        assignment.setPdfFile("viejo.pdf");
        MockMultipartFile file = new MockMultipartFile("file", "guia.pdf", "application/pdf", "%PDF-1.4".getBytes());
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(assignment));
        when(fileStorageService.storePdf(file)).thenReturn("nuevo.pdf");
        when(assignmentRepository.save(assignment)).thenReturn(assignment);

        AssignmentResponse response = assignmentService.attachPdf(orderId, file);

        assertEquals("nuevo.pdf", response.pdfFile());
        verify(fileStorageService).delete("viejo.pdf");
    }

    @Test
    void attachImageStoresTheFileAndKeepsItsName() {
        Assignment assignment = new Assignment(order, activeDriver);
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(assignment));
        when(fileStorageService.storeImage(file)).thenReturn("foto-guardada.png");
        when(assignmentRepository.save(assignment)).thenReturn(assignment);

        AssignmentResponse response = assignmentService.attachImage(orderId, file);

        assertEquals("foto-guardada.png", response.imageFile());
        assertNull(response.pdfFile());
    }

    @Test
    void attachFilesFailsWhenOrderHasNoAssignment() {
        MockMultipartFile file = new MockMultipartFile("file", "guia.pdf", "application/pdf", "%PDF-1.4".getBytes());
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        try {
            assignmentService.attachPdf(orderId, file);
            fail("Se esperaba ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertTrue(e.getMessage().contains("no tiene una asignación"));
        }
        verify(fileStorageService, never()).storePdf(any(MockMultipartFile.class));
    }
}
