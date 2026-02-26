package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.BudgetPeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateBudgetRequest(
    @NotBlank @Size(max = 150) String name,
    @NotNull BudgetPeriodType periodType,
    @NotNull LocalDate startDate,
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter code") String currency,
    @Valid List<CreateBudgetCategoryLimitRequest> categoryLimits
) {}
