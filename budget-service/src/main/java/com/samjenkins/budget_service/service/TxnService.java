package com.samjenkins.budget_service.service;

import com.samjenkins.budget_service.dto.CreateTransactionRequest;
import com.samjenkins.budget_service.dto.TransactionResponse;
import com.samjenkins.budget_service.dto.UpdateTransactionRequest;
import com.samjenkins.budget_service.entity.Txn;
import com.samjenkins.budget_service.exception.BadRequestException;
import com.samjenkins.budget_service.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import com.samjenkins.budget_service.repository.CategoryRepository;
import com.samjenkins.budget_service.repository.TxnRepository;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TxnService {

    private final TxnRepository txnRepository;
    private final CategoryRepository categoryRepository;
    private final AlertService alertService;
    private final EntityManager entityManager;

    @Transactional
    public TransactionResponse create(UUID userId, CreateTransactionRequest request) {
        validateAmount(request.amountCents());
        validateCategoryOwnership(userId, request.categoryId());

        Txn txn = Txn.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .categoryId(request.categoryId())
            .merchant(request.merchant().trim())
            .description(trimToNull(request.description()))
            .amountCents(request.amountCents())
            .transactionDate(request.transactionDate())
            .source(request.source())
            .updatedAt(OffsetDateTime.now())
            .build();

        Txn saved = txnRepository.saveAndFlush(txn);
        entityManager.refresh(saved);
        evaluateThresholdsForTxn(userId, null, saved);
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID transactionId, UpdateTransactionRequest request) {
        Txn existing = txnRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new NotFoundException("Transaction not found"));

        Txn before = Txn.builder()
            .categoryId(existing.getCategoryId())
            .transactionDate(existing.getTransactionDate())
            .amountCents(existing.getAmountCents())
            .build();

        if (request.merchant() != null) {
            if (request.merchant().isBlank()) {
                throw new BadRequestException("Merchant cannot be blank");
            }
            existing.setMerchant(request.merchant().trim());
        }
        if (request.description() != null) {
            existing.setDescription(trimToNull(request.description()));
        }
        if (request.amountCents() != null) {
            validateAmount(request.amountCents());
            existing.setAmountCents(request.amountCents());
        }
        if (request.transactionDate() != null) {
            existing.setTransactionDate(request.transactionDate());
        }
        if (request.source() != null) {
            existing.setSource(request.source());
        }
        if (request.categoryId() != null) {
            validateCategoryOwnership(userId, request.categoryId());
            existing.setCategoryId(request.categoryId());
        }

        existing.setUpdatedAt(OffsetDateTime.now());
        Txn saved = txnRepository.save(existing);
        evaluateThresholdsForTxn(userId, before, saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listByMonth(UUID userId, YearMonth month) {
        return txnRepository.findAllByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                userId,
                month.atDay(1),
                month.atEndOfMonth())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void validateAmount(long amountCents) {
        if (amountCents == 0L) {
            throw new BadRequestException("amountCents must be non-zero");
        }
    }

    private void validateCategoryOwnership(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }
        if (!categoryRepository.existsByIdAndUserId(categoryId, userId)) {
            throw new NotFoundException("Category not found");
        }
    }

    private void evaluateThresholdsForTxn(UUID userId, Txn before, Txn after) {
        List<CategoryMonthPair> pairs = new ArrayList<>();
        if (before != null && before.getCategoryId() != null && before.getAmountCents() < 0) {
            pairs.add(new CategoryMonthPair(before.getCategoryId(), YearMonth.from(before.getTransactionDate())));
        }
        if (after.getCategoryId() != null && after.getAmountCents() < 0) {
            pairs.add(new CategoryMonthPair(after.getCategoryId(), YearMonth.from(after.getTransactionDate())));
        }

        pairs.stream().filter(Objects::nonNull).distinct()
            .forEach(pair -> alertService.evaluateBudgetThresholds(userId, pair.categoryId(), pair.month()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TransactionResponse toResponse(Txn txn) {
        return new TransactionResponse(
            txn.getId(),
            txn.getCategoryId(),
            txn.getMerchant(),
            txn.getDescription(),
            txn.getAmountCents(),
            txn.getTransactionDate(),
            txn.getSource(),
            txn.getCreatedAt(),
            txn.getUpdatedAt()
        );
    }

    private record CategoryMonthPair(UUID categoryId, YearMonth month) {}
}
