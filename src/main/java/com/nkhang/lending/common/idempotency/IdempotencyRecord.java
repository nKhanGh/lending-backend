package com.nkhang.lending.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "idempotency_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdempotencyRecord {

    @Id
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    IdempotencyOperation operation;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    String requestFingerprint;

    @Column(name = "resource_id")
    UUID resourceId;

    @Column(name = "response_body", columnDefinition = "TEXT")
    String responseBody;

    @Column(name = "http_status")
    Integer httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    IdempotencyStatus status;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    public static IdempotencyRecord processing(
            IdempotencyOperation operation, UUID userId, String key, String fingerprint, Instant now) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.operation = operation;
        record.userId = userId;
        record.idempotencyKey = key;
        record.requestFingerprint = fingerprint;
        record.status = IdempotencyStatus.PROCESSING;
        record.createdAt = now;
        record.updatedAt = now;
        return record;
    }

    public void complete(UUID resourceId, String responseBody, int httpStatus, Instant now) {
        this.resourceId = resourceId;
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
        this.status = IdempotencyStatus.COMPLETED;
        this.updatedAt = now;
    }
}

