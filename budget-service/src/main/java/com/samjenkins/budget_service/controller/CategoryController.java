package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.CategoryResponse;
import com.samjenkins.budget_service.dto.CreateCategoryRequest;
import com.samjenkins.budget_service.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(CurrentUser.userId(), request);
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.list(CurrentUser.userId());
    }
}
