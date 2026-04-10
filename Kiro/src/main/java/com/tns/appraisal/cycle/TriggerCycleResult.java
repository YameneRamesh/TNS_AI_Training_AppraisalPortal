package com.tns.appraisal.cycle;

import java.util.List;

/**
 * Result DTO for bulk cycle trigger operations.
 * Contains summary of successes and failures with detailed failure information.
 */
public class TriggerCycleResult {

    private final int totalEmployees;
    private final int successCount;
    private final int failureCount;
    private final List<EmployeeFailure> failures;

    public TriggerCycleResult(int totalEmployees, int successCount, int failureCount, List<EmployeeFailure> failures) {
        this.totalEmployees = totalEmployees;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.failures = failures;
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public List<EmployeeFailure> getFailures() {
        return failures;
    }

    /**
     * Represents a single employee processing failure.
     */
    public static class EmployeeFailure {
        private final Long employeeId;
        private final String errorReason;

        public EmployeeFailure(Long employeeId, String errorReason) {
            this.employeeId = employeeId;
            this.errorReason = errorReason;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public String getErrorReason() {
            return errorReason;
        }
    }
}
