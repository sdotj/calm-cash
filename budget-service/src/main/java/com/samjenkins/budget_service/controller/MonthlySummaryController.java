package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.MonthlySummaryResponse;
import com.samjenkins.budget_service.service.MonthlySummaryService;
import com.samjenkins.budget_service.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monthly-summary")
@RequiredArgsConstructor
public class MonthlySummaryController {

    private final MonthlySummaryService monthlySummaryService;

    @GetMapping
    public MonthlySummaryResponse get(@RequestParam String month) {
        return monthlySummaryService.summarize(CurrentUser.userId(), MonthUtil.parse(month));
    }
}
