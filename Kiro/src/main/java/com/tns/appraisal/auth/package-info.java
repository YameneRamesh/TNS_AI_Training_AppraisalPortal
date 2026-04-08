/**
 * Authentication and session management module.
 * 
 * <p>This package contains components for user authentication, session management,
 * and security configuration. It handles login/logout operations and maintains
 * user sessions with a 15-minute timeout policy.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>AuthController - REST endpoints for authentication operations</li>
 *   <li>AuthService - Business logic for authentication and session management</li>
 *   <li>SessionManager - Session lifecycle management</li>
 * </ul>
 * 
 * <p>REST Endpoints:</p>
 * <ul>
 *   <li>POST /api/auth/login - Authenticate user and create session</li>
 *   <li>POST /api/auth/logout - Invalidate current session</li>
 *   <li>GET /api/auth/me - Get current user profile</li>
 * </ul>
 */
package com.tns.appraisal.auth;
