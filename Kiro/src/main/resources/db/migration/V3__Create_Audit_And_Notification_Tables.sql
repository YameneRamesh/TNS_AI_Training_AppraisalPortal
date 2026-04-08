-- =====================================================
-- Migration: V3 - Create Audit and Notification Tables
-- Description: Creates audit_logs, notification_templates, and email_notification_log tables
-- =====================================================

-- Create audit_logs table
CREATE TABLE audit_logs (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT        REFERENCES users(id),
    action      NVARCHAR(100) NOT NULL,
    entity_type NVARCHAR(100),
    entity_id   BIGINT,
    details     NVARCHAR(MAX),
    ip_address  NVARCHAR(50),
    created_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);

-- Create notification_templates table
CREATE TABLE notification_templates (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    trigger_event  NVARCHAR(100) NOT NULL UNIQUE,
    subject        NVARCHAR(500) NOT NULL,
    body_html      NVARCHAR(MAX) NOT NULL,
    updated_by     BIGINT        REFERENCES users(id),
    updated_at     DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);

-- Create email_notification_log table
CREATE TABLE email_notification_log (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    form_id         BIGINT        REFERENCES appraisal_forms(id),
    cycle_id        BIGINT        REFERENCES appraisal_cycles(id),
    recipient_email NVARCHAR(200) NOT NULL,
    subject         NVARCHAR(500) NOT NULL,
    trigger_event   NVARCHAR(100) NOT NULL,
    status          NVARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    error_reason    NVARCHAR(MAX),
    sent_at         DATETIME2,
    created_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT chk_email_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- Create indexes for audit_logs table
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Create indexes for notification_templates table
CREATE INDEX idx_notification_templates_trigger_event ON notification_templates(trigger_event);

-- Create indexes for email_notification_log table
CREATE INDEX idx_email_notification_log_form_id ON email_notification_log(form_id);
CREATE INDEX idx_email_notification_log_cycle_id ON email_notification_log(cycle_id);
CREATE INDEX idx_email_notification_log_recipient_email ON email_notification_log(recipient_email);
CREATE INDEX idx_email_notification_log_trigger_event ON email_notification_log(trigger_event);
CREATE INDEX idx_email_notification_log_status ON email_notification_log(status);
CREATE INDEX idx_email_notification_log_created_at ON email_notification_log(created_at);
