package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.Alert;
import com.samjenkins.budget_service.entity.AlertType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    boolean existsByUserIdAndTypeAndCategoryIdAndMonthAndThresholdPct(
        UUID userId,
        AlertType type,
        UUID categoryId,
        LocalDate month,
        Integer thresholdPct
    );

    List<Alert> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Alert> findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);
}
