/**
 * Audit logging module.
 * 
 * <p>This package contains components for tamper-evident audit logging of all
 * significant system actions. Audit logs support search and filtering by user,
 * action type, and date range.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>AuditLogService - Async audit log creation</li>
 *   <li>AuditLogRepository - Data access for audit log entities</li>
 *   <li>AuditLog entity - Immutable audit record</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/audit-logs - Search audit logs with filters</li>
 * </ul>
 * 
 * <p>Logged actions include: LOGIN, LOGOUT, FORM_SUBMIT, REVIEW_COMPLETE,
 * ROLE_CHANGE, CYCLE_TRIGGER, FORM_REOPEN, and other significant operations.</p>
 */
package com.tns.appraisal.audit;
