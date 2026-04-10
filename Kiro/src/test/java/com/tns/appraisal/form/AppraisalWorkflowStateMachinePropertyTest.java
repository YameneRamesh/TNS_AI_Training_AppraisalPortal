package com.tns.appraisal.form;

import com.tns.appraisal.exception.InvalidStateTransitionException;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.assertj.core.api.Assertions;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Property-Based Test: Appraisal Workflow State Machine Validity (Property 11)
 *
 * <p>Validates that:
 * <ol>
 *   <li>The form status only transitions through valid state paths:
 *       NOT_STARTED → DRAFT_SAVED → SUBMITTED → UNDER_REVIEW → REVIEW_DRAFT_SAVED → REVIEWED_AND_COMPLETED</li>
 *   <li>Each transition is only performable by the authorized role
 *       (EMPLOYEE for self-appraisal, MANAGER for review, HR for reopen).</li>
 *   <li>Any attempt to perform an invalid structural transition throws InvalidStateTransitionException.</li>
 *   <li>Any attempt by an unauthorized role to perform a valid structural transition is also rejected.</li>
 * </ol>
 *
 * <p>Validates Requirements: 5.3, 5.4, 5.6, 5.8, 6.3, 6.4, 6.8
 */
class AppraisalWorkflowStateMachinePropertyTest {

    private FormStateMachine stateMachine;

    // All valid (from, to) structural transitions as defined in FormStateMachine
    private static final List<TransitionPair> VALID_STRUCTURAL_TRANSITIONS = List.of(
        new TransitionPair(FormStatus.NOT_STARTED,           FormStatus.DRAFT_SAVED),
        new TransitionPair(FormStatus.NOT_STARTED,           FormStatus.SUBMITTED),
        new TransitionPair(FormStatus.DRAFT_SAVED,           FormStatus.SUBMITTED),
        new TransitionPair(FormStatus.SUBMITTED,             FormStatus.UNDER_REVIEW),
        new TransitionPair(FormStatus.SUBMITTED,             FormStatus.DRAFT_SAVED),
        new TransitionPair(FormStatus.UNDER_REVIEW,          FormStatus.REVIEW_DRAFT_SAVED),
        new TransitionPair(FormStatus.UNDER_REVIEW,          FormStatus.REVIEWED_AND_COMPLETED),
        new TransitionPair(FormStatus.REVIEW_DRAFT_SAVED,    FormStatus.REVIEWED_AND_COMPLETED),
        new TransitionPair(FormStatus.REVIEWED_AND_COMPLETED, FormStatus.DRAFT_SAVED)
    );

    // Authorized role transitions: role → (from, to) pairs it is allowed to perform
    private static final List<RoleTransition> AUTHORIZED_ROLE_TRANSITIONS = List.of(
        new RoleTransition("EMPLOYEE", FormStatus.NOT_STARTED,        FormStatus.DRAFT_SAVED),
        new RoleTransition("EMPLOYEE", FormStatus.NOT_STARTED,        FormStatus.SUBMITTED),
        new RoleTransition("EMPLOYEE", FormStatus.DRAFT_SAVED,        FormStatus.SUBMITTED),
        new RoleTransition("MANAGER",  FormStatus.SUBMITTED,          FormStatus.UNDER_REVIEW),
        new RoleTransition("MANAGER",  FormStatus.UNDER_REVIEW,       FormStatus.REVIEW_DRAFT_SAVED),
        new RoleTransition("MANAGER",  FormStatus.UNDER_REVIEW,       FormStatus.REVIEWED_AND_COMPLETED),
        new RoleTransition("MANAGER",  FormStatus.REVIEW_DRAFT_SAVED, FormStatus.REVIEWED_AND_COMPLETED),
        new RoleTransition("HR",       FormStatus.SUBMITTED,          FormStatus.DRAFT_SAVED),
        new RoleTransition("HR",       FormStatus.REVIEWED_AND_COMPLETED, FormStatus.DRAFT_SAVED)
    );

    @BeforeProperty
    void setUp() {
        stateMachine = new FormStateMachine();
    }

    // -------------------------------------------------------------------------
    // Property 1: Every valid structural transition is accepted
    // -------------------------------------------------------------------------

