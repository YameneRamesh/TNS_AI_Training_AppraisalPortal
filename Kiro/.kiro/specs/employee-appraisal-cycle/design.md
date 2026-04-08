# Design Document: Employee Appraisal Cycle

## Overview

The Employee Appraisal Cycle application is a role-based web platform for Think n Solutions (TnS) that manages the end-to-end annual performance appraisal process. It supports four roles — Employee, Manager, HR, and Admin — and covers the full lifecycle from cycle configuration and employee self-appraisal through manager review to final completion and historical record-keeping.

The system is built as a Spring Boot REST API backend with an Angular 21 SPA frontend, backed by MS SQL Server. The appraisal form is rendered dynamically from a JSON schema (Appraisal_Template) stored in the database, ensuring the digital form mirrors the TnS Appraisal Form V3.0 structure.

### Key Design Goals

- Role-based access control enforced at both API and UI layers
- Dynamic form rendering from a versioned JSON template
- Full audit trail for all significant actions
- Asynchronous email notification with delivery tracking
- PDF generation on review completion
- Historical form preservation using template versioning

---

## Architecture

The system follows a layered, three-tier architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Angular 21 SPA (Frontend)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ Employee │ │ Manager  │ │    HR    │ │    Admin     │   │
│  │Dashboard │ │Dashboard │ │Dashboard │ │  Dashboard   │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
│              Dynamic Form Renderer (JSON Schema)             │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/REST (JSON)
┌─────────────────────────▼───────────────────────────────────┐
│               Spring Boot 3.x REST API (Backend)             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │  Auth/RBAC   │ │  Appraisal   │ │  Notification        │ │
│  │  Module      │ │  Module      │ │  Module (Async)      │ │
│  └──────────────┘ └──────────────┘ └──────────────────────┘ │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │  User/Admin  │ │  PDF Export  │ │  Audit Log           │ │
│  │  Module      │ │  Module      │ │  Module              │ │
│  └──────────────┘ └──────────────┘ └──────────────────────┘ │
└─────────────────────────┬───────────────────────────────────┘
                          │ JDBC (SQL Server Driver)
┌─────────────────────────▼───────────────────────────────────┐
│                    MS SQL Server Database                     │
│  Users │ Roles │ Cycles │ Templates │ Forms │ Audit │ Email  │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 21, Angular Material, TypeScript |
| Backend | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Database | MS SQL Server |
| PDF Generation | iText 7 (or Apache PDFBox) |
| Email | Spring Mail (JavaMailSender) with async processing |
| Authentication | Session-based (HttpSession) with Spring Security |
| Build | Maven (backend), Angular CLI (frontend) |

### Appraisal Workflow State Machine

```
                    ┌─────────────┐
                    │ Not Started │ ◄── Cycle triggered by HR
                    └──────┬──────┘
                           │ Employee saves draft
                    ┌──────▼──────┐
                    │ Draft Saved │ ◄── Employee can edit
                    └──────┬──────┘
                           │ Employee submits
                    ┌──────▼──────┐
                    │  Submitted  │ ◄── HR can reopen → Draft Saved
                    └──────┬──────┘
                           │ Manager opens review
                    ┌──────▼──────┐
                    │Under Review │ ◄── Manager can save draft
                    └──────┬──────┘
                           │ Manager saves draft
               ┌───────────▼──────────┐
               │ Review Draft Saved   │
               └───────────┬──────────┘
                           │ Manager completes
               ┌───────────▼──────────┐
               │Reviewed and Completed│ ──► PDF generated + Email sent
               └──────────────────────┘
```

---

## Components and Interfaces

### Backend Module Structure

```
com.tns.appraisal
├── config/
│   ├── SecurityConfig.java
│   ├── WebMvcConfig.java
│   └── AsyncConfig.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   └── SessionManager.java
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   └── UserRepository.java
├── cycle/
│   ├── CycleController.java
│   ├── CycleService.java
│   └── CycleRepository.java
├── template/
│   ├── TemplateController.java
│   ├── TemplateService.java
│   └── TemplateRepository.java
├── form/
│   ├── FormController.java
│   ├── FormService.java
│   └── FormRepository.java
├── review/
│   ├── ReviewController.java
│   ├── ReviewService.java
│   └── ReviewRepository.java
├── notification/
│   ├── NotificationService.java
│   ├── EmailLogRepository.java
│   └── NotificationTemplateRepository.java
├── pdf/
│   └── PdfGenerationService.java
├── audit/
│   ├── AuditLogService.java
│   └── AuditLogRepository.java
└── dashboard/
    └── DashboardController.java
```

