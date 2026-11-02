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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final AssignmentMapper assignmentMapper;
    private final FileStorageService fileStorageService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             OrderRepository orderRepository,
                             DriverRepository driverRepository,
                             AssignmentMapper assignmentMapper,
                             FileStorageService fileStorageService) {
        this.assignmentRepository = assignmentRepository;
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.assignmentMapper = assignmentMapper;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AssignmentResponse assignDriver(UUID orderId, UUID driverId) {
        Order order = findOrder(orderId);
        Driver driver = findDriver(driverId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessRuleException(
                    "Solo se puede asignar conductor a órdenes en estado CREATED (estado actual: "
                            + order.getStatus() + ")");
        }
        if (!driver.isActive()) {
            throw new BusinessRuleException("El conductor " + driverId + " no está activo");
        }
        if (assignmentRepository.existsByOrderId(orderId)) {
            throw new BusinessRuleException("La orden " + orderId + " ya tiene un conductor asignado");
        }

        Assignment saved = assignmentRepository.save(new Assignment(order, driver));
        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("driverId", driverId)
                .log("driver assigned to order");
        return assignmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getByOrder(UUID orderId) {
        return assignmentMapper.toResponse(findAssignment(orderId));
    }

    @Transactional
    public AssignmentResponse attachPdf(UUID orderId, MultipartFile file) {
        Assignment assignment = findAssignment(orderId);

        String previous = assignment.getPdfFile();
        String storedName = fileStorageService.storePdf(file);
        assignment.setPdfFile(storedName);
        Assignment saved = assignmentRepository.save(assignment);

        fileStorageService.delete(previous);
        return assignmentMapper.toResponse(saved);
    }

    @Transactional
    public AssignmentResponse attachImage(UUID orderId, MultipartFile file) {
        Assignment assignment = findAssignment(orderId);

        String previous = assignment.getImageFile();
        String storedName = fileStorageService.storeImage(file);
        assignment.setImageFile(storedName);
        Assignment saved = assignmentRepository.save(assignment);

        fileStorageService.delete(previous);
        return assignmentMapper.toResponse(saved);
    }

    private Order findOrder(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty()) {
            throw new ResourceNotFoundException("No existe la orden con id " + orderId);
        }
        return order.get();
    }

    private Driver findDriver(UUID driverId) {
        Optional<Driver> driver = driverRepository.findById(driverId);
        if (driver.isEmpty()) {
            throw new ResourceNotFoundException("No existe el conductor con id " + driverId);
        }
        return driver.get();
    }

    private Assignment findAssignment(UUID orderId) {
        Optional<Assignment> assignment = assignmentRepository.findByOrderId(orderId);
        if (assignment.isEmpty()) {
            throw new ResourceNotFoundException("La orden " + orderId + " no tiene una asignación");
        }
        return assignment.get();
    }
}
