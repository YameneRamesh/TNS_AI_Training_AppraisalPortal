/**
 * Appraisal form management module.
 * 
 * <p>This package contains components for managing appraisal forms throughout
 * their lifecycle. It handles form creation, employee self-appraisal, draft
 * saving, submission, and state transitions according to the workflow state machine.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>FormController - REST endpoints for form operations</li>
 *   <li>FormService - Business logic for form lifecycle and state transitions</li>
 *   <li>FormRepository - Data access for appraisal form entities</li>
 *   <li>AppraisalForm entity - Form data and status tracking</li>
 *   <li>FormStatus enum - Valid form states (NOT_STARTED, DRAFT_SAVED, SUBMITTED, etc.)</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/forms/my - Get current user's form</li>
 *   <li>GET /api/forms/{id} - Get form by ID</li>
 *   <li>PUT /api/forms/{id}/draft - Save self-appraisal draft</li>
 *   <li>POST /api/forms/{id}/submit - Submit self-appraisal</li>
 *   <li>GET /api/forms/{id}/pdf - Download PDF</li>
 *   <li>GET /api/forms/history - Get historical forms</li>
 * </ul>
 */
package com.tns.appraisal.form;
