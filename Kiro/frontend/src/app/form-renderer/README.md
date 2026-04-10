# Form Renderer Module

Dynamic form rendering service for the Employee Appraisal Cycle application.

## Overview

The `FormRendererService` interprets JSON template schemas stored in the database (`appraisal_templates.schema_json`) and provides utilities for rendering dynamic appraisal forms in Angular components.

## Key Features

- Parse and validate JSON template schemas
- Map template sections to renderable structures with data-binding keys
- Determine field editability based on user role and form status
- Provide rating options and descriptions
- Validate template completeness

## Usage

### 1. Parse Template Schema

```typescript
import { FormRendererService } from './form-renderer';

constructor(private formRenderer: FormRendererService) {}

const schema = this.formRenderer.parseTemplateSchema(template.schemaJson);
```

### 2. Render Sections

```typescript
const renderedSections = this.formRenderer.renderSections(schema);

// Each section contains:
// - sectionType: 'key_responsibilities' | 'idp' | 'goals' | etc.
// - title: Display title
// - items: Array of items with id, label, ratingScale, and dataKey
```

### 3. Determine Field Editability

```typescript
const editability = this.formRenderer.getFieldEditability(
  isEmployee,
  isManager,
  form.status
);

// Returns:
// {
//   selfCommentEditable: boolean,
//   selfRatingEditable: boolean,
//   managerCommentEditable: boolean,
//   managerRatingEditable: boolean
// }
```

### 4. Get Rating Options

```typescript
// For competency ratings
const competencyOptions = this.formRenderer.getRatingOptions('competency');
// Returns: ['Excels', 'Exceeds', 'Meets', 'Developing']

// For policy adherence (1-10 scale)
const policyOptions = this.formRenderer.getRatingOptions('policy_1_10');
// Returns: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
```

### 5. Validate Template

```typescript
const validation = this.formRenderer.validateTemplateCompleteness(schema);
if (!validation.valid) {
  console.error('Missing sections:', validation.missing);
}
```

## Data Binding Keys

The service generates data-binding keys that map to the `FormData` structure:

| Section Type | Data Key Pattern | Example |
|---|---|---|
| key_responsibilities | `keyResponsibilities[index]` | `keyResponsibilities[0]` |
| idp | `idp[index]` | `idp[1]` |
| goals | `goals[index]` | `goals[0]` |
| policy_adherence | `policyAdherence` | `policyAdherence` |

## Editability Rules

### Employee (Self-Appraisal)
- Can edit self fields when status is `NOT_STARTED` or `DRAFT_SAVED`
- Cannot edit after `SUBMITTED`

### Manager (Review)
- Can edit manager fields when status is `SUBMITTED`, `UNDER_REVIEW`, or `REVIEW_DRAFT_SAVED`
- Cannot edit after `REVIEWED_AND_COMPLETED`

## Template Schema Structure

```typescript
{
  "version": "3.0",
  "sections": [
    {
      "sectionType": "key_responsibilities",
      "title": "Key Responsibilities",
      "items": [
        {
          "id": "kr_1",
          "label": "Essential Duty 1",
          "ratingScale": "competency"
        }
      ]
    }
  ]
}
```

## Next Steps

This service provides the foundation for:
- Task 3.3.2: Section renderers (components for each section type)
- Task 3.3.3: Form field components (text, textarea, rating selectors)
- Task 3.3.4: Form validation logic
- Task 3.3.5: Form navigation and progress indicator
