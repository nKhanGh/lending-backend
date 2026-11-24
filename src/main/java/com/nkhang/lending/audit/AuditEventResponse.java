package com.nkhang.lending.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID id, UUID actorId, UUID subjectUserId,
                                 String action, String resourceType, UUID resourceId,
                                 String details, Instant occurredAt) { }
