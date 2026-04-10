# Form Validation Logic

This document describes the validation logic implemented for the dynamic appraisal form renderer.

## Overview

The form validation system ensures data integrity and completeness for both employee self-appraisals and manager reviews. Validation is context-aware, adapting to:
- User role (Employee vs Manager)
- Form status (determines which fields are required)
- Section type (different validation rules per section)

## Validation Service

The `FormValidationService` provides centralized validation logic for all form fields and sections.

### Key Features

1. **Field-Level Validation**: Individual validators for comments and ratings
2. **Form-Level Validation**: Complete validation for submission and review completion
3. **Context-Aware Requirements**: Validation rules change based on form status
4. **Custom Validators**: Specialized validators for competency and policy ratings

## Validation Rules

### Comment Fields

- **Max Length**: 2000 characters
- **Required**: Based on form status and user role
- **Applies to**: Self comments, manager comments, overall evaluation comments

### Rating Fields

#### Competency Ratings
- **Valid Values**: 'Excels', 'Exceeds', 'Meets', 'Developing'
- **Type**: String enum
- **Applies to**: Key Responsibilities, IDP, Goals

#### Policy Ratings
- **Valid Values**: 1-10 (integers only)
- **Type**: Number
- **Applies to**: Policy Adherence section

### Form Status Requirements

#### Employee Self-Appraisal (DRAFT_SAVED, SUBMITTED)
Required fields:
- All Key Responsibilities: self comment + self rating
- All IDP items: self comment + self rating
- All Goals: self comment + self rating

#### Manager Review (REVIEW_DRAFT_SAVED, REVIEWED_AND_COMPLETED)
Required fields:
- All Key Responsibilities: manager comment + manager rating
- All IDP items: manager comment + manager rating
- All Goals: manager comment + manager rating
- Policy Adherence: all three ratings (HR Policy, Availability, Additional Support)
- Overall Evaluation: manager comments

## Usage Examples

### Creating Form Controls with Validation

```typescript
import { FormValidationService } from './form-validation.service';
import { FormRendererService } from './form-renderer.service';

// Inject services
constructor(
  private formRenderer: FormRendererService,
  private validation: FormValidationService
) {}

// Create a self-comment control with validation
const selfCommentControl = this.formRenderer.createSelfCommentControl(
  '', // initial value
  true // is required
);

// Create a rating control with validation
const selfRatingControl = this.formRenderer.createSelfRatingControl(
  null, // initial value
  true, // is required
  'competency' // rating scale
);
```

### Validating Before Submission

```typescript
// Validate employee submission
const employeeValidation = this.formRenderer.validateForEmployeeSubmission(formData);
if (!employeeValidation.valid) {
  console.error('Validation errors:', employeeValidation.errors);
  return;
}

// Validate manager review completion
const managerValidation = this.formRenderer.validateForManagerCompletion(formData);
if (!managerValidation.valid) {
  console.error('Validation errors:', managerValidation.errors);
  return;
}
```

### Displaying Validation Errors

```typescript
// Get error message for a control
if (control.invalid && control.touched) {
  const errorMessage = this.validation.getValidationErrorMessage(control.errors);
  // Display errorMessage to user
}

// Mark all fields as touched to show validation errors
this.validation.markAllAsTouched(formGroup);

// Get all validation errors from form
const allErrors = this.validation.getAllValidationErrors(formGroup);
```

## Integration with Field Components

All field components (TextFieldComponent, TextareaFieldComponent, RatingSelectorComponent) automatically display validation errors when:
1. The control is invalid
2. The control has been touched by the user

Error messages are generated using `getErrorMessage()` methods that check for specific validation errors.

## Validation Flow

### Employee Submission Flow
1. Employee fills out self-appraisal fields
2. Employee clicks "Submit"
3. System validates all self-appraisal fields are complete
4. If valid: Form status → SUBMITTED
5. If invalid: Display errors, prevent submission

### Manager Review Flow
1. Manager fills out review fields
2. Manager clicks "Complete Review"
3. System validates all manager review fields are complete
4. If valid: Form status → REVIEWED_AND_COMPLETED, generate PDF, send notifications
5. If invalid: Display errors, prevent completion

## Testing

Comprehensive unit tests are provided in `form-validation.service.spec.ts` covering:
- Competency rating validation
- Policy rating validation
- Comment validation (max length)
- Form status requirements
- Employee submission validation
- Manager review validation
- Error message generation
- Form group utilities

Run tests with:
```bash
ng test --include='**/form-validation.service.spec.ts'
```

## Best Practices

1. **Always validate before submission**: Use `validateForEmployeeSubmission()` or `validateForManagerCompletion()`
2. **Show validation errors early**: Mark fields as touched on blur or on submit attempt
3. **Provide clear error messages**: Use `getValidationErrorMessage()` for user-friendly messages
4. **Disable submit buttons**: When form is invalid to prevent invalid submissions
5. **Context-aware validation**: Check form status to determine which fields require validation

## Future Enhancements

Potential improvements for future iterations:
- Cross-field validation (e.g., ensure manager rating aligns with comments)
- Async validation (e.g., check for duplicate goals)
- Custom validation rules per template version
- Validation warnings vs errors (soft vs hard validation)
- Real-time validation feedback as user types
