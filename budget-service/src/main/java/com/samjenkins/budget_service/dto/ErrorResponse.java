package com.samjenkins.budget_service.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    OffsetDateTime timestamp,
    String requestId,
    int status,
    String error,
    String message,
    List<String> details
) {}
