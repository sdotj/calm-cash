package com.samjenkins.budget_service.dto;

import com.samjenkins.budget_service.entity.TransactionSource;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID categoryId,
    String merchant,
    String description,
    long amountCents,
    LocalDate transactionDate,
    TransactionSource source,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
