package com.nkhang.lending.common.idempotency;

import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdempotencyService {

    IdempotencyRepository idempotencyRepository;
    EntityManager entityManager;
    Clock clock;

    public IdempotencyRecord begin(
            IdempotencyOperation operation, String rawKey, String fingerprintSource) {
        String key = validateKey(rawKey);
        String fingerprint = sha256(fingerprintSource);

        acquireTransactionLock(operation + ":" + key);

        return idempotencyRepository.findByOperationAndIdempotencyKey(operation, key)
                .map(existing -> validateExisting(existing, fingerprint))
                .orElseGet(() -> idempotencyRepository.save(
                        IdempotencyRecord.processing(operation, key, fingerprint, clock.instant())));
    }

    public void complete(IdempotencyRecord record, java.util.UUID resourceId, int httpStatus) {
        record.complete(resourceId, null, httpStatus, clock.instant());
    }

    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private IdempotencyRecord validateExisting(IdempotencyRecord record, String fingerprint) {
        if (!record.getRequestFingerprint().equals(fingerprint)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        if (record.getStatus() == IdempotencyStatus.PROCESSING) {
            throw new AppException(ErrorCode.REQUEST_IN_PROGRESS);
        }
        return record;
    }

    private String validateKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank() || rawKey.length() > 120) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Idempotency-Key must contain between 1 and 120 characters");
        }
        return rawKey.trim();
    }

    private void acquireTransactionLock(String lockKey) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(?1, 0))")
                .setParameter(1, lockKey)
                .getSingleResult();
    }
}

