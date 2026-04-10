# Task List: Employee Appraisal Cycle

## Phase 0: Project Setup and Foundation (Lead Developer)

This phase must be completed first by the lead developer before other phases can begin.

### 0.1 Backend Project Setup
- [x] 0.1.1 Create Spring Boot 3.x project with Java 21
- [x] 0.1.2 Configure Maven dependencies (Spring Web, Spring Security, Spring Data JPA, SQL Server driver, Spring Mail, iText 7, jqwik)
- [x] 0.1.3 Configure application.properties for MS SQL Server connection
- [x] 0.1.4 Set up package structure (auth, user, cycle, template, form, review, notification, pdf, audit, dashboard)
- [x] 0.1.5 Create base configuration classes (SecurityConfig, WebMvcConfig, AsyncConfig)
- [x] 0.1.6 Set up exception handling infrastructure (GlobalExceptionHandler, custom exceptions)
- [x] 0.1.7 Configure session management (15-minute timeout)

### 0.2 Database Schema Setup
- [x] 0.2.1 Create database migration scripts for all tables (users, roles, user_roles, reporting_hierarchy)
- [x] 0.2.2 Create database migration scripts (appraisal_templates, appraisal_cycles, appraisal_forms)
- [x] 0.2.3 Create database migration scripts (audit_logs, notification_templates, email_notification_log)
- [x] 0.2.4 Create indexes for performance (foreign keys, status columns, date ranges)
- [x] 0.2.5 Insert seed data for roles (EMPLOYEE, MANAGER, HR, ADMIN)
- [x] 0.2.6 Create initial notification templates for all trigger events

### 0.3 Frontend Project Setup
- [x] 0.3.1 Create Angular 21 project with Angular CLI
- [x] 0.3.2 Install Angular Material and configure theme
- [x] 0.3.3 Set up project structure (core, shared, features, form-renderer modules)
- [x] 0.3.4 Create TypeScript interfaces for all domain models
- [x] 0.3.5 Set up HTTP interceptor for session handling and error responses
- [x] 0.3.6 Create AuthGuard and RoleGuard for route protection
- [x] 0.3.7 Set up shared components (Header, Sidebar, Loading, ConfirmDialog)

### 0.4 Common Backend Infrastructure
- [x] 0.4.1 Create base entity classes and JPA repositories
- [x] 0.4.2 Implement AuditLogService with async logging
- [x] 0.4.3 Create common DTOs and mappers
- [x] 0.4.4 Set up API response wrapper classes
- [x] 0.4.5 Configure CORS for frontend-backend communication

---

## Phase 1: Authentication and User Management (Developer 1)

### 1.1 Authentication Module
- [ ] 1.1.1 Create User entity and UserRepository
- [ ] 1.1.2 Create Role entity and RoleRepository
- [ ] 1.1.3 Implement AuthService (login, logout, session management)
- [ ] 1.1.4 Create AuthController with /api/auth/login, /api/auth/logout, /api/auth/me endpoints
- [ ] 1.1.5 Implement password hashing with BCrypt
- [ ] 1.1.6 Configure Spring Security for session-based authentication
- [ ] 1.1.7 Implement session timeout handling

### 1.2 User Management Module (Admin)
- [ ] 1.2.1 Create UserService with CRUD operations
- [ ] 1.2.2 Create UserController with /api/users endpoints
- [ ] 1.2.3 Implement role assignment/revocation logic
- [ ] 1.2.4 Create ReportingHierarchy entity and repository
- [ ] 1.2.5 Implement reporting hierarchy management

### 1.3 Frontend Authentication
- [ ] 1.3.1 Create AuthService in Angular
- [ ] 1.3.2 Implement login page component
- [ ] 1.3.3 Implement session management and auto-logout on 401
- [ ] 1.3.4 Create user profile display component
- [ ] 1.3.5 Implement route guards (AuthGuard, RoleGuard)

