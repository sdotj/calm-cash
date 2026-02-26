package com.samjenkins.budget_service.repository;

import com.samjenkins.budget_service.entity.Category;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    List<Category> findAllByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    List<Category> findAllByIdIn(Collection<UUID> ids);
}
