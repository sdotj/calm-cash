package com.samjenkins.budget_service.controller;

import com.samjenkins.budget_service.config.CurrentUser;
import com.samjenkins.budget_service.dto.CreateTransactionRequest;
import com.samjenkins.budget_service.dto.TransactionResponse;
import com.samjenkins.budget_service.dto.UpdateTransactionRequest;
import com.samjenkins.budget_service.service.TxnService;
import com.samjenkins.budget_service.util.MonthUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TxnController {

    private final TxnService txnService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return txnService.create(CurrentUser.userId(), request);
    }

    @PatchMapping("/{transactionId}")
    public TransactionResponse update(@PathVariable UUID transactionId, @Valid @RequestBody UpdateTransactionRequest request) {
        return txnService.update(CurrentUser.userId(), transactionId, request);
    }

    @GetMapping
    public List<TransactionResponse> list(@RequestParam String month) {
        return txnService.listByMonth(CurrentUser.userId(), MonthUtil.parse(month));
    }
}
