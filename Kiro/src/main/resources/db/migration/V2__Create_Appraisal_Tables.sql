-- =====================================================
-- Migration: V2 - Create Appraisal Tables
-- Description: Creates appraisal_templates, appraisal_cycles, and appraisal_forms tables
-- =====================================================

-- Create appraisal_templates table
CREATE TABLE appraisal_templates (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    version     NVARCHAR(50)  NOT NULL,
    schema_json NVARCHAR(MAX) NOT NULL,
    is_active   BIT           NOT NULL DEFAULT 0,
    created_by  BIGINT        REFERENCES users(id),
    created_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT chk_only_one_active_template CHECK (
        is_active = 0 OR 
        NOT EXISTS (
            SELECT 1 FROM appraisal_templates t2 
            WHERE t2.is_active = 1 AND t2.id != appraisal_templates.id
        )
    )
);

-- Create appraisal_cycles table
CREATE TABLE appraisal_cycles (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(200) NOT NULL,
    start_date  DATE          NOT NULL,
    end_date    DATE          NOT NULL,
    template_id BIGINT        NOT NULL REFERENCES appraisal_templates(id),
    status      NVARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    created_by  BIGINT        NOT NULL REFERENCES users(id),
    created_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    updated_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT chk_cycle_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_cycle_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

-- Create appraisal_forms table
CREATE TABLE appraisal_forms (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    cycle_id            BIGINT        NOT NULL REFERENCES appraisal_cycles(id),
    employee_id         BIGINT        NOT NULL REFERENCES users(id),
    manager_id          BIGINT        NOT NULL REFERENCES users(id),
    backup_reviewer_id  BIGINT        REFERENCES users(id),
    template_id         BIGINT        NOT NULL REFERENCES appraisal_templates(id),
    status              NVARCHAR(50)  NOT NULL DEFAULT 'NOT_STARTED',
    form_data           NVARCHAR(MAX),
    submitted_at        DATETIME2,
    review_started_at   DATETIME2,
    reviewed_at         DATETIME2,
    pdf_storage_path    NVARCHAR(500),
    created_at          DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    updated_at          DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT uq_form_cycle_employee UNIQUE (cycle_id, employee_id),
    CONSTRAINT chk_form_status CHECK (
        status IN (
            'NOT_STARTED', 
            'DRAFT_SAVED', 
            'SUBMITTED', 
            'UNDER_REVIEW', 
            'REVIEW_DRAFT_SAVED', 
            'REVIEWED_AND_COMPLETED'
        )
    )
);

-- Create indexes for appraisal_templates table
CREATE INDEX idx_appraisal_templates_is_active ON appraisal_templates(is_active);
CREATE INDEX idx_appraisal_templates_version ON appraisal_templates(version);
CREATE INDEX idx_appraisal_templates_created_by ON appraisal_templates(created_by);

-- Create indexes for appraisal_cycles table
CREATE INDEX idx_appraisal_cycles_status ON appraisal_cycles(status);
CREATE INDEX idx_appraisal_cycles_template_id ON appraisal_cycles(template_id);
CREATE INDEX idx_appraisal_cycles_created_by ON appraisal_cycles(created_by);
CREATE INDEX idx_appraisal_cycles_start_date ON appraisal_cycles(start_date);
CREATE INDEX idx_appraisal_cycles_end_date ON appraisal_cycles(end_date);

-- Create indexes for appraisal_forms table
CREATE INDEX idx_appraisal_forms_cycle_id ON appraisal_forms(cycle_id);
CREATE INDEX idx_appraisal_forms_employee_id ON appraisal_forms(employee_id);
CREATE INDEX idx_appraisal_forms_manager_id ON appraisal_forms(manager_id);
CREATE INDEX idx_appraisal_forms_backup_reviewer_id ON appraisal_forms(backup_reviewer_id);
CREATE INDEX idx_appraisal_forms_template_id ON appraisal_forms(template_id);
CREATE INDEX idx_appraisal_forms_status ON appraisal_forms(status);
CREATE INDEX idx_appraisal_forms_submitted_at ON appraisal_forms(submitted_at);
CREATE INDEX idx_appraisal_forms_reviewed_at ON appraisal_forms(reviewed_at);
