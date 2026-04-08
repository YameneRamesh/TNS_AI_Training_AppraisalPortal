# Requirements Document

## Introduction

The Employee Appraisal Cycle application is a role-based web platform for Think n Solutions (TnS) that manages the end-to-end annual performance appraisal process. The system supports four roles — Employee, Manager, HR, and Admin — and covers the full lifecycle from cycle configuration and employee self-appraisal through manager review to final completion and historical record-keeping. The appraisal form is based on the TnS Appraisal Form V3.0 (2025-26) and is rendered dynamically from a JSON schema stored in the database.

---

## Glossary

- **System**: The Employee Appraisal Cycle web application
- **Employee**: A TnS staff member who completes a self-appraisal
- **Manager**: A TnS staff member who reviews direct reportees' appraisals
- **HR**: Human Resources personnel who configure and oversee appraisal cycles
- **Admin**: System administrator responsible for user management and system configuration
- **Appraisal_Cycle**: A defined period (e.g., 2025-26) during which appraisals are conducted
- **Appraisal_Form**: The digital representation of the TnS Appraisal Form V3.0, rendered from a JSON schema
- **Appraisal_Template**: The JSON schema definition that drives the structure of an Appraisal_Form
- **Self_Appraisal**: The section of the Appraisal_Form completed by the Employee
- **Manager_Review**: The section of the Appraisal_Form completed by the Manager
- **Rating**: A performance score assigned using the scale: Excels, Exceeds, Meets, Developing (for competency sections) or 1–10 (for policy adherence section)
- **Key_Responsibility**: One of 3–5 essential job duties listed in the Appraisal_Form
- **IDP**: Individual Development Plan — the ownership and development section of the Appraisal_Form
- **Backup_Reviewer**: An alternate Manager or HR delegate assigned to complete a review when the primary reviewer is unavailable
- **Audit_Log**: A tamper-evident record of all significant system actions
- **Notification**: An automated email sent to relevant parties at defined workflow trigger points
- **Dashboard**: A role-specific summary view of appraisal cycle status and actions

---

## Requirements

---

### Requirement 1: Secure Authentication and Session Management

**User Story:** As any user, I want to log in securely and see my basic profile details, so that I can access the features relevant to my role.

#### Acceptance Criteria

1. WHEN a user submits valid credentials, THE System SHALL authenticate the user and establish a session.
2. WHEN a user submits invalid credentials, THE System SHALL display an error message and deny access.
3. WHEN a user is authenticated, THE System SHALL display the logged-in employee's name, designation, department, and manager name on the home screen.
4. WHEN a session is idle for 15 minutes, THE System SHALL invalidate the session and redirect the user to the login page.
5. IF a user attempts to access a protected route without an active session, THEN THE System SHALL redirect the user to the login page.
6. THE System SHALL use HTTP for all communication between the client and server (POC environment only).

---

### Requirement 2: Role-Based Access Control

**User Story:** As a system administrator, I want each user to see only the features and data permitted for their role, so that data security and separation of concerns are maintained.

#### Acceptance Criteria

1. THE System SHALL assign each user exactly one of the following roles: Employee, Manager, HR, or Admin.
2. WHEN a user accesses any page or API endpoint, THE System SHALL verify the user's role and deny access if the role is not authorized for that resource.
3. THE System SHALL allow a user to hold multiple roles simultaneously (e.g., a Manager is also an Employee).
4. IF a user attempts to access a resource outside their role's permissions, THEN THE System SHALL return an access-denied response and log the attempt in the Audit_Log.
5. THE Admin SHALL be able to assign, modify, and revoke role mappings for any user.

---

### Requirement 3: Appraisal Cycle Configuration

**User Story:** As an HR user, I want to configure and trigger an appraisal cycle, so that eligible employees receive their appraisal forms at the right time.

#### Acceptance Criteria

