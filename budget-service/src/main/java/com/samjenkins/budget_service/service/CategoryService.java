package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.CategoryResponse;
import com.samjenkins.budget_service.dto.CreateCategoryRequest;
import com.samjenkins.budget_service.entity.Category;
import com.samjenkins.budget_service.exception.ConflictException;
import jakarta.persistence.EntityManager;
import com.samjenkins.budget_service.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
        String normalized = request.name().trim();
        Category category = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .name(normalized)
            .build();

        try {
            Category saved = categoryRepository.saveAndFlush(category);
            entityManager.refresh(saved);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Category with this name already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getCreatedAt());
    }
}
