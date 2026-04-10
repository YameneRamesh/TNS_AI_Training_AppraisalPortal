/**
 * User management and administration module.
 * 
 * <p>This package contains components for managing user accounts, roles, and
 * reporting hierarchy. It provides CRUD operations for users and role assignments,
 * accessible to Admin users.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>UserController - REST endpoints for user management</li>
 *   <li>UserService - Business logic for user operations</li>
 *   <li>UserRepository - Data access for user entities</li>
 *   <li>User entity - User account representation</li>
 *   <li>Role entity - Role definition (EMPLOYEE, MANAGER, HR, ADMIN)</li>
 *   <li>ReportingHierarchy entity - Manager-employee relationships</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>GET /api/users - List all users</li>
 *   <li>POST /api/users - Create new user</li>
 *   <li>PUT /api/users/{id} - Update user</li>
 *   <li>DELETE /api/users/{id} - Deactivate user</li>
 *   <li>PUT /api/users/{id}/roles - Assign/revoke roles</li>
 * </ul>
 */
package com.tns.appraisal.user;