    /**
     * For every valid (from, to) pair, validateTransition must NOT throw.
     */
    @Property(tries = 200)
    void validStructuralTransitions_areAlwaysAccepted(
            @ForAll("validTransitionPairs") TransitionPair pair) {

        Assertions.assertThatCode(() -> stateMachine.validateTransition(pair.from(), pair.to()))
            .as("Expected valid transition %s → %s to be accepted", pair.from(), pair.to())
            .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Property 2: Every invalid structural transition is rejected
    // -------------------------------------------------------------------------

    /**
     * For every (from, to) pair that is NOT in the valid set, validateTransition must throw.
     */
    @Property(tries = 200)
    void invalidStructuralTransitions_areAlwaysRejected(
            @ForAll("invalidTransitionPairs") TransitionPair pair) {

        Assertions.assertThatThrownBy(() -> stateMachine.validateTransition(pair.from(), pair.to()))
            .as("Expected invalid transition %s → %s to be rejected", pair.from(), pair.to())
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // Property 3: Authorized role transitions are accepted
    // -------------------------------------------------------------------------

    /**
     * For every (role, from, to) triple in the authorized set, validateRoleTransition must NOT throw.
     */
    @Property(tries = 200)
    void authorizedRoleTransitions_areAlwaysAccepted(
            @ForAll("authorizedRoleTransitions") RoleTransition rt) {

        Assertions.assertThatCode(() -> stateMachine.validateRoleTransition(rt.from(), rt.to(), rt.role()))
            .as("Expected role %s to be authorized for %s → %s", rt.role(), rt.from(), rt.to())
            .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Property 4: Unauthorized role transitions are rejected
    // -------------------------------------------------------------------------

    /**
     * For every structurally valid (from, to) pair, if the role is NOT authorized for it,
     * validateRoleTransition must throw.
     */
    @Property(tries = 200)
    void unauthorizedRoleTransitions_areAlwaysRejected(
            @ForAll("unauthorizedRoleTransitions") RoleTransition rt) {

        Assertions.assertThatThrownBy(() -> stateMachine.validateRoleTransition(rt.from(), rt.to(), rt.role()))
            .as("Expected role %s to be rejected for %s → %s", rt.role(), rt.from(), rt.to())
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // Property 5: isValidTransition is consistent with validateTransition
    // -------------------------------------------------------------------------

    /**
     * isValidTransition(from, to) must return true iff validateTransition does not throw.
     * Tested across all possible (from, to) combinations.
     */
    @Property(tries = 200)
    void isValidTransition_isConsistentWithValidateTransition(
            @ForAll FormStatus from,
            @ForAll FormStatus to) {

        boolean reported = stateMachine.isValidTransition(from, to);
        boolean accepted;
        try {
            stateMachine.validateTransition(from, to);
            accepted = true;
        } catch (InvalidStateTransitionException e) {
            accepted = false;
        }

        Assertions.assertThat(reported)
            .as("isValidTransition(%s, %s) must agree with validateTransition", from, to)
            .isEqualTo(accepted);
    }

    // -------------------------------------------------------------------------
    // Property 6: getAllowedTransitions is consistent with isValidTransition
    // -------------------------------------------------------------------------

    /**
     * For any status S, getAllowedTransitions(S) must contain exactly the statuses T
     * for which isValidTransition(S, T) returns true.
     */
    @Property(tries = 100)
    void getAllowedTransitions_isConsistentWithIsValidTransition(@ForAll FormStatus from) {
        Set<FormStatus> allowed = stateMachine.getAllowedTransitions(from);

        for (FormStatus to : FormStatus.values()) {
            boolean inAllowed = allowed.contains(to);
            boolean isValid   = stateMachine.isValidTransition(from, to);
            Assertions.assertThat(inAllowed)
                .as("getAllowedTransitions(%s) must agree with isValidTransition for target %s", from, to)
                .isEqualTo(isValid);
        }
    }

    // -------------------------------------------------------------------------
    // Property 7: REVIEWED_AND_COMPLETED is a terminal state for normal workflow
    // -------------------------------------------------------------------------

    /**
     * The only valid exit from REVIEWED_AND_COMPLETED is back to DRAFT_SAVED (HR reopen).
     * All other targets must be rejected.
     */
    @Property(tries = 100)
    void reviewedAndCompleted_onlyAllowsHrReopenTransition(@ForAll FormStatus to) {
        boolean isValid = stateMachine.isValidTransition(FormStatus.REVIEWED_AND_COMPLETED, to);

        if (to == FormStatus.DRAFT_SAVED) {
            Assertions.assertThat(isValid)
                .as("REVIEWED_AND_COMPLETED → DRAFT_SAVED (HR reopen) must be valid")
                .isTrue();
        } else {
            Assertions.assertThat(isValid)
                .as("REVIEWED_AND_COMPLETED → %s must be invalid", to)
                .isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Property 8: Employee cannot perform manager or HR transitions
    // -------------------------------------------------------------------------

    /**
     * EMPLOYEE role must be rejected for any transition that belongs exclusively to MANAGER or HR.
     */
    @Property(tries = 100)
    void employee_cannotPerformManagerOrHrTransitions(@ForAll("managerOrHrTransitions") RoleTransition rt) {
        Assertions.assertThatThrownBy(
                () -> stateMachine.validateRoleTransition(rt.from(), rt.to(), "EMPLOYEE"))
            .as("EMPLOYEE must not perform %s → %s (a MANAGER/HR transition)", rt.from(), rt.to())
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    // -------------------------------------------------------------------------
    // Property 9: Manager cannot perform employee or HR transitions
    // -------------------------------------------------------------------------

    /**
     * MANAGER role must be rejected for transitions that belong exclusively to EMPLOYEE or HR.
     */
    @Property(tries = 100)
    void manager_cannotPerformEmployeeOrHrTransitions(@ForAll("employeeOrHrTransitions") RoleTransition rt) {
        Assertions.assertThatThrownBy(
                () -> stateMachine.validateRoleTransition(rt.from(), rt.to(), "MANAGER"))
            .as("MANAGER must not perform %s → %s (an EMPLOYEE/HR transition)", rt.from(), rt.to())
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    // =========================================================================
    // Arbitraries (providers)
    // =========================================================================

    @Provide
    Arbitrary<TransitionPair> validTransitionPairs() {
        return Arbitraries.of(VALID_STRUCTURAL_TRANSITIONS);
    }

    @Provide
    Arbitrary<TransitionPair> invalidTransitionPairs() {
        List<TransitionPair> invalid = new java.util.ArrayList<>();
        for (FormStatus from : FormStatus.values()) {
            for (FormStatus to : FormStatus.values()) {
                TransitionPair candidate = new TransitionPair(from, to);
                if (!VALID_STRUCTURAL_TRANSITIONS.contains(candidate)) {
                    invalid.add(candidate);
                }
            }
        }
        return Arbitraries.of(invalid);
    }

    @Provide
    Arbitrary<RoleTransition> authorizedRoleTransitions() {
        return Arbitraries.of(AUTHORIZED_ROLE_TRANSITIONS);
    }

    @Provide
    Arbitrary<RoleTransition> unauthorizedRoleTransitions() {
        List<String> roles = List.of("EMPLOYEE", "MANAGER", "HR");
        List<RoleTransition> unauthorized = new java.util.ArrayList<>();

        for (String role : roles) {
            for (TransitionPair pair : VALID_STRUCTURAL_TRANSITIONS) {
                RoleTransition candidate = new RoleTransition(role, pair.from(), pair.to());
                if (!AUTHORIZED_ROLE_TRANSITIONS.contains(candidate)) {
                    unauthorized.add(candidate);
                }
            }
        }
        return Arbitraries.of(unauthorized);
    }

    @Provide
    Arbitrary<RoleTransition> managerOrHrTransitions() {
        List<RoleTransition> managerOrHr = AUTHORIZED_ROLE_TRANSITIONS.stream()
            .filter(rt -> rt.role().equals("MANAGER") || rt.role().equals("HR"))
            // Exclude any that EMPLOYEE is also authorized for (none exist, but be safe)
            .filter(rt -> AUTHORIZED_ROLE_TRANSITIONS.stream()
                .noneMatch(a -> a.role().equals("EMPLOYEE")
                    && a.from() == rt.from() && a.to() == rt.to()))
            .toList();
        return Arbitraries.of(managerOrHr);
    }

    @Provide
    Arbitrary<RoleTransition> employeeOrHrTransitions() {
        List<RoleTransition> employeeOrHr = AUTHORIZED_ROLE_TRANSITIONS.stream()
            .filter(rt -> rt.role().equals("EMPLOYEE") || rt.role().equals("HR"))
            // Exclude any that MANAGER is also authorized for (none exist, but be safe)
            .filter(rt -> AUTHORIZED_ROLE_TRANSITIONS.stream()
                .noneMatch(a -> a.role().equals("MANAGER")
                    && a.from() == rt.from() && a.to() == rt.to()))
            .toList();
        return Arbitraries.of(employeeOrHr);
    }

    // =========================================================================
    // Value types
    // =========================================================================

    record TransitionPair(FormStatus from, FormStatus to) {}

    record RoleTransition(String role, FormStatus from, FormStatus to) {}
}