### REST API Endpoints

#### Authentication
| Method | Path | Description | Roles |
|---|---|---|---|
| POST | `/api/auth/login` | Authenticate user | Public |
| POST | `/api/auth/logout` | Invalidate session | Authenticated |
| GET | `/api/auth/me` | Get current user profile | Authenticated |

#### User Management (Admin)
| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/users` | List all users | Admin |
| POST | `/api/users` | Create user | Admin |
| PUT | `/api/users/{id}` | Update user | Admin |
| DELETE | `/api/users/{id}` | Deactivate user | Admin |
| PUT | `/api/users/{id}/roles` | Assign/revoke roles | Admin |

#### Appraisal Cycles (HR)
| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/cycles` | List all cycles | HR, Admin |
| POST | `/api/cycles` | Create new cycle | HR |
| PUT | `/api/cycles/{id}` | Update cycle | HR |
| POST | `/api/cycles/{id}/trigger` | Trigger cycle (bulk) | HR |
| POST | `/api/cycles/{id}/reopen/{formId}` | Reopen a form | HR |
| PUT | `/api/cycles/{id}/backup-reviewer` | Assign backup reviewer | HR |

#### Appraisal Templates
| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/templates` | List templates | HR, Admin |
| POST | `/api/templates` | Create template | HR, Admin |
| GET | `/api/templates/{id}` | Get template by ID | HR, Admin |

#### Appraisal Forms
| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/forms/my` | Get current user's form | Employee |
| GET | `/api/forms/{id}` | Get form by ID | Employee (own), Manager (team), HR, Admin |
| PUT | `/api/forms/{id}/draft` | Save self-appraisal draft | Employee |
| POST | `/api/forms/{id}/submit` | Submit self-appraisal | Employee |
| PUT | `/api/forms/{id}/review/draft` | Save review draft | Manager |
| POST | `/api/forms/{id}/review/complete` | Complete review | Manager |
| GET | `/api/forms/{id}/pdf` | Download PDF | Employee (own), Manager, HR |
| GET | `/api/forms/history` | Get historical forms | Employee (own), Manager, HR |

#### Dashboards
| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/dashboard/employee` | Employee dashboard data | Employee |
| GET | `/api/dashboard/manager` | Manager dashboard data | Manager |
| GET | `/api/dashboard/hr` | HR dashboard data | HR |

#### Audit & Notifications
| Method | Path | Description | Roles |
|---|---|---|---|
| GET | `/api/audit-logs` | Search audit logs | Admin |
| GET | `/api/notifications/log` | View email notification log | HR, Admin |
| GET | `/api/notifications/templates` | List notification templates | HR, Admin |
| PUT | `/api/notifications/templates/{id}` | Update notification template | HR, Admin |

### Frontend Module Structure

```
src/app/
├── core/
│   ├── auth/           (AuthService, AuthGuard, RoleGuard)
│   ├── interceptors/   (HttpInterceptor for session handling)
│   └── models/         (TypeScript interfaces)
├── shared/
│   ├── components/     (Header, Sidebar, Loading, Confirm Dialog)
│   └── pipes/
├── features/
│   ├── auth/           (Login page)
│   ├── employee/       (Employee dashboard, self-appraisal form)
│   ├── manager/        (Manager dashboard, review form)
│   ├── hr/             (HR dashboard, cycle management)
│   └── admin/          (User management, audit logs)
└── form-renderer/      (Dynamic JSON schema form renderer)
```

### Dynamic Form Renderer

The `form-renderer` module is a key component that interprets the JSON Appraisal_Template and renders the appropriate Angular form controls:

```typescript
// Template section types
type SectionType = 'header' | 'rating_key' | 'key_responsibilities' 
                 | 'idp' | 'policy_adherence' | 'goals' | 'next_year_goals' 
                 | 'overall_evaluation' | 'signature';

