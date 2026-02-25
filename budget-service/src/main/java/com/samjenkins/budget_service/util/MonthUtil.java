package com.samjenkins.budget_service.util;

import com.samjenkins.budget_service.exception.BadRequestException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class MonthUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private MonthUtil() {}

    public static YearMonth parse(String value) {
        try {
            return YearMonth.parse(value, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Month must use yyyy-MM format");
        }
    }

    public static String format(YearMonth yearMonth) {
        return yearMonth.format(FORMATTER);
    }
}
