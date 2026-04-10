# Property Test 2.5.3: Historical Form Uses Correct Template Version

## Overview

This document describes the property-based test for **Property 10** from the Employee Appraisal Cycle design document.

## Property Definition

**Property 10: Historical Form Uses Correct Template Version**

*For any* historical appraisal form, the template used to render it SHALL match the template_id recorded on the form at the time of cycle creation — not the currently active template.

**Validates:** Requirements 4.11, 13.1, 13.2

## Test Implementation

### File Location
`src/test/java/com/tns/appraisal/cycle/HistoricalFormTemplatePropertyTest.java`

### Test Framework
- **jqwik** (version 1.9.1) - Property-based testing framework for Java
- **Spring Boot Test** - For database integration testing
- **JUnit 5** - Test execution platform

### Test Configuration

The test runs **100 iterations** with randomly generated template versions from a predefined set.

```java
@Property(tries = 100)
@Label("Feature: employee-appraisal-cycle, Property 10: Historical Form Uses Correct Template Version")
void historicalFormUsesCorrectTemplateVersion(
    @ForAll("templateVersions") String initialTemplateVersion,
    @ForAll("templateVersions") String newTemplateVersion
)
```

### Test Scenario

For each iteration, the test:

1. **Creates initial template A** and activates it
2. **Creates a cycle** with template A
3. **Creates an appraisal form** that references template A
4. **Creates and activates a new template B** (making template A inactive)
5. **Verifies** that the historical form still references template A (not the currently active template B)
6. **Verifies** that the form can be rendered using template A's schema

### Test Assertions

For each iteration, the test verifies:

1. **Template Preservation**: Form's template_id remains unchanged after a new template is activated
2. **No Active Template Reference**: Form does NOT reference the currently active template
3. **Historical Template Exists**: The original template still exists in the database (not deleted)
4. **Rendering Capability**: Form can be rendered using its recorded template_id
5. **Schema Correctness**: Retrieved schema matches the historical template version, not the current one

### Additional Test Properties

#### Property 10 Extension 1: Multiple Historical Forms Preserve Template Versions

Tests that multiple forms from different cycles can coexist with different template versions:

- Creates three sequential cycles with three different template versions
- Verifies each form preserves its original template_id
- Verifies only the latest template is active
- Verifies all historical templates still exist
- Verifies each form can retrieve its specific template schema

#### Property 10 Extension 2: Form Rendering Uses Stored Template ID

Tests that the rendering logic explicitly uses the form's template_id field:

- Creates a historical form with an old template
- Activates a new template
- Simulates form rendering by retrieving the template
- Verifies rendering uses the stored template_id, not the active template
- Verifies retrieved schema is from the historical template

## Current Status

**Status:** ⚠️ **DISABLED** - Awaiting implementation of required components

The test is currently disabled with the `@Disabled` annotation because it requires the following components to be implemented:

### Required Components

1. **Entities**
   - `AppraisalForm` entity (`src/main/java/com/tns/appraisal/form/AppraisalForm.java`)
   - `User` entity (`src/main/java/com/tns/appraisal/user/User.java`)

2. **Repositories**
   - `AppraisalFormRepository` (`src/main/java/com/tns/appraisal/form/AppraisalFormRepository.java`)
   - `UserRepository` (`src/main/java/com/tns/appraisal/user/UserRepository.java`)

3. **Service Implementation**
   - Form rendering service that uses the form's template_id field
   - Ensure rendering logic retrieves template by the stored template_id, not the active template

## How to Enable the Test

Follow these steps to enable and run the property test:

### Step 1: Implement Required Entities

Create the missing JPA entities with proper annotations and relationships.

### Step 2: Implement Required Repositories

Create Spring Data JPA repositories for the entities.

### Step 3: Implement Form Rendering Service

Create a `FormRenderingService` that:
- Accepts a form ID as input
- Retrieves the form's template_id from the database
- Fetches the template using the stored template_id (NOT the currently active template)
- Renders the form using the retrieved template schema

**Critical Implementation Detail:**
```java
// CORRECT: Use form's stored template_id
AppraisalForm form = formRepository.findById(formId);
AppraisalTemplate template = templateRepository.findById(form.getTemplateId());

// INCORRECT: Do NOT use active template
AppraisalTemplate template = templateRepository.findByIsActiveTrue(); // WRONG!
```

### Step 4: Update the Test