interface TemplateSection {
  sectionType: SectionType;
  title: string;
  items?: TemplateItem[];
  fields?: TemplateField[];
}

interface TemplateItem {
  id: string;
  label: string;
  allowSelfRating: boolean;
  allowManagerRating: boolean;
  ratingScale: 'competency' | 'policy_1_10';
}
```

---

## Data Models

### Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────┐       ┌──────────────────┐
│    users     │       │  user_roles  │       │      roles       │
├──────────────┤       ├──────────────┤       ├──────────────────┤
│ id (PK)      │──────<│ user_id (FK) │>──────│ id (PK)          │
│ employee_id  │       │ role_id (FK) │       │ name             │
│ full_name    │       └──────────────┘       └──────────────────┘
│ email        │
│ designation  │       ┌──────────────┐
│ department   │       │  reporting   │
│ manager_id   │──────<│  hierarchy   │
│ is_active    │       ├──────────────┤
│ created_at   │       │ employee_id  │
│ updated_at   │       │ manager_id   │
└──────┬───────┘       │ effective_dt │
       │               └──────────────┘
       │
       │         ┌──────────────────┐       ┌──────────────────────┐
       │         │ appraisal_cycles │       │ appraisal_templates  │
       │         ├──────────────────┤       ├──────────────────────┤
       │         │ id (PK)          │──────>│ id (PK)              │
       │         │ name             │       │ version              │
       │         │ start_date       │       │ schema_json (NVARCHAR │
       │         │ end_date         │       │   MAX)               │
       │         │ template_id (FK) │       │ is_active            │
       │         │ status           │       │ created_at           │
       │         │ created_by (FK)  │       └──────────────────────┘
       │         │ created_at       │
       │         └────────┬─────────┘
       │                  │
       │         ┌────────▼─────────┐
       └────────>│ appraisal_forms  │
                 ├──────────────────┤
                 │ id (PK)          │
                 │ cycle_id (FK)    │
                 │ employee_id (FK) │
                 │ manager_id (FK)  │
                 │ backup_reviewer  │
                 │ template_id (FK) │
                 │ status           │
                 │ form_data (NVARCHAR MAX) │
                 │ submitted_at     │
                 │ reviewed_at      │
                 │ pdf_path         │
                 │ created_at       │
                 │ updated_at       │
                 └──────────────────┘
```

### Database Table Definitions

#### `users`
```sql
CREATE TABLE users (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    employee_id     NVARCHAR(50)  NOT NULL UNIQUE,
    full_name       NVARCHAR(200) NOT NULL,
    email           NVARCHAR(200) NOT NULL UNIQUE,
    password_hash   NVARCHAR(255) NOT NULL,
    designation     NVARCHAR(200),
    department      NVARCHAR(200),
    manager_id      BIGINT REFERENCES users(id),
    is_active       BIT           NOT NULL DEFAULT 1,
    created_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    updated_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);
```

#### `roles`
```sql
CREATE TABLE roles (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE  -- EMPLOYEE, MANAGER, HR, ADMIN
);
```

#### `user_roles`
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id INT    NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

#### `reporting_hierarchy`
```sql
CREATE TABLE reporting_hierarchy (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    employee_id  BIGINT    NOT NULL REFERENCES users(id),
    manager_id   BIGINT    NOT NULL REFERENCES users(id),
    effective_dt DATE      NOT NULL,
    end_dt       DATE,
    created_at   DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);
```

#### `appraisal_templates`
```sql
CREATE TABLE appraisal_templates (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    version     NVARCHAR(50)  NOT NULL,
    schema_json NVARCHAR(MAX) NOT NULL,  -- JSON schema defining form structure
    is_active   BIT           NOT NULL DEFAULT 0,
    created_by  BIGINT        REFERENCES users(id),
    created_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);
```

