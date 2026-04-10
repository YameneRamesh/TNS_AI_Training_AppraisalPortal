package com.tns.appraisal.form;

import com.tns.appraisal.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * State machine enforcing valid appraisal form status transitions and
 * role-based authorization for each transition.
 *
 * <p>Invalid transitions throw {@link InvalidStateTransitionException} → HTTP 409.</p>
 */
@Component
public class FormStateMachine {

    /** All structurally valid transitions regardless of role. */
    private static final Map<FormStatus, Set<FormStatus>> VALID_TRANSITIONS = Map.of(
        FormStatus.NOT_STARTED,             Set.of(FormStatus.DRAFT_SAVED, FormStatus.SUBMITTED),
        FormStatus.DRAFT_SAVED,             Set.of(FormStatus.SUBMITTED),
        FormStatus.SUBMITTED,               Set.of(FormStatus.UNDER_REVIEW, FormStatus.DRAFT_SAVED),
        FormStatus.UNDER_REVIEW,            Set.of(FormStatus.REVIEW_DRAFT_SAVED, FormStatus.REVIEWED_AND_COMPLETED),
        FormStatus.REVIEW_DRAFT_SAVED,      Set.of(FormStatus.REVIEWED_AND_COMPLETED),
        FormStatus.REVIEWED_AND_COMPLETED,  Set.of(FormStatus.DRAFT_SAVED)
    );

    /**
     * Transitions each role is authorized to perform.
     * Key: "ROLE:FROM_STATUS:TO_STATUS"
     */
    private static final Set<String> AUTHORIZED_ROLE_TRANSITIONS = Set.of(
        // Employee transitions
        "EMPLOYEE:NOT_STARTED:DRAFT_SAVED",
        "EMPLOYEE:NOT_STARTED:SUBMITTED",
        "EMPLOYEE:DRAFT_SAVED:SUBMITTED",
        // Manager transitions
        "MANAGER:SUBMITTED:UNDER_REVIEW",
        "MANAGER:UNDER_REVIEW:REVIEW_DRAFT_SAVED",
        "MANAGER:UNDER_REVIEW:REVIEWED_AND_COMPLETED",
        "MANAGER:REVIEW_DRAFT_SAVED:REVIEWED_AND_COMPLETED",
        // HR transitions (reopen)
        "HR:SUBMITTED:DRAFT_SAVED",
        "HR:REVIEWED_AND_COMPLETED:DRAFT_SAVED"
    );

    /**
     * Validates that transitioning from {@code from} to {@code to} is structurally valid.
     *
     * @throws InvalidStateTransitionException if the transition is not permitted
     */
    public void validateTransition(FormStatus from, FormStatus to) {
        if (!isValidTransition(from, to)) {
            throw new InvalidStateTransitionException(from.name(), to.name());
        }
    }

    /**
     * Returns {@code true} if the transition from {@code from} to {@code to} is structurally valid.
     */
    public boolean isValidTransition(FormStatus from, FormStatus to) {
        Set<FormStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Set.of());
        return allowed.contains(to);
    }

    /**
     * Returns the set of statuses that {@code from} can legally transition to.
     */
    public Set<FormStatus> getAllowedTransitions(FormStatus from) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of());
    }

    /**
     * Validates both structural validity and role authorization for a transition.
     *
     * @param from the current form status
     * @param to   the target form status
     * @param role the role of the user attempting the transition (e.g. "EMPLOYEE", "MANAGER", "HR")
     * @throws InvalidStateTransitionException if the transition is structurally invalid or the role
     *                                         is not authorized to perform it
     */
    public void validateRoleTransition(FormStatus from, FormStatus to, String role) {
        validateTransition(from, to);
        String key = role + ":" + from.name() + ":" + to.name();
        if (!AUTHORIZED_ROLE_TRANSITIONS.contains(key)) {
            throw new InvalidStateTransitionException(
                String.format("Role %s is not authorized to transition form from %s to %s", role, from, to)
            );
        }
    }
}
