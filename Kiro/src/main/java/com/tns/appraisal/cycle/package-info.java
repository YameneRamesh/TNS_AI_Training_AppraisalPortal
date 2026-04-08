/**
 * Appraisal cycle management module.
 * 
 * <p>This package contains components for managing appraisal cycles, including
 * cycle creation, triggering, and form reopening. HR users can configure cycles,
 * trigger bulk form creation, and assign backup reviewers.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>CycleController - REST endpoints for cycle operations</li>
 *   <li>CycleService - Business logic for cycle management and bulk operations</li>
 *   <li>CycleRepository - Data access for appraisal cycle entities</li>
 *   <li>AppraisalCycle entity - Cycle definition and configuration</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/cycles - List all cycles</li>
 *   <li>POST /api/cycles - Create new cycle</li>
 *   <li>PUT /api/cycles/{id} - Update cycle</li>
 *   <li>POST /api/cycles/{id}/trigger - Trigger cycle (bulk form creation)</li>
 *   <li>POST /api/cycles/{id}/reopen/{formId} - Reopen a form</li>
 *   <li>PUT /api/cycles/{id}/backup-reviewer - Assign backup reviewer</li>
 * </ul>
 */
package com.tns.appraisal.cycle;
