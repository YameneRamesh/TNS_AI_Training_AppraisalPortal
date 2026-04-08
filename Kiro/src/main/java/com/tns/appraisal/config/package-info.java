/**
 * Application configuration module.
 * 
 * <p>This package contains Spring configuration classes for security, web MVC,
 * async processing, and other cross-cutting concerns.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>SecurityConfig - Spring Security configuration with session management (15-minute timeout)</li>
 *   <li>WebMvcConfig - Web MVC configuration including CORS for frontend-backend communication</li>
 *   <li>AsyncConfig - Async task executor configuration for notifications and audit logs</li>
 * </ul>
 * 
 * <p>Security features:</p>
 * <ul>
 *   <li>Session-based authentication (HttpSession)</li>
 *   <li>BCrypt password hashing</li>
 *   <li>Role-based access control enforcement</li>
 *   <li>CSRF protection</li>
 *   <li>Session timeout handling</li>
 * </ul>
 */
package com.tns.appraisal.config;
