# Story 2.8: Phase 1 and Phase 2 Signoff Fixes

Status: ready-for-dev

## Story

As an HR/admin stakeholder,
I want Phase 1 and Phase 2 flows to be stable under real E2E execution without manual UI interaction hacks,
so that we can reliably sign off auth, role guard, cycle trigger, and template/cycle UI behaviors.

## Acceptance Criteria

1. UI state updates after async API responses without requiring user clicks.
2. Phase 2 trigger selection excludes ineligible employee records that are guaranteed to fail due to missing manager assignment.
3. Trigger workflow communicates excluded records to HR before action.
4. Existing Phase 1 role-based route protections remain unchanged.
5. Frontend compiles successfully after fixes.

## Tasks / Subtasks

- [x] Restore default async change-detection behavior at app level.
  - [x] Add `zone.js` to Angular polyfills.
- [x] Prevent predictable trigger partial failures in UI pre-selection.
  - [x] Filter trigger list to `EMPLOYEE` users with manager assignment.
  - [x] Add UI message that reports excluded users.
- [x] Validate no regression in route-based role protections.
- [x] Build frontend in development configuration.

## Dev Notes

- Global fix was applied in Angular configuration (`polyfills`) rather than adding per-component click-triggered workarounds.
- Trigger list now removes users who cannot pass backend form creation preconditions.
- Reopen happy-path remains a data-state dependency (requires form in reopenable status as per PRD).

### Project Structure Notes

- Frontend changes only:
  - `frontend/angular.json`
  - `frontend/src/app/features/hr/cycle-trigger/cycle-trigger.component.ts`
  - `frontend/src/app/features/hr/cycle-trigger/cycle-trigger.component.html`

### References

- [Source: `.kiro/specs/employee-appraisal-cycle/phase2-prd.md`]
- [Source: `.kiro/specs/employee-appraisal-cycle/tasks.md`]

## Dev Agent Record

### Agent Model Used

Codex (Cursor coding agent)

### Debug Log References

- E2E run identified no-manager employees causing deterministic trigger failures.
- Async UI rendering issue traced to missing `zone.js` polyfill.

### Completion Notes List

- Added `zone.js` polyfill to restore default Angular change detection behavior.
- Added eligibility filtering and excluded-user warning in cycle trigger screen.
- Frontend development build completed successfully.

### File List

- `frontend/angular.json`
- `frontend/src/app/features/hr/cycle-trigger/cycle-trigger.component.ts`
- `frontend/src/app/features/hr/cycle-trigger/cycle-trigger.component.html`