#### `appraisal_cycles`
```sql
CREATE TABLE appraisal_cycles (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(200) NOT NULL,
    start_date  DATE          NOT NULL,
    end_date    DATE          NOT NULL,
    template_id BIGINT        NOT NULL REFERENCES appraisal_templates(id),
    status      NVARCHAR(50)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT, ACTIVE, CLOSED
    created_by  BIGINT        NOT NULL REFERENCES users(id),
    created_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    updated_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);
```

#### `appraisal_forms`
```sql
CREATE TABLE appraisal_forms (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    cycle_id            BIGINT        NOT NULL REFERENCES appraisal_cycles(id),
    employee_id         BIGINT        NOT NULL REFERENCES users(id),
    manager_id          BIGINT        NOT NULL REFERENCES users(id),
    backup_reviewer_id  BIGINT        REFERENCES users(id),
    template_id         BIGINT        NOT NULL REFERENCES appraisal_templates(id),
    status              NVARCHAR(50)  NOT NULL DEFAULT 'NOT_STARTED',
    form_data           NVARCHAR(MAX),  -- JSON blob of all form field values
    submitted_at        DATETIME2,
    review_started_at   DATETIME2,
    reviewed_at         DATETIME2,
    pdf_storage_path    NVARCHAR(500),
    created_at          DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    updated_at          DATETIME2     NOT NULL DEFAULT GETUTCDATE(),
    CONSTRAINT uq_form_cycle_employee UNIQUE (cycle_id, employee_id)
);
```

#### `audit_logs`
```sql
CREATE TABLE audit_logs (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT        REFERENCES users(id),
    action      NVARCHAR(100) NOT NULL,  -- LOGIN, LOGOUT, FORM_SUBMIT, REVIEW_COMPLETE, etc.
    entity_type NVARCHAR(100),
    entity_id   BIGINT,
    details     NVARCHAR(MAX),           -- JSON with additional context
    ip_address  NVARCHAR(50),
    created_at  DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);
```

#### `notification_templates`
```sql
CREATE TABLE notification_templates (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    trigger_event  NVARCHAR(100) NOT NULL UNIQUE,  -- CYCLE_TRIGGERED, REVIEW_COMPLETED, etc.
    subject        NVARCHAR(500) NOT NULL,
    body_html      NVARCHAR(MAX) NOT NULL,          -- Supports {{placeholders}}
    updated_by     BIGINT        REFERENCES users(id),
    updated_at     DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);
```

#### `email_notification_log`
```sql
CREATE TABLE email_notification_log (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    form_id         BIGINT        REFERENCES appraisal_forms(id),
    cycle_id        BIGINT        REFERENCES appraisal_cycles(id),
    recipient_email NVARCHAR(200) NOT NULL,
    subject         NVARCHAR(500) NOT NULL,
    trigger_event   NVARCHAR(100) NOT NULL,
    status          NVARCHAR(50)  NOT NULL DEFAULT 'PENDING',  -- PENDING, SENT, FAILED
    error_reason    NVARCHAR(MAX),
    sent_at         DATETIME2,
    created_at      DATETIME2     NOT NULL DEFAULT GETUTCDATE()
);
```

### Form Data JSON Schema

The `form_data` column in `appraisal_forms` stores a JSON blob that captures all field values. The structure mirrors the Appraisal_Template schema:

```json
{
  "header": {
    "dateOfHire": "2020-01-15",
    "dateOfReview": "2025-04-01",
    "reviewPeriod": "2025-26",
    "typeOfReview": "Annual"
  },
  "keyResponsibilities": [
    {
      "itemId": "kr_1",
      "selfComment": "...",
      "selfRating": "Meets",
      "managerComment": "...",
      "managerRating": "Exceeds"
    }
  ],
  "idp": [
    {
      "itemId": "idp_nextgen",
      "selfComment": "...",
      "selfRating": "Excels",
      "managerComment": "...",
      "managerRating": "Excels"
    }
  ],
  "policyAdherence": {
    "hrPolicy": { "managerRating": 8 },
    "availability": { "managerRating": 9 },
    "additionalSupport": { "managerRating": 7 },
    "managerComments": "..."
  },
  "goals": [
    {
      "itemId": "goal_1",
      "selfComment": "...",
      "selfRating": "Meets",
      "managerComment": "...",
      "managerRating": "Meets"
    }
  ],
  "nextYearGoals": "...",
  "overallEvaluation": {
    "managerComments": "...",
    "teamMemberComments": "..."
  },
  "signature": {
    "preparedBy": "...",
    "reviewedBy": "...",
    "teamMemberAcknowledgement": "..."
  }
}
```

