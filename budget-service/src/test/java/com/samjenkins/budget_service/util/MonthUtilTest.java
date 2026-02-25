package com.samjenkins.budget_service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.samjenkins.budget_service.exception.BadRequestException;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MonthUtilTest {

    @Test
    void parseAcceptsValidYearMonth() {
        YearMonth parsed = MonthUtil.parse("2026-03");
        assertEquals(YearMonth.of(2026, 3), parsed);
    }

    @Test
    void parseRejectsInvalidFormat() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> MonthUtil.parse("03-2026"));
        assertEquals("Month must use yyyy-MM format", ex.getMessage());
    }

    @Test
    void formatProducesYearMonthString() {
        assertEquals("2026-11", MonthUtil.format(YearMonth.of(2026, 11)));
    }
}