### 1.4 Frontend Admin Module
- [ ] 1.4.1 Create user management dashboard component
- [ ] 1.4.2 Implement user list with search and filter
- [ ] 1.4.3 Create user create/edit form
- [ ] 1.4.4 Implement role assignment UI
- [ ] 1.4.5 Create audit log viewer component

### 1.5 Testing - Authentication and User Management
- [ ] 1.5.1 Write property test: Authentication Correctness (Property 1)
- [ ] 1.5.2 Write property test: Session Protection (Property 2)
- [ ] 1.5.3 Write property test: Role-Based Access Enforcement (Property 3)
- [ ] 1.5.4 Write property test: Role Assignment Round Trip (Property 5)
- [ ] 1.5.5 Write unit tests for AuthService and UserService
- [ ] 1.5.6 Write integration tests for authentication flow
- [ ] 1.5.7 Write Angular unit tests for auth components

---

## Phase 2: Appraisal Cycle and Template Management (Developer 2)

### 2.1 Template Module
- [x] 2.1.1 Create AppraisalTemplate entity and repository
- [x] 2.1.2 Implement TemplateService (CRUD, versioning, activation)
- [x] 2.1.3 Create TemplateController with /api/templates endpoints
- [x] 2.1.4 Implement JSON schema validation for template structure
- [x] 2.1.5 Create default TnS Appraisal Form V3.0 template in JSON

### 2.2 Cycle Module
- [x] 2.2.1 Create AppraisalCycle entity and repository
- [x] 2.2.2 Implement CycleService (CRUD, trigger, reopen)
- [x] 2.2.3 Create CycleController with /api/cycles endpoints
- [x] 2.2.4 Implement bulk cycle trigger logic
- [x] 2.2.5 Implement form reopen logic
- [x] 2.2.6 Implement backup reviewer assignment

### 2.3 Frontend Template Management
- [x] 2.3.1 Create template list component
- [x] 2.3.2 Create template viewer component (JSON display)
- [x] 2.3.3 Implement template activation UI

### 2.4 Frontend Cycle Management (HR)
- [x] 2.4.1 Create cycle management dashboard
- [x] 2.4.2 Create cycle creation form
- [x] 2.4.3 Implement employee selection UI for cycle trigger
- [x] 2.4.4 Create bulk trigger confirmation dialog
- [x] 2.4.5 Implement form reopen UI
- [x] 2.4.6 Create backup reviewer assignment UI

### 2.5 Testing - Cycle and Template
- [x] 2.5.1 Write property test: Cycle Trigger Creates Exactly One Form Per Employee (Property 6)
- [x] 2.5.2 Write property test: Form Reopen Resets Status (Property 8)
- [x] 2.5.3 Write property test: Historical Form Uses Correct Template Version (Property 10)
- [x] 2.5.4 Write property test: Bulk Trigger Partial Failure Resilience (Property 18)
- [x] 2.5.5 Write unit tests for TemplateService and CycleService
- [x] 2.5.6 Write integration tests for cycle trigger workflow
- [x] 2.5.7 Write Angular unit tests for cycle management components

---

## Phase 3: Appraisal Form and Review Workflow (Developer 3)

### 3.1 Form Module
- [ ] 3.1.1 Create AppraisalForm entity and repository
- [ ] 3.1.2 Implement FormService (CRUD, state transitions, validation)
- [ ] 3.1.3 Create FormController with /api/forms endpoints
- [ ] 3.1.4 Implement form data JSON serialization/deserialization
- [ ] 3.1.5 Implement state machine for form status transitions
- [ ] 3.1.6 Implement draft save logic (employee)
- [ ] 3.1.7 Implement form submission logic (employee)

### 3.2 Review Module
- [ ] 3.2.1 Implement ReviewService (review operations, completion)
- [ ] 3.2.2 Create ReviewController with /api/forms/{id}/review endpoints
- [ ] 3.2.3 Implement review draft save logic (manager)
- [ ] 3.2.4 Implement review completion logic (manager)
- [ ] 3.2.5 Implement backup reviewer permission logic

