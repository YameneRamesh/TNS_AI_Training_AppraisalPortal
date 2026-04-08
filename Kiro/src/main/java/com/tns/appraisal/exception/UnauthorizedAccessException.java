package com.tns.appraisal.exception;

/**
 * Exception thrown when a user attempts to access a resource without proper authorization.
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(String action, String resource) {
        super(String.format("Unauthorized to perform action '%s' on resource '%s'", action, resource));
    }
}