1. THE HR SHALL be able to create a new Appraisal_Cycle by specifying a cycle name, start date, and end date.
2. THE HR SHALL be able to select one or more eligible employees to include in an Appraisal_Cycle.
3. WHEN the HR triggers an Appraisal_Cycle, THE System SHALL create an Appraisal_Form record for each eligible employee using the active Appraisal_Template.
4. WHEN the HR triggers an Appraisal_Cycle, THE System SHALL send a Notification email to each eligible employee with the application URL, CC-ing their respective Manager.
5. WHEN the HR triggers an Appraisal_Cycle, THE System SHALL send a separate Notification email to each Manager listing the eligible team members assigned to them.
6. THE HR SHALL be able to reopen a submitted or completed Appraisal_Form, resetting its status to allow re-submission.
7. THE HR SHALL be able to assign a Backup_Reviewer to substitute for a Manager who is unavailable.
8. THE HR SHALL be able to assign a backup HR delegate to perform HR-level actions on their behalf.
9. THE System SHALL allow the HR to reuse an existing Appraisal_Template structure when creating a new Appraisal_Cycle.
10. THE HR SHALL be able to configure email Notification templates for each workflow trigger point.

---

### Requirement 4: Appraisal Form Structure and Rendering

**User Story:** As an employee or manager, I want the appraisal form to reflect the TnS Appraisal Form V3.0 structure, so that the digital experience matches the established process.

#### Acceptance Criteria

1. THE System SHALL render the Appraisal_Form dynamically from the JSON-based Appraisal_Template stored in the database.
2. THE Appraisal_Form SHALL include a header section containing: Team Member name, Date of Hire, Designation, Date of Review, Manager name, Review Period, and Type of Review.
3. THE Appraisal_Form SHALL include a Rating Key section displaying the four rating levels: Excels, Exceeds, Meets, and Developing, each with its description.
4. THE Appraisal_Form SHALL include an Overall Evaluation Rating section with fields for Manager's Comments and Team Member's Comments.
5. THE Appraisal_Form SHALL include a Key Responsibilities section where 3 to 5 essential duties are listed, each with a Team Member comment field, a Self Rating field, a Manager comment field, and a Manager Rating field.
6. THE Appraisal_Form SHALL include an IDP section with three categories — NextGen Tech Skills, Value Addition, and Leadership — each with a Team Member comment field, a Self Rating field, a Manager comment field, and a Manager Rating field.
7. THE Appraisal_Form SHALL include a Company Policies and Business Continuity Support Adherence section with three items rated on a scale of 1 to 10: Follow HR Policy, Team Member Availability During Critical Deliverables, and Additional Support Beyond Regular Work Assignments, along with a Manager's Comments field.
8. THE Appraisal_Form SHALL include a Goals section containing goals carried over from the previous year and a field for next-year goals, each with Team Member comment, Self Rating, Manager comment, and Manager Rating fields.
9. THE Appraisal_Form SHALL include a Next Year Goals table for documenting upcoming objectives.
10. THE Appraisal_Form SHALL include a signature block for Manager (Prepared/Delivered By, Reviewed By) and a Team Member acknowledgement field.
11. WHERE a historical Appraisal_Cycle is being viewed, THE System SHALL render the Appraisal_Form using the Appraisal_Template version that was active at the time of that cycle.

---

### Requirement 5: Employee Self-Appraisal

**User Story:** As an employee, I want to complete and submit my self-appraisal, so that my manager can review my performance.

#### Acceptance Criteria

1. WHEN an Employee logs in during an active Appraisal_Cycle, THE System SHALL display the assigned Appraisal_Form on the Employee's dashboard.
2. THE Employee SHALL be able to fill in all Self_Appraisal fields including comments and Self Rating for each section.
3. THE Employee SHALL be able to save the Appraisal_Form as a draft without submitting, transitioning the status to "Draft Saved".
4. THE Employee SHALL be able to submit the completed Appraisal_Form, transitioning the status from "Draft Saved" or "Not Started" to "Submitted".
5. WHEN an Employee submits the Appraisal_Form, THE System SHALL record the submission timestamp and make the form available to the assigned Manager.
6. WHILE the Appraisal_Form status is "Submitted" or later, THE System SHALL prevent the Employee from editing the Self_Appraisal fields unless the form is reopened by HR.
7. THE Employee SHALL be able to view all previously submitted Appraisal_Forms from past Appraisal_Cycles in read-only mode.
8. THE System SHALL track the following statuses for an Employee's Appraisal_Form: Not Started, Draft Saved, Submitted.

