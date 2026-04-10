# Form Field Components

Reusable field components for the dynamic form renderer. These components provide consistent styling and behavior across all appraisal form sections.

## Components

### TextFieldComponent

Single-line text input field for short text entries.

**Usage:**
```typescript
import { TextFieldComponent } from './fields';

// In template:
<app-text-field
  label="Employee Name"
  [control]="formGroup.get('employeeName')"
  [readonly]="!isEditable">
</app-text-field>
```

**Inputs:**
- `label: string` - Field label
- `control: FormControl` - Reactive form control
- `type: string` - Input type (default: 'text')
- `readonly: boolean` - Read-only mode (default: false)
- `placeholder: string` - Placeholder text
- `fieldClass: string` - Additional CSS classes

### TextareaFieldComponent

Multi-line textarea field for longer text entries like comments.

**Usage:**
```typescript
import { TextareaFieldComponent } from './fields';

// In template:
<app-textarea-field
  label="Team Member Comments"
  [control]="formGroup.get('selfComment')"
  [rows]="3"
  [readonly]="!editability.selfCommentEditable"
  [showCharCount]="true"
  [maxLength]="500">
</app-textarea-field>
```

**Inputs:**
- `label: string` - Field label
- `control: FormControl` - Reactive form control
- `rows: number` - Number of visible rows (default: 3)
- `readonly: boolean` - Read-only mode (default: false)
- `placeholder: string` - Placeholder text
- `fieldClass: string` - Additional CSS classes
- `showCharCount: boolean` - Show character counter (default: false)
- `maxLength: number` - Maximum character length

### RatingSelectorComponent

Rating selector supporting both competency and numeric scales.

**Usage:**
```typescript
import { RatingSelectorComponent } from './fields';

// In template:
// Competency rating (Excels, Exceeds, Meets, Developing)
<app-rating-selector
  label="Self Rating"
  [control]="formGroup.get('selfRating')"
  ratingScale="competency"
  [disabled]="!editability.selfRatingEditable"
  displayMode="dropdown">
</app-rating-selector>

// Policy rating (1-10)
<app-rating-selector
  label="HR Policy Adherence"
  [control]="formGroup.get('hrPolicyRating')"
  ratingScale="policy_1_10"
  [disabled]="!editability.managerRatingEditable"
  displayMode="toggle">
</app-rating-selector>
```

**Inputs:**
- `label: string` - Field label
- `control: FormControl` - Reactive form control
- `ratingScale: RatingScale` - 'competency' or 'policy_1_10' (default: 'competency')
- `disabled: boolean` - Disabled state (default: false)
- `fieldClass: string` - Additional CSS classes
- `displayMode: 'dropdown' | 'toggle'` - Display as dropdown or button toggle (default: 'dropdown')

## Rating Scales

### Competency Scale
- Excels
- Exceeds
- Meets
- Developing

### Policy Scale (1-10)
Numeric scale from 1 to 10 for policy adherence ratings.

## Example: Refactoring a Section Component

Before (inline fields):
```typescript
<mat-form-field appearance="outline">
  <mat-label>Team Member Comments</mat-label>
  <textarea 
    matInput 
    formControlName="selfComment"
    rows="3"
    [readonly]="!editability.selfCommentEditable">
  </textarea>
</mat-form-field>
```

After (using field component):
```typescript
<app-textarea-field
  label="Team Member Comments"
  [control]="itemForm.get('selfComment')"
  [rows]="3"
  [readonly]="!editability.selfCommentEditable">
</app-textarea-field>
```

## Benefits

- **Consistency**: Uniform styling and behavior across all forms
- **Validation**: Built-in error message handling
- **Accessibility**: Proper ARIA labels and form field associations
- **Maintainability**: Single source of truth for field styling
- **Reusability**: Use in any section component or custom form
