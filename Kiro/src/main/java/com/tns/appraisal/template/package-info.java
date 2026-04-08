/**
 * Appraisal template management module.
 * 
 * <p>This package contains components for managing appraisal form templates.
 * Templates are stored as JSON schemas that define the structure of appraisal
 * forms, supporting versioning and historical form rendering.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>TemplateController - REST endpoints for template operations</li>
 *   <li>TemplateService - Business logic for template CRUD and versioning</li>
 *   <li>TemplateRepository - Data access for template entities</li>
 *   <li>AppraisalTemplate entity - JSON schema-based form template</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/templates - List templates</li>
 *   <li>POST /api/templates - Create template</li>
 *   <li>GET /api/templates/{id} - Get template by ID</li>
 * </ul>
 */
package com.tns.appraisal.template;