---

### Requirement 6: Manager Review

**User Story:** As a manager, I want to review my direct reportees' submitted appraisals and record my ratings and comments, so that the appraisal process can be completed.

#### Acceptance Criteria

1. WHEN an Employee submits an Appraisal_Form, THE System SHALL display the submission in the Manager's dashboard under "Pending Reviews".
2. THE Manager SHALL be able to add comments and ratings in all Manager-designated fields of the Appraisal_Form.
3. THE Manager SHALL be able to save the review as a draft, transitioning the status to "Review Draft Saved".
4. THE Manager SHALL be able to complete the review, transitioning the status to "Reviewed and Completed".
5. WHEN a Manager completes a review, THE System SHALL send a Notification email to the Employee, the Manager, and HR, attaching the reviewed Appraisal_Form as a PDF.
6. THE Manager SHALL be able to assign a Backup_Reviewer (immediate lead) to complete a review on their behalf.
7. WHILE reviewing an Appraisal_Form, THE Manager SHALL be able to view the Employee's Self_Appraisal fields in read-only mode alongside the Manager's input fields.
8. THE System SHALL track the following statuses for a Manager's review: Under Review, Review Draft Saved, Reviewed and Completed.
9. THE Manager SHALL be able to complete their own self-appraisal following the same workflow as an Employee.

---

### Requirement 7: Employee Dashboard

**User Story:** As an employee, I want a dashboard that shows my current appraisal status and history, so that I can track my progress and access past records.

#### Acceptance Criteria

1. THE Employee Dashboard SHALL display the current Appraisal_Form with its status and submission deadline.
2. THE Employee Dashboard SHALL display a list of Appraisal_Forms from previous Appraisal_Cycles, each accessible in read-only mode.
3. THE Employee Dashboard SHALL display the current status of the active Appraisal_Form (Not Started, Draft Saved, Submitted, Under Review, Reviewed and Completed).

---

### Requirement 8: Manager Dashboard

**User Story:** As a manager, I want a dashboard that shows my team's appraisal progress, so that I can track pending and completed reviews.

#### Acceptance Criteria

1. THE Manager Dashboard SHALL display the Manager's own current Appraisal_Form and its status.
2. THE Manager Dashboard SHALL display a list of direct reportees with their Appraisal_Form status.
3. THE Manager Dashboard SHALL display a count of pending reviews and completed reviews.
4. THE Manager Dashboard SHALL display the team's overall appraisal completion percentage.
5. THE Manager Dashboard SHALL provide a hyperlink from each team member's entry to their Appraisal_Form.
6. THE Manager Dashboard SHALL display historical Appraisal_Cycles and allow the Manager to view past team appraisals in read-only mode.

---

### Requirement 9: HR Dashboard

**User Story:** As an HR user, I want an organization-wide dashboard, so that I can monitor cycle progress and take corrective actions.

#### Acceptance Criteria

1. THE HR Dashboard SHALL display a list of all eligible employees for the active Appraisal_Cycle with their current status.
2. THE HR Dashboard SHALL display aggregate metrics including: total eligible employees, pending submissions, pending reviews, and completed appraisals.
3. THE HR Dashboard SHALL display department-wise appraisal completion progress.
4. THE HR Dashboard SHALL allow HR to export appraisal data as a report.
5. THE HR Dashboard SHALL display all historical Appraisal_Cycles and allow HR to view any past Appraisal_Form in read-only mode.
6. THE HR Dashboard SHALL display a hyperlink that opens the reviewed Appraisal_Form PDF for any completed appraisal.

---

### Requirement 10: Admin Functions