### Appraisal Template JSON Schema

The `schema_json` column in `appraisal_templates` defines the form structure:

```json
{
  "version": "3.0",
  "sections": [
    {
      "sectionType": "key_responsibilities",
      "title": "Key Responsibilities",
      "items": [
        { "id": "kr_1", "label": "Essential Duty 1", "ratingScale": "competency" },
        { "id": "kr_2", "label": "Essential Duty 2", "ratingScale": "competency" }
      ]
    },
    {
      "sectionType": "idp",
      "title": "Individual Development Plan",
      "items": [
        { "id": "idp_nextgen", "label": "NextGen Tech Skills", "ratingScale": "competency" },
        { "id": "idp_value", "label": "Value Addition", "ratingScale": "competency" },
        { "id": "idp_leadership", "label": "Leadership", "ratingScale": "competency" }
      ]
    },
    {
      "sectionType": "policy_adherence",
      "title": "Company Policies and Business Continuity",
      "items": [
        { "id": "policy_hr", "label": "Follow HR Policy", "ratingScale": "policy_1_10" },
        { "id": "policy_avail", "label": "Team Member Availability During Critical Deliverables", "ratingScale": "policy_1_10" },
        { "id": "policy_support", "label": "Additional Support Beyond Regular Work Assignments", "ratingScale": "policy_1_10" }
      ]
    }
  ]
}
```

---

## Correctness Properties


*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

**Property Reflection:** After reviewing all testable criteria, the following consolidations were made:
- 1.1 (valid login) and 1.2 (invalid login) are complementary and can be expressed as a single authentication property.
- 5.3, 5.4, 6.3, 6.4 all test state transitions in the appraisal workflow — consolidated into one state machine property.
- 7.1–7.3 (employee dashboard) and 8.1–8.6 (manager dashboard) are both API response completeness properties — consolidated into one dashboard completeness property.
- 3.3, 3.4, 3.5 (cycle trigger effects) are related but test distinct invariants (form creation vs. email counts) — kept separate.
- 4.1–4.10 (form structure) are all about form rendering completeness — consolidated into one property.
- 13.1 and 13.2 (historical preservation) overlap with 4.11 (template versioning) — consolidated into one historical access property.
- 15.2 and 15.3 (backup reviewer) are closely related — consolidated into one backup reviewer property.

### Property 1: Authentication Correctness

*For any* user credential pair, authentication SHALL succeed if and only if the credentials match a valid active user account — establishing a session on success and returning an error with no session on failure.

**Validates: Requirements 1.1, 1.2**

### Property 2: Session Protection of All Endpoints

*For any* protected API endpoint, a request made without a valid active session SHALL receive a 401 Unauthorized response and SHALL NOT return any protected data.

**Validates: Requirements 1.5**

### Property 3: Role-Based Access Enforcement

*For any* API endpoint and any authenticated user, the response SHALL be 403 Forbidden if the user's role(s) are not in the authorized set for that endpoint, and SHALL be a successful response if the user's role(s) are authorized.

**Validates: Requirements 2.2, 2.4**

### Property 4: Audit Log Completeness

*For any* significant action performed in the system (login, logout, form submission, review completion, role change, cycle trigger, reopen), an audit log entry SHALL be created containing the correct action type, user ID, entity type, entity ID, and timestamp.

**Validates: Requirements 2.4, 10.6**

### Property 5: Role Assignment Round Trip

*For any* user and any valid role, assigning the role SHALL result in the user having that role; revoking the role SHALL result in the user no longer having that role.

**Validates: Requirements 2.5**

### Property 6: Cycle Trigger Creates Exactly One Form Per Employee