### 3.3 Frontend Form Renderer
- [ ] 3.3.1 Create dynamic form renderer service (interprets JSON template)
- [ ] 3.3.2 Implement section renderers (header, rating_key, key_responsibilities, idp, policy_adherence, goals, signature)
- [ ] 3.3.3 Create form field components (text, textarea, rating selector)
- [ ] 3.3.4 Implement form validation logic
- [ ] 3.3.5 Create form navigation and progress indicator

### 3.4 Frontend Employee Module
- [ ] 3.4.1 Create employee dashboard component
- [ ] 3.4.2 Create self-appraisal form component
- [ ] 3.4.3 Implement draft save functionality
- [ ] 3.4.4 Implement form submission with confirmation
- [ ] 3.4.5 Create historical forms viewer

### 3.5 Frontend Manager Module
- [ ] 3.5.1 Create manager dashboard component
- [ ] 3.5.2 Create team appraisal list component
- [ ] 3.5.3 Create review form component
- [ ] 3.5.4 Implement review draft save functionality
- [ ] 3.5.5 Implement review completion with confirmation
- [ ] 3.5.6 Create manager's own self-appraisal view

### 3.6 Testing - Form and Review
- [ ] 3.6.1 Write property test: Appraisal Workflow State Machine Validity (Property 11)
- [ ] 3.6.2 Write property test: Employee Cannot Edit Submitted Form (Property 12)
- [ ] 3.6.3 Write property test: Dynamic Form Rendering Completeness (Property 9)
- [ ] 3.6.4 Write property test: Backup Reviewer Permission Equivalence (Property 19)
- [ ] 3.6.5 Write unit tests for FormService and ReviewService
- [ ] 3.6.6 Write unit tests for form data JSON round trips
- [ ] 3.6.7 Write integration tests for end-to-end appraisal workflow
- [ ] 3.6.8 Write Angular unit tests for form renderer
- [ ] 3.6.9 Write Angular unit tests for employee and manager components

---

## Phase 4: Notifications, PDF, Dashboards, and Audit (Developer 4)

### 4.1 Notification Module
- [ ] 4.1.1 Create NotificationTemplate entity and repository
- [ ] 4.1.2 Create EmailNotificationLog entity and repository
- [ ] 4.1.3 Implement NotificationService with async email sending
- [ ] 4.1.4 Configure Spring Mail with JavaMailSender
- [ ] 4.1.5 Implement template placeholder substitution
- [ ] 4.1.6 Implement notification logging and failure tracking
- [ ] 4.1.7 Create notification template management endpoints

### 4.2 PDF Generation Module
- [ ] 4.2.1 Implement PdfGenerationService using iText 7
- [ ] 4.2.2 Create PDF template matching TnS Appraisal Form V3.0 layout
- [ ] 4.2.3 Implement form data to PDF rendering
- [ ] 4.2.4 Implement PDF storage and retrieval
- [ ] 4.2.5 Create /api/forms/{id}/pdf download endpoint

### 4.3 Dashboard Module
- [ ] 4.3.1 Implement DashboardService (aggregations, metrics)
- [ ] 4.3.2 Create DashboardController with role-specific endpoints
- [ ] 4.3.3 Implement employee dashboard data aggregation
- [ ] 4.3.4 Implement manager dashboard data aggregation (team stats, completion %)
- [ ] 4.3.5 Implement HR dashboard data aggregation (org-wide stats)

### 4.4 Audit Module
- [ ] 4.4.1 Implement audit log search and filter logic
- [ ] 4.4.2 Create /api/audit-logs endpoint with pagination
- [ ] 4.4.3 Integrate audit logging into all significant actions

