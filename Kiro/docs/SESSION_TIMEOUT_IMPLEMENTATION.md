# Session Timeout Implementation

## Overview

The Employee Appraisal Cycle application implements a 15-minute session timeout mechanism to ensure security and comply with the requirement that sessions expire after 15 minutes of inactivity.

## Backend Implementation

### Configuration

Session timeout is configured in `application.properties`:

```properties
server.servlet.session.timeout=15m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
```

### SecurityConfig

The `SecurityConfig` class configures Spring Security to handle session management:

- **Session Timeout**: Set to 15 minutes (900 seconds)
- **Maximum Sessions**: 1 session per user (prevents concurrent logins)
- **Expired Session Strategy**: Returns HTTP 401 with JSON error message
- **Session Event Publisher**: Tracks session lifecycle events

Key features:
- Automatic session invalidation after 15 minutes of inactivity
- Custom expired session handler that returns structured JSON error
- Session cookies are HTTP-only for security

### AuthService

The `AuthService` provides session management methods:

- `login()`: Creates a new session with 15-minute timeout
- `logout()`: Invalidates the current session
- `isSessionValid()`: Checks if a session is active
- `refreshSession()`: Resets the 15-minute timeout timer
- `getCurrentUser()`: Retrieves the authenticated user from session

## Frontend Implementation

### AuthService (Angular)

The Angular `AuthService` implements client-side session tracking:

**Activity Tracking**:
- Monitors user activity events (mousedown, keydown, scroll, touchstart, click)
- Updates last activity timestamp on each interaction
- Checks session status every 30 seconds

**Session Warning**:
- Shows warning 2 minutes before session expires
- Emits `sessionWarning$` observable for UI components to subscribe
- Automatically dismisses warning when user becomes active

**Session Refresh**:
- Refreshes session activity on successful API calls
- Provides `getRemainingSessionTime()` to display countdown

### HTTP Interceptor

The `authInterceptor` handles session-related HTTP responses:

- Includes credentials (session cookies) in all requests
- Refreshes session activity on successful API calls
- Redirects to login page on 401 Unauthorized
- Passes session expiration flag to login page

### Session Timeout Warning Component

The `SessionTimeoutWarningComponent` displays a Material snackbar notification:

- Shows warning 2 minutes before timeout
- Provides "Stay Active" button to refresh session
- Automatically dismisses when user interacts with the page
- Styled with orange background for visibility

## User Experience Flow

### Normal Session Flow

1. User logs in → Session created with 15-minute timeout
2. User interacts with application → Activity tracked, timeout resets
3. User makes API calls → Session refreshed automatically
4. User logs out → Session invalidated immediately

### Session Timeout Flow

1. User is idle for 13 minutes → No action
2. User is idle for 13+ minutes → Warning snackbar appears
3. User clicks "Stay Active" or interacts → Session refreshed, warning dismissed
4. User remains idle for 15 minutes → Session expires
5. Next API call returns 401 → User redirected to login page

### Session Expiration Handling

When a session expires:
1. Backend returns HTTP 401 with error message
2. Frontend interceptor catches 401 response
3. User redirected to login page with `sessionExpired=true` query param
4. Login page displays "Your session has expired" message

## Testing

### Unit Tests

`SessionTimeoutTest.java` verifies:
- Session timeout is set to 15 minutes (900 seconds)
- `isSessionValid()` correctly identifies active/expired sessions
- `refreshSession()` resets the timeout timer
- `logout()` invalidates the session

### Integration Tests

`SessionTimeoutIntegrationTest.java` verifies:
- Login creates session with correct timeout
- Protected endpoints require active session
- Invalidated sessions return 401
- Logout properly invalidates session

## Security Considerations

1. **HTTP-Only Cookies**: Session cookies are HTTP-only to prevent XSS attacks
2. **Session Invalidation**: Sessions are properly invalidated on logout and timeout
3. **Single Session**: Only one session per user prevents session fixation
4. **Audit Logging**: All login, logout, and session events are logged
5. **Secure Flag**: Set to `false` for POC (should be `true` in production with HTTPS)

## Configuration Options

To modify session timeout duration, update `application.properties`:

```properties
# Change to 30 minutes
server.servlet.session.timeout=30m

# Change to 10 minutes
server.servlet.session.timeout=10m
```

To modify warning time in Angular, update `AuthService`:

```typescript
private readonly WARNING_BEFORE_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes
```

## Troubleshooting

### Session Expires Too Quickly

- Check `application.properties` for correct timeout value
- Verify activity tracking is working in browser console
- Ensure HTTP interceptor is refreshing session on API calls

### Session Warning Not Appearing

- Check browser console for errors
- Verify `SessionTimeoutWarningComponent` is included in app
- Check that `sessionWarning$` observable is being subscribed

### 401 Errors After Login

- Verify session cookies are being sent with requests
- Check `withCredentials: true` in HTTP interceptor
- Ensure CORS is configured to allow credentials

## Future Enhancements

1. **Configurable Warning Time**: Allow users to configure warning duration
2. **Session Extension**: Add "Extend Session" button in warning
3. **Multiple Session Support**: Allow users to have multiple active sessions
4. **Session Analytics**: Track session duration and timeout frequency
5. **Remember Me**: Optional longer session duration for trusted devices