*For any* appraisal cycle triggered with a set of N eligible employees, the system SHALL create exactly N appraisal form records — one per employee — each referencing the active Appraisal_Template at the time of triggering.

**Validates: Requirements 3.3**

### Property 7: Cycle Trigger Notification Completeness

*For any* appraisal cycle triggered with N eligible employees having M distinct managers, the system SHALL log exactly N employee notification emails and exactly M manager notification emails in the email notification log.

**Validates: Requirements 3.4, 3.5**

### Property 8: Form Reopen Resets Status

*For any* appraisal form in Submitted or Reviewed_and_Completed status, after HR reopens it, the form status SHALL be reset to a state that allows re-submission (Draft_Saved or Not_Started).

**Validates: Requirements 3.6**

### Property 9: Dynamic Form Rendering Completeness

*For any* valid Appraisal_Template JSON with a defined set of sections and items, the rendered Appraisal_Form SHALL contain all sections defined in the template with all required fields (self-comment, self-rating, manager-comment, manager-rating) present for each item.

**Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10**

### Property 10: Historical Form Uses Correct Template Version

*For any* historical appraisal form, the template used to render it SHALL match the template_id recorded on the form at the time of cycle creation — not the currently active template.

**Validates: Requirements 4.11, 13.1, 13.2**

### Property 11: Appraisal Workflow State Machine Validity

*For any* appraisal form, the status SHALL only transition through valid state paths (Not_Started → Draft_Saved → Submitted → Under_Review → Review_Draft_Saved → Reviewed_and_Completed), and each transition SHALL only be performable by the authorized role (Employee for self-appraisal transitions, Manager for review transitions, HR for reopen).

**Validates: Requirements 5.3, 5.4, 5.6, 5.8, 6.3, 6.4, 6.8**

### Property 12: Employee Cannot Edit Submitted Form

*For any* appraisal form in Submitted, Under_Review, Review_Draft_Saved, or Reviewed_and_Completed status, an employee's attempt to modify self-appraisal fields SHALL be rejected with a 403 Forbidden response.

**Validates: Requirements 5.6**

### Property 13: Review Completion Triggers Notifications

*For any* appraisal form that transitions to Reviewed_and_Completed status, the system SHALL log exactly 3 notification email entries in the email notification log — one each for the employee, the manager, and HR.

**Validates: Requirements 6.5**

### Property 14: Dashboard API Response Completeness

*For any* authenticated user, the dashboard API response SHALL contain all required fields for their role: employees receive current form with status and history; managers receive own form, team list with statuses, pending/completed counts, and completion percentage.

**Validates: Requirements 7.1, 7.2, 7.3, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6**

### Property 15: Audit Log Search Filter Correctness

*For any* combination of search filters (user, action type, date range) applied to the audit log, every returned entry SHALL satisfy all applied filter criteria — no entries outside the filter scope SHALL be returned.

**Validates: Requirements 10.3**

### Property 16: PDF Generation on Review Completion

*For any* appraisal form that transitions to Reviewed_and_Completed status, the system SHALL generate a non-empty PDF and persist its storage path on the form record.

**Validates: Requirements 11.1, 11.5**

### Property 17: Email Notification Log Completeness

*For any* outbound notification triggered by the system, an email_notification_log entry SHALL be created containing recipient, subject, trigger event, timestamp, and delivery status — and if delivery fails, the entry SHALL be updated with FAILED status and the error reason.

**Validates: Requirements 12.1, 12.2**

### Property 18: Bulk Trigger Partial Failure Resilience

*For any* bulk cycle trigger operation where K out of N employee form creations fail, the system SHALL create N-K forms successfully, log K failure entries, and complete the operation without aborting — leaving no employee silently unprocessed.

**Validates: Requirements 14.3**

### Property 19: Backup Reviewer Permission Equivalence

*For any* backup reviewer assignment, the backup reviewer SHALL have exactly the same review permissions as the primary manager for the assigned appraisal forms — able to perform all review actions (save draft, complete review) that the primary manager can perform.

**Validates: Requirements 15.2, 15.3**

---

## Error Handling

### HTTP Error Response Format

All API errors return a consistent JSON structure:

