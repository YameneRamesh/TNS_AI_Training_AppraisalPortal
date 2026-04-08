package com.tns.appraisal.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Service for managing audit logs with async logging support.
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Log an action asynchronously.
     */
    @Async
    @Transactional
    public void logAsync(Long userId, String action, String entityType, Long entityId, 
                         Map<String, Object> details, String ipAddress) {
        try {
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            AuditLog auditLog = new AuditLog(userId, action, entityType, entityId, detailsJson, ipAddress);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created: action={}, userId={}, entityType={}, entityId={}", 
                        action, userId, entityType, entityId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit log details", e);
        } catch (Exception e) {
            logger.error("Failed to create audit log", e);
        }
    }

    /**
     * Log an action synchronously (for critical operations).
     */
    @Transactional
    public void logSync(Long userId, String action, String entityType, Long entityId, 
                        Map<String, Object> details, String ipAddress) {
        try {
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            AuditLog auditLog = new AuditLog(userId, action, entityType, entityId, detailsJson, ipAddress);
            auditLogRepository.save(auditLog);
            logger.debug("Audit log created (sync): action={}, userId={}, entityType={}, entityId={}", 
                        action, userId, entityType, entityId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit log details", e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }

    /**
     * Search audit logs with filters.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogs(Long userId, String action, Instant startDate, 
                                          Instant endDate, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(userId, action, startDate, endDate, pageable);
    }

    /**
     * Get audit logs for a specific entity.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
