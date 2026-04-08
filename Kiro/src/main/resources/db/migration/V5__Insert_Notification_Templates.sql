-- =====================================================
-- Migration: V5 - Insert Initial Notification Templates
-- Description: Creates notification templates for all trigger events
-- =====================================================

-- Template 1: Cycle Triggered - Employee Notification
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'CYCLE_TRIGGERED_EMPLOYEE',
    'Action Required: Complete Your Appraisal for {{cycleName}}',
    '<html>
    <body>
        <p>Dear {{employeeName}},</p>
        <p>The appraisal cycle <strong>{{cycleName}}</strong> has been initiated for the period <strong>{{reviewPeriod}}</strong>.</p>
        <p>Please log in to the Employee Appraisal System to complete your self-appraisal by <strong>{{deadline}}</strong>.</p>
        <p><a href="{{applicationUrl}}">Access Appraisal System</a></p>
        <p>Your manager for this review is: <strong>{{managerName}}</strong></p>
        <p>If you have any questions, please contact your manager or HR.</p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 2: Cycle Triggered - Manager Notification
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'CYCLE_TRIGGERED_MANAGER',
    'Appraisal Cycle {{cycleName}} - Team Members Assigned',
    '<html>
    <body>
        <p>Dear {{managerName}},</p>
        <p>The appraisal cycle <strong>{{cycleName}}</strong> has been initiated for the period <strong>{{reviewPeriod}}</strong>.</p>
        <p>The following team members have been assigned to you for review:</p>
        <ul>
            {{teamMembersList}}
        </ul>
        <p>Please monitor their self-appraisal submissions and complete your reviews by <strong>{{deadline}}</strong>.</p>
        <p><a href="{{applicationUrl}}">Access Appraisal System</a></p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 3: Self-Appraisal Submitted
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'SELF_APPRAISAL_SUBMITTED',
    'Self-Appraisal Submitted - {{employeeName}}',
    '<html>
    <body>
        <p>Dear {{managerName}},</p>
        <p><strong>{{employeeName}}</strong> has submitted their self-appraisal for the cycle <strong>{{cycleName}}</strong>.</p>
        <p>Please log in to the system to review and complete your evaluation.</p>
        <p><a href="{{applicationUrl}}/forms/{{formId}}">View Appraisal Form</a></p>
        <br>
        <p>Best regards,<br>Employee Appraisal System</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 4: Review Completed
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'REVIEW_COMPLETED',
    'Appraisal Review Completed - {{cycleName}}',
    '<html>
    <body>
        <p>Dear {{employeeName}},</p>
        <p>Your manager <strong>{{managerName}}</strong> has completed your appraisal review for the cycle <strong>{{cycleName}}</strong>.</p>
        <p>Please log in to the system to view your completed appraisal.</p>
        <p><a href="{{applicationUrl}}/forms/{{formId}}">View Completed Appraisal</a></p>
        <p>The completed appraisal PDF is attached to this email.</p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 5: Form Reopened
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'FORM_REOPENED',
    'Appraisal Form Reopened - {{cycleName}}',
    '<html>
    <body>
        <p>Dear {{employeeName}},</p>
        <p>Your appraisal form for the cycle <strong>{{cycleName}}</strong> has been reopened by HR.</p>
        <p>You can now edit and resubmit your self-appraisal.</p>
        <p><a href="{{applicationUrl}}/forms/{{formId}}">Access Appraisal Form</a></p>
        <p>Please complete and resubmit by <strong>{{deadline}}</strong>.</p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 6: Backup Reviewer Assigned
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'BACKUP_REVIEWER_ASSIGNED',
    'Backup Reviewer Assignment - {{employeeName}}',
    '<html>
    <body>
        <p>Dear {{backupReviewerName}},</p>
        <p>You have been assigned as a backup reviewer for <strong>{{employeeName}}</strong> in the appraisal cycle <strong>{{cycleName}}</strong>.</p>
        <p>Please log in to the system to complete the review.</p>
        <p><a href="{{applicationUrl}}/forms/{{formId}}">View Appraisal Form</a></p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 7: Reminder - Pending Self-Appraisal
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'REMINDER_PENDING_SELF_APPRAISAL',
    'Reminder: Complete Your Self-Appraisal - {{cycleName}}',
    '<html>
    <body>
        <p>Dear {{employeeName}},</p>
        <p>This is a reminder that your self-appraisal for the cycle <strong>{{cycleName}}</strong> is still pending.</p>
        <p>Please complete and submit your appraisal by <strong>{{deadline}}</strong>.</p>
        <p><a href="{{applicationUrl}}/forms/{{formId}}">Complete Appraisal</a></p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);

-- Template 8: Reminder - Pending Manager Review
INSERT INTO notification_templates (trigger_event, subject, body_html, updated_at)
VALUES (
    'REMINDER_PENDING_MANAGER_REVIEW',
    'Reminder: Pending Reviews - {{cycleName}}',
    '<html>
    <body>
        <p>Dear {{managerName}},</p>
        <p>You have <strong>{{pendingCount}}</strong> pending appraisal review(s) for the cycle <strong>{{cycleName}}</strong>.</p>
        <p>Please complete your reviews by <strong>{{deadline}}</strong>.</p>
        <p><a href="{{applicationUrl}}/dashboard/manager">View Pending Reviews</a></p>
        <br>
        <p>Best regards,<br>Human Resources<br>Think n Solutions</p>
    </body>
    </html>',
    GETUTCDATE()
);
