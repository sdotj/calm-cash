package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.TransactionSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransactionRequest(
    UUID categoryId,
    @NotBlank @Size(max = 255) String merchant,
    @Size(max = 1000) String description,
    long amountCents,
    @NotNull LocalDate transactionDate,
    @NotNull TransactionSource source
) {}