```json
{
  "timestamp": "2025-04-01T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient role",
  "path": "/api/forms/42/review/complete"
}
```

### Error Scenarios and Handling

| Scenario | HTTP Status | Behavior |
|---|---|---|
| Invalid credentials | 401 | Return error message, no session created |
| No active session | 401 | Return 401, frontend redirects to login |
| Insufficient role | 403 | Return 403, log to audit_log |
| Resource not found | 404 | Return 404 with descriptive message |
| Invalid state transition | 409 | Return 409 Conflict with current status |
| Validation failure | 400 | Return 400 with field-level error details |
| Email delivery failure | — | Log failure in email_notification_log, do not throw to caller |
| PDF generation failure | 500 | Log error, mark form with generation_failed flag, alert HR |
| Bulk trigger partial failure | — | Log individual failures, continue processing, return summary |

### State Transition Validation

The `FormService` enforces valid state transitions using a state machine:

```java
private static final Map<FormStatus, Set<FormStatus>> VALID_TRANSITIONS = Map.of(
    FormStatus.NOT_STARTED,          Set.of(FormStatus.DRAFT_SAVED, FormStatus.SUBMITTED),
    FormStatus.DRAFT_SAVED,          Set.of(FormStatus.SUBMITTED),
    FormStatus.SUBMITTED,            Set.of(FormStatus.UNDER_REVIEW, FormStatus.DRAFT_SAVED), // DRAFT_SAVED via HR reopen
    FormStatus.UNDER_REVIEW,         Set.of(FormStatus.REVIEW_DRAFT_SAVED, FormStatus.REVIEWED_AND_COMPLETED),
    FormStatus.REVIEW_DRAFT_SAVED,   Set.of(FormStatus.REVIEWED_AND_COMPLETED),
    FormStatus.REVIEWED_AND_COMPLETED, Set.of(FormStatus.DRAFT_SAVED) // via HR reopen
);
```

Any attempt to transition to an invalid state throws an `InvalidStateTransitionException` which maps to HTTP 409.

### Session Timeout

Spring Security is configured with a 15-minute session timeout. On expiry, the `SessionExpiredStrategy` returns a 401 response. The Angular `HttpInterceptor` catches 401 responses and redirects to the login page.

---

## Testing Strategy

### Dual Testing Approach

The testing strategy combines unit/integration tests for specific scenarios with property-based tests for universal invariants.

### Property-Based Testing

**Library**: [jqwik](https://jqwik.net/) for Java 21 property-based testing.

Each property test runs a minimum of **100 iterations** with randomly generated inputs. Tests are tagged with the design property they validate.

**Tag format**: `Feature: employee-appraisal-cycle, Property {N}: {property_text}`

Example property test structure:

```java
@Property(tries = 100)
@Label("Feature: employee-appraisal-cycle, Property 3: Role-Based Access Enforcement")
void roleBasedAccessEnforcement(
    @ForAll("validEndpoints") String endpoint,
    @ForAll("unauthorizedRoles") UserRole role
) {
    // Arrange: create user with given role
    // Act: call endpoint
    // Assert: response is 403
}
```

### Unit Tests

Focus on:
- State machine transition logic (`FormService`)
- Form data JSON serialization/deserialization round trips
- Notification template placeholder substitution
- PDF content structure validation
- Audit log entry construction
- Dashboard aggregation calculations (completion percentage, counts)

### Integration Tests

Focus on:
- Full authentication flow (login → session → logout)
- End-to-end appraisal workflow (trigger → self-appraisal → review → completion)
- Email notification delivery (with mock SMTP)
- PDF generation and storage
- Bulk trigger with partial failures (with injected failures)
- Session timeout behavior (with mocked clock)

### Frontend Testing

- **Unit tests**: Angular component logic, form renderer with mock templates, service calls
- **E2E tests** (Cypress or Playwright): Full workflow scenarios per role

### Test Coverage Targets

| Layer | Target |
|---|---|
| Backend service layer | ≥ 80% line coverage |
| Property-based tests | 100 iterations per property, 19 properties |
| API integration tests | All endpoints covered |
| Frontend unit tests | ≥ 70% line coverage |
