package com.nkhang.lending.audit;

import lombok.RequiredArgsConstructor;
import com.nkhang.lending.common.exception.AppException;
import com.nkhang.lending.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public void record(UUID actorId, UUID subjectUserId, String action,
                       String resourceType, UUID resourceId, String details) {
        jdbc.update("""
                insert into audit_events(id,actor_id,subject_user_id,action,resource_type,resource_id,details,occurred_at)
                values (?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), actorId, subjectUserId, action, resourceType, resourceId,
                details, Timestamp.from(clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recent(UUID subjectUserId, int limit) {
        if (limit < 1 || limit > 100) throw new AppException(ErrorCode.INVALID_REQUEST, "limit must be 1..100");
        return jdbc.query("""
                select id,actor_id,subject_user_id,action,resource_type,resource_id,details,occurred_at
                from audit_events where (?::uuid is null or subject_user_id = ?::uuid)
                order by occurred_at desc, id desc limit ?
                """, (rs, row) -> new AuditEventResponse(
                        rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                        rs.getObject("subject_user_id", UUID.class), rs.getString("action"),
                        rs.getString("resource_type"), rs.getObject("resource_id", UUID.class),
                        rs.getString("details"), rs.getTimestamp("occurred_at").toInstant()),
                subjectUserId, subjectUserId, limit);
    }
}
