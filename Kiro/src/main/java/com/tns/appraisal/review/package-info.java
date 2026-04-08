/**
 * Manager review module.
 * 
 * <p>This package contains components for manager review operations on submitted
 * appraisal forms. Managers can add ratings and comments, save review drafts,
 * and complete reviews. Backup reviewers have equivalent permissions.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>ReviewController - REST endpoints for review operations</li>
 *   <li>ReviewService - Business logic for review workflow and completion</li>
 *   <li>Review operations integrate with FormService for state transitions</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>PUT /api/forms/{id}/review/draft - Save review draft</li>
 *   <li>POST /api/forms/{id}/review/complete - Complete review</li>
 * </ul>
 */
package com.tns.appraisal.review;