1. Remove the `@Disabled` annotation from `HistoricalFormTemplatePropertyTest`
2. Inject `FormRenderingService` into the test class
3. Replace direct JDBC operations with service calls:
   ```java
   FormRenderingResult result = formRenderingService.renderForm(formId);
   assert result.getTemplateId().equals(expectedTemplateId);
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
mvn test -Dtest=HistoricalFormTemplatePropertyTest
```

## Expected Behavior

When the test is enabled and all components are implemented:

1. **Test Execution**: The test will run 100 iterations
2. **Random Generation**: Each iteration uses randomly generated template versions from the set: 1.0, 2.0, 3.0, 3.1, 4.0, 4.5, 5.0
3. **Verification**: All assertions pass, confirming Property 10 holds across all test cases
4. **Output**: jqwik will report:
   - Number of tries: 100
   - Number of checks: 100
   - Seed value (for reproducibility)
   - Success/failure status

## Test Output Example

```
timestamp = 2026-04-09T13:00:00, HistoricalFormTemplatePropertyTest:Feature: employee-appraisal-cycle, Property 10: Historical Form Uses Correct Template Version = 
                              |-----------------------jqwik-----------------------|
tries = 100                   | # of calls to property
checks = 100                  | # of not rejected calls
generation = RANDOMIZED       | parameters are randomly generated
after-failure = SAMPLE_FIRST  | try previously failed sample, then previous seed
when-fixed-seed = ALLOW       | fixing the random seed is allowed
edge-cases#mode = MIXIN       | edge cases are mixed in
edge-cases#total = 49         | # of all combined edge cases
edge-cases#tried = 49         | # of edge cases tried in current run
seed = 1234567890123456789    | random seed to reproduce generated values
```

## Troubleshooting

### Test Fails with "Form references active template instead of historical template"

**Cause**: Form rendering service is using the currently active template instead of the form's stored template_id

**Solution**: 
- Review the form rendering logic
- Ensure it retrieves the template using `form.getTemplateId()`, not `templateRepository.findByIsActiveTrue()`
- Add logging to verify which template is being used

### Test Fails with "Template not found"

**Cause**: Historical templates are being deleted when a new template is activated

**Solution**:
- Ensure template activation only sets `is_active = false` on old templates
- Do NOT delete historical templates
- Historical templates must be preserved for rendering old forms

### Test Fails with "Schema contains wrong version"

**Cause**: Template schema is being updated in place instead of creating new versions

**Solution**:
- Ensure each template version is a separate database record
- Never update the `schema_json` of an existing template
- Always create a new template record for schema changes

## Design Implications

This property test validates a critical design decision: **template versioning for historical preservation**.

### Why This Matters

1. **Audit Trail**: Historical appraisal forms must be viewable exactly as they were at the time of creation
2. **Legal Compliance**: Performance records may be subject to legal review years later
3. **Data Integrity**: Changing a template should not retroactively alter historical forms
4. **User Trust**: Employees and managers must trust that historical records are immutable

### Implementation Requirements

To satisfy Property 10, the system must:

1. **Store template_id on forms**: Each form must record which template was used at creation time
2. **Preserve historical templates**: Old templates must never be deleted, only deactivated
3. **Render by template_id**: Form rendering must use the stored template_id, not the active template
4. **Version templates**: Each template change must create a new version, not update the existing one

### Anti-Patterns to Avoid

❌ **DO NOT** delete old templates when activating a new one
❌ **DO NOT** update template schema in place
❌ **DO NOT** use the active template for rendering historical forms
❌ **DO NOT** allow forms to reference a template by version string instead of ID

✅ **DO** store template_id as a foreign key on forms
✅ **DO** preserve all template versions in the database
✅ **DO** render forms using their stored template_id
✅ **DO** create new template records for each version

## Related Documentation

- **Design Document**: `.kiro/specs/employee-appraisal-cycle/design.md` (Property 10)
- **Requirements Document**: `.kiro/specs/employee-appraisal-cycle/requirements.md` (Requirements 4.11, 13.1, 13.2)
- **Tasks Document**: `.kiro/specs/employee-appraisal-cycle/tasks.md` (Task 2.5.3)
- **jqwik Documentation**: https://jqwik.net/docs/current/user-guide.html

## Notes

- The test uses direct JDBC operations as a temporary measure until the full service layer is implemented
- Once the service layer is complete, the test should be updated to use `FormRenderingService` directly
- The test validates the core invariant of Property 10 across a wide range of template version combinations
- Property-based testing provides stronger guarantees than example-based testing by exploring the input space systematically
- This test is critical for ensuring historical data integrity and audit trail compliance
