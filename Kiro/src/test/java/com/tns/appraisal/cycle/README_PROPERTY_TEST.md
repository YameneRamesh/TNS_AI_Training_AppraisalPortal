# Property Test 2.5.1: Cycle Trigger Creates Exactly One Form Per Employee

## Overview

This document describes the property-based test for **Property 6** from the Employee Appraisal Cycle design document.

## Property Definition

**Property 6: Cycle Trigger Creates Exactly One Form Per Employee**

*For any* appraisal cycle triggered with a set of N eligible employees, the system SHALL create exactly N appraisal form records — one per employee — each referencing the active Appraisal_Template at the time of triggering.

**Validates:** Requirements 3.3

## Test Implementation

### File Location
`src/test/java/com/tns/appraisal/cycle/CycleTriggerPropertyTest.java`

### Test Framework
- **jqwik** (version 1.9.1) - Property-based testing framework for Java
- **Spring Boot Test** - For database integration testing
- **JUnit 5** - Test execution platform

### Test Configuration

The test runs **100 iterations** with randomly generated employee counts ranging from 1 to 50 employees per iteration.

```java
@Property(tries = 100)
@Label("Feature: employee-appraisal-cycle, Property 6: Cycle Trigger Creates Exactly One Form Per Employee")
void cycleTriggerCreatesExactlyOneFormPerEmployee(
    @ForAll @IntRange(min = 1, max = 50) int numberOfEmployees
)
```

### Test Assertions

For each iteration, the test verifies:

1. **Exact Count**: Exactly N appraisal forms are created for N employees
2. **Uniqueness**: Each employee has exactly one form in the cycle
3. **Template Reference**: All forms reference the active template at trigger time
4. **No Duplicates**: No duplicate forms exist for the same employee in the same cycle (enforced by unique constraint)
5. **Initial Status**: All forms have the correct initial status (`NOT_STARTED`)
6. **Required Fields**: All forms have required fields populated (cycle_id, employee_id, manager_id, template_id, status)

## Current Status

**Status:** ⚠️ **DISABLED** - Awaiting implementation of required components

The test is currently disabled with the `@Disabled` annotation because it requires the following components to be implemented:

### Required Components

1. **Entities**
   - `AppraisalForm` entity (`src/main/java/com/tns/appraisal/form/AppraisalForm.java`)
   - `User` entity (`src/main/java/com/tns/appraisal/user/User.java`)
   - `Role` entity (`src/main/java/com/tns/appraisal/user/Role.java`)

2. **Repositories**
   - `AppraisalFormRepository` (`src/main/java/com/tns/appraisal/form/AppraisalFormRepository.java`)
   - `UserRepository` (`src/main/java/com/tns/appraisal/user/UserRepository.java`)

3. **Service Implementation**
   - Complete `CycleService.triggerCycle()` method implementation
   - Uncomment TODO sections in `CycleService.java`
   - Inject `AppraisalFormRepository` and `UserRepository`
   - Implement form creation logic
   - Implement notification sending logic

4. **Notification Service**
   - `NotificationService` for sending emails to employees and managers

## How to Enable the Test

Follow these steps to enable and run the property test:

### Step 1: Implement Required Entities

Create the missing JPA entities with proper annotations and relationships.

### Step 2: Implement Required Repositories

Create Spring Data JPA repositories for the entities.

### Step 3: Complete CycleService Implementation

Update `CycleService.triggerCycle()` to:
- Fetch employees from `UserRepository`
- Create `AppraisalForm` records using `AppraisalFormRepository`
- Send notifications using `NotificationService`
- Handle partial failures gracefully (Property 18)

### Step 4: Update the Test

1. Remove the `@Disabled` annotation from `CycleTriggerPropertyTest`
2. Inject `CycleService` into the test class
3. Replace direct JDBC operations in the `triggerCycle()` helper method with:
   ```java
   cycleService.triggerCycle(cycleId, employeeIds, currentUserId);
   ```

### Step 5: Configure Test Database

Update `TestPropertySource` to point to a dedicated test database:
```java
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=appraisal_test;...",
    "spring.flyway.enabled=true",
    "spring.flyway.clean-disabled=false"
})
```

### Step 6: Run the Test

```bash
mvn test -Dtest=CycleTriggerPropertyTest
```

## Expected Behavior

When the test is enabled and all components are implemented:

1. **Test Execution**: The test will run 100 iterations
2. **Random Generation**: Each iteration uses a randomly generated number of employees (1-50)
3. **Verification**: All assertions pass, confirming Property 6 holds across all test cases
4. **Output**: jqwik will report:
   - Number of tries: 100
   - Number of checks: 100
   - Seed value (for reproducibility)
   - Success/failure status

## Test Output Example

```
timestamp = 2026-04-09T13:00:00, CycleTriggerPropertyTest:Feature: employee-appraisal-cycle, Property 6: Cycle Trigger Creates Exactly One Form Per Employee = 
                              |-----------------------jqwik-----------------------|
tries = 100                   | # of calls to property
checks = 100                  | # of not rejected calls
generation = RANDOMIZED       | parameters are randomly generated
after-failure = SAMPLE_FIRST  | try previously failed sample, then previous seed
when-fixed-seed = ALLOW       | fixing the random seed is allowed
edge-cases#mode = MIXIN       | edge cases are mixed in
edge-cases#total = 2          | # of all combined edge cases
edge-cases#tried = 2          | # of edge cases tried in current run
seed = 1234567890123456789    | random seed to reproduce generated values
```

## Troubleshooting

### Test Fails with NullPointerException

**Cause**: `JdbcTemplate` is not being injected

**Solution**: Ensure the test class has proper Spring Boot test annotations:
- `@DataJpaTest` or `@SpringBootTest`
- `@AutoConfigureTestDatabase`
- `@TestPropertySource` with correct database configuration

### Test Fails with Database Connection Error

**Cause**: Database is not accessible or credentials are incorrect

**Solution**: 
- Verify SQL Server is running
- Check database connection string in `@TestPropertySource`
- Verify username and password are correct
- Ensure Flyway migrations have run successfully

### Test Fails with Assertion Error

**Cause**: The implementation doesn't satisfy Property 6

**Solution**:
- Review the failing assertion message
- Check the jqwik output for the failing sample (employee count)
- Debug the `CycleService.triggerCycle()` implementation
- Verify database constraints (unique constraint on cycle_id + employee_id)

## Related Documentation

- **Design Document**: `.kiro/specs/employee-appraisal-cycle/design.md` (Property 6)
- **Requirements Document**: `.kiro/specs/employee-appraisal-cycle/requirements.md` (Requirement 3.3)
- **Tasks Document**: `.kiro/specs/employee-appraisal-cycle/tasks.md` (Task 2.5.1)
- **jqwik Documentation**: https://jqwik.net/docs/current/user-guide.html

## Notes

- The test uses direct JDBC operations as a temporary measure until the full service layer is implemented
- Once the service layer is complete, the test should be updated to use `CycleService.triggerCycle()` directly
- The test validates the core invariant of Property 6 across a wide range of input values (1-50 employees)
- Property-based testing provides stronger guarantees than example-based testing by exploring the input space systematically
