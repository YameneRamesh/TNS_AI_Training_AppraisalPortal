package com.tns.appraisal.exception;

/**
 * Exception thrown when an invalid state transition is attempted.
 */
public class InvalidStateTransitionException extends RuntimeException {
    
    public InvalidStateTransitionException(String message) {
        super(message);
    }
    
    public InvalidStateTransitionException(String currentState, String targetState) {
        super(String.format("Invalid state transition from %s to %s", currentState, targetState));
    }
}
