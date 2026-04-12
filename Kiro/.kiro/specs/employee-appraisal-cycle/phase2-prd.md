# Phase 2 PRD: Appraisal Cycle and Template Management

## Overview

Phase 2 implements the Appraisal Cycle and Template Management module for the TNS Employee Appraisal Cycle application. This module enables HR users to manage appraisal templates (JSON-based form schemas), create and configure appraisal cycles, trigger cycles for eligible employees (creating form records), reopen submitted forms, and assign backup reviewers.

**Tech Stack:** Java 21 / Spring Boot 3.x (backend), Angular 21 (frontend), MS SQL Server (database)

**Depends on:** Phase 1 (Auth & User Management) — fully functional  
**Depended on by:** Phase 3 (Form & Review Workflow) — requires AppraisalForm entity from trigger

---

## Requirements Traced to Phase 2

| Requirement | Source | Phase 2 Scope |
|---|---|---|
| Req 3: Appraisal Cycle Configuration | requirements.md | Full — create, trigger, reopen, backup |
| Req 4.1: Dynamic form rendering from template | requirements.md | Template CRUD + JSON schema storage |
| Req 4.11: Historical template versioning | requirements.md | Template versioning + activation |
| Req 14: Bulk Trigger Support | requirements.md | Bulk trigger with partial failure resilience |
| Req 15: Reporting Hierarchy & Backup Assignment | requirements.md | Backup reviewer assignment |

---

## Functional Requirements

### FR-1: Template Management (HR, Admin)

**FR-1.1** The system SHALL store appraisal templates as JSON schemas in the `appraisal_templates` table with version, schema_json, is_active, created_by, and created_at fields.

**FR-1.2** HR/Admin SHALL be able to list all templates via `GET /api/templates`.

**FR-1.3** HR/Admin SHALL be able to view a template's full JSON schema via `GET /api/templates/{id}`.

**FR-1.4** HR/Admin SHALL be able to create a new template via `POST /api/templates` with JSON schema validation.

**FR-1.5** HR/Admin SHALL be able to activate a template via `POST /api/templates/{id}/activate`, which deactivates all other templates (only one active at a time).

**FR-1.6** HR/Admin SHALL be able to deactivate a template via `POST /api/templates/{id}/deactivate`.

**FR-1.7** The JSON schema validator SHALL support the nested `fields` structure used in the TnS Appraisal Form V3.0 template (sections with `items` containing `fields` arrays, not just flat `ratingScale` on items).

**FR-1.8** HR/Admin SHALL be able to create a new version of an existing template via `POST /api/templates/{id}/version`.

### FR-2: Cycle Management (HR)

**FR-2.1** HR SHALL be able to create a new appraisal cycle with name, start_date, end_date, and template_id via `POST /api/cycles`. Status defaults to DRAFT.

**FR-2.2** HR SHALL be able to list all cycles via `GET /api/cycles`.

**FR-2.3** HR SHALL be able to view cycle details via `GET /api/cycles/{id}`.

**FR-2.4** HR SHALL be able to update a DRAFT cycle via `PUT /api/cycles/{id}`.

**FR-2.5** HR SHALL be able to delete a DRAFT cycle via `DELETE /api/cycles/{id}`.

### FR-3: Cycle Trigger (HR)

**FR-3.1** HR SHALL be able to trigger a cycle for selected employees via `POST /api/cycles/{id}/trigger` with a list of employee IDs.

**FR-3.2** WHEN triggered, the system SHALL create one `appraisal_forms` record per employee, referencing the cycle's template (NOT the globally active template).

**FR-3.3** WHEN triggered, the system SHALL set the cycle status to ACTIVE.

**FR-3.4** IF any individual form creation fails, the system SHALL log the failure and continue processing remaining employees (partial failure resilience).

**FR-3.5** The trigger response SHALL include successCount, failureCount, totalEmployees, and a list of failures with employeeId and errorReason.

### FR-4: Form Reopen (HR)

**FR-4.1** HR SHALL be able to reopen a submitted or completed form via `POST /api/cycles/{id}/reopen/{formId}`.

**FR-4.2** WHEN reopened, the form status SHALL reset to DRAFT_SAVED, allowing re-submission.

**FR-4.3** The system SHALL log the reopen action in the audit log.

### FR-5: Backup Reviewer Assignment (HR)

**FR-5.1** HR SHALL be able to assign a backup reviewer to a form via `PUT /api/cycles/{id}/backup-reviewer`.

**FR-5.2** The backup reviewer SHALL be a user with MANAGER or HR role.

**FR-5.3** The system SHALL persist the backup_reviewer_id on the appraisal_forms record.

### FR-6: Cycle Forms Listing

**FR-6.1** HR SHALL be able to view all forms in a cycle via `GET /api/cycles/{id}/forms`.

**FR-6.2** Each form entry SHALL display employee name, employee ID, manager name, backup reviewer name, status, submitted_at, and reviewed_at.

---

## Gap Analysis: Current Implementation vs Requirements

### CRITICAL Issues

| # | Issue | Backend/Frontend | Details |
|---|---|---|---|
| C1 | **No AppraisalForm entity/repository** | Backend | The `form` package contains only `package-info.java`. `CycleService.triggerCycle()` has a placeholder loop that logs but creates NO form records. Tasks 2.2.4 marked done but not implemented. |
| C2 | **reopenForm is a stub** | Backend | `CycleService.reopenForm()` only writes an audit log. No form is loaded, no status is changed, no persistence occurs. |
| C3 | **assignBackupReviewer is a stub** | Backend | `CycleService.assignBackupReviewer()` only writes an audit log. No form row is updated with `backup_reviewer_id`. |
| C4 | **CycleDetails never loads forms** | Frontend | `CycleDetailsComponent.loadForms()` is a placeholder returning empty array. No `getCycleForms` service method exists. |
| C5 | **No GET /api/cycles/{id}/forms endpoint** | Backend | Required for CycleDetails to display forms in a cycle. Not implemented. |

