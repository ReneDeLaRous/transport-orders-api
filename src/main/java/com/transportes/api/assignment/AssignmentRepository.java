package com.transportes.api.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    boolean existsByOrderId(UUID orderId);

    Optional<Assignment> findByOrderId(UUID orderId);
}
