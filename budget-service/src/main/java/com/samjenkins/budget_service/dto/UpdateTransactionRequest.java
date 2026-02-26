package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.TransactionSource;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTransactionRequest(
    UUID budgetId,
    UUID categoryId,
    @Size(max = 255) String merchant,
    @Size(max = 1000) String description,
    Long amountCents,
    LocalDate transactionDate,
    TransactionSource source
) {}
