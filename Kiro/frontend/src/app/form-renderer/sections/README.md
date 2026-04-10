# Form Renderer Section Components

This directory contains standalone Angular components for rendering each section of the TnS Appraisal Form V3.0.

## Components

### HeaderSectionComponent
Renders the form header with read-only fields:
- Team Member Name
- Date of Hire
- Designation
- Date of Review
- Manager Name
- Review Period
- Type of Review

**Usage:**
```typescript
<app-header-section [formGroup]="headerFormGroup"></app-header-section>
```

### RatingKeySectionComponent
Displays the rating key legend with descriptions for:
- Excels
- Exceeds
- Meets
- Developing

**Usage:**
```typescript
<app-rating-key-section></app-rating-key-section>
```

### KeyResponsibilitiesSectionComponent
Renders 3-5 key responsibility items with:
- Team Member Comments (textarea)
- Self Rating (competency scale)
- Manager Comments (textarea)
- Manager Rating (competency scale)

**Usage:**
```typescript
<app-key-responsibilities-section
  [title]="'Key Responsibilities'"
  [items]="renderedItems"
  [formGroup]="responsibilitiesFormArray"
  [editability]="fieldEditability">
</app-key-responsibilities-section>
```

### IdpSectionComponent
Renders Individual Development Plan items (NextGen Tech Skills, Value Addition, Leadership) with:
- Team Member Comments (textarea)
- Self Rating (competency scale)
- Manager Comments (textarea)
- Manager Rating (competency scale)

**Usage:**
```typescript
<app-idp-section
  [title]="'Individual Development Plan'"
  [items]="renderedItems"
  [formGroup]="idpFormArray"
  [editability]="fieldEditability">
</app-idp-section>
```

### PolicyAdherenceSectionComponent
Renders policy adherence items rated on 1-10 scale:
- Follow HR Policy
- Team Member Availability During Critical Deliverables
- Additional Support Beyond Regular Work Assignments
- Manager's Comments (textarea)

**Usage:**
```typescript
<app-policy-adherence-section
  [title]="'Company Policies and Business Continuity Support Adherence'"
  [items]="renderedItems"
  [formGroup]="policyFormGroup"
  [editability]="fieldEditability">
</app-policy-adherence-section>
```

### GoalsSectionComponent
Renders goals from previous year and next year goals:
- Previous year goals with comments and ratings
- Next Year Goals (textarea)

**Usage:**
```typescript
<app-goals-section
  [title]="'Goals'"
  [items]="renderedItems"
  [formGroup]="goalsFormGroup"
  [editability]="fieldEditability">
</app-goals-section>
```

### SignatureSectionComponent
Renders signature and acknowledgement fields:
- Manager: Prepared/Delivered By, Reviewed By
- Team Member: Acknowledgement

**Usage:**
```typescript
<app-signature-section
  [formGroup]="signatureFormGroup"
  [editability]="fieldEditability">
</app-signature-section>
```

## Common Inputs

All section components (except RatingKeySectionComponent) accept:

- `formGroup`: FormGroup or FormArray containing the section's form controls
- `editability`: FieldEditability object determining which fields are editable based on user role and form status

Components with items (KeyResponsibilities, IDP, PolicyAdherence, Goals) also accept:

- `items`: RenderedItem[] array from the template schema
- `title`: Section title (optional, has defaults)

## Field Editability

The `FieldEditability` interface controls which fields are editable:

```typescript
interface FieldEditability {
  selfCommentEditable: boolean;
  selfRatingEditable: boolean;
  managerCommentEditable: boolean;
  managerRatingEditable: boolean;
}
```

This is determined by the `FormRendererService.getFieldEditability()` method based on:
- User role (Employee, Manager, Backup Reviewer)
- Form status (NOT_STARTED, DRAFT_SAVED, SUBMITTED, UNDER_REVIEW, etc.)

## Styling

All components use:
- Angular Material form fields and components
- Consistent spacing and layout
- Responsive grid layouts
- Color-coded backgrounds for different section types
- Read-only styling for non-editable fields

## Integration

These components are designed to be used within a parent form component that:
1. Fetches the appraisal form data and template schema
2. Builds the reactive form structure using FormBuilder
3. Determines field editability based on user role and form status
4. Renders sections in the correct order using the section components
5. Handles form submission and validation
