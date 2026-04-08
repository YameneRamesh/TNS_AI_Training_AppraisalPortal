/**
 * Dashboard and reporting module.
 * 
 * <p>This package contains components for role-specific dashboard data aggregation.
 * Dashboards provide summary views of appraisal status, completion metrics, and
 * pending actions tailored to each user role.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>DashboardController - REST endpoints for role-specific dashboards</li>
 *   <li>DashboardService - Data aggregation and metrics calculation</li>
 *   <li>Employee dashboard - Current form status and history</li>
 *   <li>Manager dashboard - Team progress and completion percentage</li>
 *   <li>HR dashboard - Organization-wide metrics and department progress</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/dashboard/employee - Employee dashboard data</li>
 *   <li>GET /api/dashboard/manager - Manager dashboard data</li>
 *   <li>GET /api/dashboard/hr - HR dashboard data</li>
 * </ul>
 */
package com.tns.appraisal.dashboard;