### HIGH Issues

| # | Issue | Backend/Frontend | Details |
|---|---|---|---|
| H1 | **TriggerCycleResult DTO mismatch** | Both | Frontend expects `totalCount`, `reason`, `employeeName`. Backend sends `totalEmployees`, `errorReason`, no `employeeName`. Partial failure display will show undefined values. |
| H2 | **Trigger uses globally active template** | Backend | `triggerCycle()` calls `findByIsActiveTrue()` instead of using `cycle.getTemplate()`. This breaks historical template versioning. |
| H3 | **CycleController hardcoded user** | Backend | `currentUserId = 8L` and `userRoles = List.of("HR")` are hardcoded instead of reading from SecurityContext/session. |
| H4 | **TemplateSchemaValidator incompatible** | Backend | Validator expects flat `ratingScale` on items, but V3.0 template uses nested `fields` arrays inside items. Seed data bypasses the API, so DB can hold JSON the validator would reject. |
| H5 | **Template viewer missing MatTooltipModule** | Frontend | `matTooltip` directive used in template but module not imported. Will cause runtime error. |

### MEDIUM Issues

| # | Issue | Backend/Frontend | Details |
|---|---|---|---|
| M1 | **CycleDashboard retry only reloads dashboard metrics** | Frontend | Error retry button calls `loadDashboard()` not `loadCycles()`. Users can't recover cycle table. |
| M2 | **CycleDashboard no error display for metrics** | Frontend | `loadDashboard` swallows errors silently. |
| M3 | **CycleDetails View/PDF menu items have no handlers** | Frontend | `(click)` handlers missing on View Form and Download PDF menu entries. |
| M4 | **DELETE /api/cycles/{id} not exposed** | Backend | `CycleService.delete()` exists but no `@DeleteMapping` in controller. |
| M5 | **createNewVersion skips schema validation** | Backend | Copies schemaJson from source without calling validator. |
| M6 | **Null safety on triggerCycle** | Backend | If `employeeIds` is null, NPE on `request.getEmployeeIds().size()`. |
| M7 | **Copy to clipboard no feedback** | Frontend | `copyToClipboard()` logs to console but shows no snackbar/toast to user. |
| M8 | **Assign backup reviewer error silent** | Frontend | Load failure shows no snackbar, user sees empty dropdown. |

---

## Implementation Priority

### Batch 1: Backend Foundation (must do first)
1. Create `AppraisalForm` entity and `AppraisalFormRepository` (C1)
2. Implement real `triggerCycle` with form creation using cycle's template (C1, H2)
3. Fix `CycleController` to read user from SecurityContext (H3)
4. Add `GET /api/cycles/{id}/forms` endpoint (C5)
5. Implement real `reopenForm` with status reset (C2)
6. Implement real `assignBackupReviewer` with persistence (C3)
7. Add null safety checks (M6)

### Batch 2: DTO Alignment
8. Align `TriggerCycleResult` field names between frontend and backend (H1)

### Batch 3: Frontend Fixes
9. Fix `TemplateViewerComponent` — add `MatTooltipModule` import (H5)
10. Fix `CycleDashboardComponent` — error retry for both metrics and cycles (M1, M2)
11. Wire `CycleDetailsComponent` with `getCycleForms` service method (C4)
12. Add click handlers for View Form / Download PDF (M3)
13. Add clipboard copy feedback (M7)
14. Add error feedback to backup reviewer dialog (M8)

### Batch 4: Validation
15. Fix `TemplateSchemaValidator` for nested fields format (H4)
16. Add `@DeleteMapping` for cycles (M4)
17. Add validation to `createNewVersion` (M5)

---

## Acceptance Criteria (Given/When/Then)

### AC-1: Cycle Trigger Creates Forms
**Given** a DRAFT cycle with templateId referencing template version 3.0  
**When** HR triggers the cycle for 5 employees  
**Then** 5 `appraisal_forms` records are created with status NOT_STARTED, each referencing the cycle's template (not globally active), and the cycle status changes to ACTIVE

### AC-2: Partial Failure Resilience
**Given** a cycle trigger for 10 employees where 2 have invalid data  
**When** the trigger executes  
**Then** 8 forms are created, 2 failures are logged, and the response includes successCount=8, failureCount=2, totalEmployees=10 with failure details

### AC-3: Form Reopen
**Given** a form in SUBMITTED status  
**When** HR reopens the form  
**Then** the form status resets to DRAFT_SAVED and the employee can edit again

### AC-4: Backup Reviewer Assignment
**Given** a form with a primary manager  
**When** HR assigns a backup reviewer  
**Then** the form's backup_reviewer_id is updated and the backup reviewer can access the form

### AC-5: Cycle Forms Listing
**Given** a triggered cycle with 5 forms  
**When** HR views the cycle details  
**Then** all 5 forms are displayed with employee name, status, and available actions (reopen, assign backup)

### AC-6: Template Activation
**Given** two templates (V2.0 active, V3.0 inactive)  
**When** HR activates V3.0  
**Then** V3.0 becomes active and V2.0 is deactivated (only one active at a time)
