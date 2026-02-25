package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.AlertResponse;
import com.samjenkins.budget_service.service.AlertService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertResponse> list(
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @RequestParam(required = false) Integer limit
    ) {
        return alertService.list(CurrentUser.userId(), unreadOnly, limit);
    }

    @PatchMapping("/{alertId}/read")
    public AlertResponse markRead(@PathVariable UUID alertId) {
        return alertService.markRead(CurrentUser.userId(), alertId);
    }
}
