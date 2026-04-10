# Phase 1 Test Scenarios - Employee Appraisal Portal

## Test Environment
- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Test Date: 2026-04-10

## Credentials
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@tns.com | password |
| HR | hr@tns.com | password |
| Manager | john.manager@tns.com | password |
| Employee | bob.jones@tns.com | password |

---

## TC-01: Login with valid credentials (Admin)
- Status: ✅ PASS
- Notes: Login succeeds, redirects to /admin, user list loads correctly

## TC-02: Login with invalid credentials
- Status: ⬜ PENDING

## TC-03: Logout functionality
- Status: ⬜ PENDING

## TC-04: Route guard - unauthenticated access to /admin
- Status: ⬜ PENDING

## TC-05: Admin - Create new user
- Status: ⬜ PENDING

## TC-06: Admin - Edit existing user
- Status: ⬜ PENDING

## TC-07: Admin - Deactivate user
- Status: ⬜ PENDING

## TC-08: Admin - Reactivate user
- Status: ⬜ PENDING

## TC-09: Admin - Assign roles to user
- Status: ⬜ PENDING

## TC-10: Admin - Search/filter users
- Status: ⬜ PENDING

## TC-11: Admin - Audit log viewer
- Status: ⬜ PENDING

## TC-12: Login as HR user
- Status: ⬜ PENDING

## TC-13: Login as Manager user
- Status: ⬜ PENDING

## TC-14: Login as Employee user
- Status: ⬜ PENDING

## TC-15: Role guard - HR cannot access /admin
- Status: ⬜ PENDING

---

## Bugs Found
| # | Description | Severity | Status |
|---|-------------|----------|--------|
