# Phase 1 + Phase 2 E2E Fix Note (2026-04-12)

## Issue

During Playwright E2E validation, users with a valid server session were intermittently redirected to `/login` when opening protected routes directly (for example `/hr/cycles`).

## Root Cause

`authGuard` and `roleGuard` read `AuthService.currentUser$` immediately.
On app bootstrap, `currentUser$` starts as `null` and `/api/auth/me` runs asynchronously.
Because both guards used `take(1)`, they decided before session restoration completed, resulting in a false unauthenticated decision and redirect.

## Fix Applied

1. Added `resolveSessionUser()` in `AuthService` to fetch `/api/auth/me`, update `currentUserSubject`, and gracefully return `null` on failure.
2. Updated `authGuard`:
   - If already authenticated, allow access.
   - Otherwise call `resolveSessionUser()` before redirecting to `/login`.
3. Updated `roleGuard`:
   - If `currentUser` is missing, call `resolveSessionUser()`.
   - Then apply role checks from route metadata.

## Expected Outcome

- Valid sessions persist across direct route access and browser refresh.
- HR/Admin/Manager/Employee protected routes no longer redirect prematurely during app initialization.
- Unauthorized users still redirect to `/login` as before.