### 4.5 Frontend HR Dashboard
- [ ] 4.5.1 Create HR dashboard component
- [ ] 4.5.2 Implement organization-wide appraisal status view
- [ ] 4.5.3 Create department-wise progress charts
- [ ] 4.5.4 Implement appraisal data export functionality
- [ ] 4.5.5 Create email notification log viewer

### 4.6 Frontend Notification Management
- [ ] 4.6.1 Create notification template list component
- [ ] 4.6.2 Create notification template editor
- [ ] 4.6.3 Implement template preview functionality

### 4.7 Testing - Notifications, PDF, Dashboards
- [ ] 4.7.1 Write property test: Cycle Trigger Notification Completeness (Property 7)
- [ ] 4.7.2 Write property test: Review Completion Triggers Notifications (Property 13)
- [ ] 4.7.3 Write property test: Email Notification Log Completeness (Property 17)
- [ ] 4.7.4 Write property test: PDF Generation on Review Completion (Property 16)
- [ ] 4.7.5 Write property test: Dashboard API Response Completeness (Property 14)
- [ ] 4.7.6 Write property test: Audit Log Completeness (Property 4)
- [ ] 4.7.7 Write property test: Audit Log Search Filter Correctness (Property 15)
- [ ] 4.7.8 Write unit tests for NotificationService and PdfGenerationService
- [ ] 4.7.9 Write unit tests for dashboard aggregation calculations
- [ ] 4.7.10 Write integration tests for email notification with mock SMTP
- [ ] 4.7.11 Write integration tests for PDF generation and storage
- [ ] 4.7.12 Write Angular unit tests for dashboard components

---

## Phase 5: Integration, E2E Testing, and Deployment (All Developers)

### 5.1 Integration Testing
- [ ] 5.1.1 Write end-to-end integration test: Full authentication flow
- [ ] 5.1.2 Write end-to-end integration test: Complete appraisal workflow (trigger → self-appraisal → review → completion)
- [ ] 5.1.3 Write end-to-end integration test: Bulk trigger with partial failures
- [ ] 5.1.4 Write end-to-end integration test: Session timeout behavior
- [ ] 5.1.5 Write end-to-end integration test: Historical form access with template versioning

### 5.2 Frontend E2E Testing
- [ ] 5.2.1 Set up Vitest (default in Angular 21) and Cypress for E2E testing
- [ ] 5.2.2 Write E2E test: Employee login and self-appraisal submission
- [ ] 5.2.3 Write E2E test: Manager login and review completion
- [ ] 5.2.4 Write E2E test: HR cycle trigger and monitoring
- [ ] 5.2.5 Write E2E test: Admin user management and audit log viewing

### 5.3 Performance and Security Testing
- [ ] 5.3.1 Perform load testing on bulk cycle trigger (1000+ employees)
- [ ] 5.3.2 Test session timeout and concurrent session handling
- [ ] 5.3.3 Perform security audit (SQL injection, XSS, CSRF protection)
- [ ] 5.3.4 Test role-based access control across all endpoints
- [ ] 5.3.5 Validate audit log completeness for all actions

### 5.4 Documentation and Deployment
- [ ] 5.4.1 Create API documentation (Swagger/OpenAPI)
- [ ] 5.4.2 Write deployment guide (database setup, application configuration)
- [ ] 5.4.3 Create user manual for each role (Employee, Manager, HR, Admin)
- [ ] 5.4.4 Set up CI/CD pipeline (build, test, deploy)
- [ ] 5.4.5 Configure production environment (database, email server, file storage)
- [ ] 5.4.6 Perform production deployment and smoke testing

---

## Notes

- **Phase 0** must be completed by the lead developer before other phases begin
- **Phases 1-4** can be executed in parallel by different developers after Phase 0 is complete
- Each developer should coordinate on shared interfaces and DTOs
- All property-based tests must run with minimum 100 iterations
- All tests must pass before merging to main branch
- Code reviews required for all phases before integration