**User Story:** As an admin, I want to manage users, roles, system configuration, and audit logs, so that the system remains secure and correctly configured.

#### Acceptance Criteria

1. THE Admin SHALL be able to create, update, deactivate, and delete user accounts.
2. THE Admin SHALL be able to assign and revoke role mappings for any user.
3. THE Admin SHALL be able to view and search the Audit_Log by user, action type, and date range.
4. THE Admin SHALL be able to configure email Notification templates for all workflow trigger points.
5. THE Admin SHALL be able to manage system-level configuration parameters.
6. THE System SHALL record an Audit_Log entry for every significant action including: login, logout, form submission, review completion, role change, cycle trigger, and reopen.

---

### Requirement 11: PDF Export of Completed Appraisal

**User Story:** As a manager or HR user, I want to export a completed appraisal as a PDF, so that it can be shared and archived.

#### Acceptance Criteria

1. WHEN a Manager completes a review, THE System SHALL generate a PDF of the completed Appraisal_Form.
2. THE System SHALL attach the generated PDF to the completion Notification email sent to the Employee, Manager, and HR.
3. THE HR SHALL be able to download the PDF of any completed Appraisal_Form from the HR Dashboard.
4. THE Employee SHALL be able to download the PDF of their own completed Appraisal_Form from the Employee Dashboard.
5. THE System SHALL preserve the PDF in association with the Appraisal_Form record for historical access.

---

### Requirement 12: Email Notification Tracking

**User Story:** As an HR user or admin, I want to track the status of all system-generated emails, so that I can confirm notifications were delivered.

#### Acceptance Criteria

1. THE System SHALL log every outbound Notification in the Email Notification Log, recording: recipient, subject, trigger event, timestamp, and delivery status.
2. WHEN an email delivery fails, THE System SHALL update the Email Notification Log with a failure status and the error reason.
3. THE HR SHALL be able to view the Email Notification Log for the active Appraisal_Cycle from the HR Dashboard.

---

### Requirement 13: Historical Appraisal Access

**User Story:** As any user, I want to view appraisals from previous cycles in read-only mode, so that I can reference past performance records.

#### Acceptance Criteria

1. THE System SHALL preserve all Appraisal_Forms and their associated Appraisal_Template versions after an Appraisal_Cycle is closed.
2. WHEN a user opens a historical Appraisal_Form, THE System SHALL render it in read-only mode using the Appraisal_Template version active at the time of that cycle.
3. THE Employee SHALL be able to access only their own historical Appraisal_Forms.
4. THE Manager SHALL be able to access historical Appraisal_Forms for their current and past direct reportees.
5. THE HR SHALL be able to access all historical Appraisal_Forms across all employees and cycles.

---

### Requirement 14: Bulk Trigger Support

**User Story:** As an HR user, I want to trigger appraisals for multiple employees at once, so that the cycle initiation process is efficient.

#### Acceptance Criteria

1. THE HR SHALL be able to select all eligible employees or a subset and trigger the Appraisal_Cycle for all selected employees in a single action.
2. WHEN a bulk trigger is executed, THE System SHALL create Appraisal_Form records and send Notification emails for all selected employees.
3. IF any individual Appraisal_Form creation or Notification fails during a bulk trigger, THEN THE System SHALL log the failure and continue processing the remaining employees without aborting the entire operation.

---

### Requirement 15: Reporting Hierarchy and Backup Assignment

**User Story:** As an HR user or manager, I want the system to respect the reporting hierarchy and support backup assignments, so that reviews are always completed even when a manager is unavailable.

#### Acceptance Criteria

1. THE System SHALL maintain a Reporting_Hierarchy that maps each Employee to their direct Manager.
2. WHEN a Manager is assigned as a Backup_Reviewer for another Manager, THE System SHALL grant the Backup_Reviewer access to the relevant Appraisal_Forms.
3. THE Backup_Reviewer SHALL have the same review permissions as the primary Manager for the assigned Appraisal_Forms.
4. THE HR SHALL be able to update the Reporting_Hierarchy to reflect organizational changes.
