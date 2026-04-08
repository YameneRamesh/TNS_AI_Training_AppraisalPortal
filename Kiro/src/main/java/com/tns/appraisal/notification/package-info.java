/**
 * Email notification module.
 * 
 * <p>This package contains components for asynchronous email notification
 * delivery. It supports template-based emails with placeholder substitution,
 * delivery tracking, and failure logging.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>NotificationService - Async email sending with template processing</li>
 *   <li>NotificationTemplateRepository - Data access for email templates</li>
 *   <li>EmailLogRepository - Data access for notification logs</li>
 *   <li>NotificationTemplate entity - Email template with placeholders</li>
 *   <li>EmailNotificationLog entity - Delivery tracking and status</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/notifications/log - View email notification log</li>
 *   <li>GET /api/notifications/templates - List notification templates</li>
 *   <li>PUT /api/notifications/templates/{id} - Update notification template</li>
 * </ul>
 */
package com.tns.appraisal.notification;
