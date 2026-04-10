# Form Navigation Component

## Overview

The `FormNavigationComponent` provides a comprehensive navigation and progress tracking interface for the appraisal form. It displays section-by-section progress, allows users to navigate between sections, and provides visual feedback on completion status.

## Features

- **Progress Bar**: Visual indicator showing overall form completion percentage
- **Section List**: Clickable list of all form sections with status indicators
- **Navigation Controls**: Previous/Next buttons for sequential navigation
- **Status Indicators**: Visual markers for completed, in-progress, and not-started sections
- **Responsive Design**: Adapts to mobile and desktop layouts
- **Accessibility**: Tooltips and ARIA-compliant controls

## Usage

### Basic Implementation

```typescript
import { Component } from '@angular/core';
import { FormNavigationComponent } from './form-renderer';
import { RenderedSection, SectionType } from './form-renderer/form-renderer.models';

@Component({
  selector: 'app-appraisal-form',
  template: `
    <app-form-navigation
      [sections]="renderedSections"
      [currentSectionIndex]="currentSection"
      [formStatus]="formStatus"
      [sectionCompletionStatus]="completionMap"
      (sectionChange)="onSectionChange($event)"
      (navigationRequest)="onNavigationRequest($event)">
    </app-form-navigation>
    
    <!-- Your form sections here -->
  `
})
export class AppraisalFormComponent {
  renderedSections: RenderedSection[] = [];
  currentSection: number = 0;
  formStatus: FormStatus = 'NOT_STARTED';
  completionMap: Map<SectionType, boolean> = new Map();

  onSectionChange(index: number): void {
    this.currentSection = index;
    // Scroll to section, update view, etc.
  }

  onNavigationRequest(direction: 'previous' | 'next'): void {
    // Handle navigation logic
    console.log(`User requested ${direction} navigation`);
  }
}
```

### Tracking Section Completion

The component requires a `Map<SectionType, boolean>` to track which sections are completed. You should update this map based on your validation logic:

```typescript
updateSectionCompletion(): void {
  const completionMap = new Map<SectionType, boolean>();
  
  // Example: Mark header as complete if all required fields are filled
  if (this.isHeaderComplete()) {
    completionMap.set('header', true);
  }
  
  // Example: Mark key responsibilities as complete if all items have ratings
  if (this.areKeyResponsibilitiesComplete()) {
    completionMap.set('key_responsibilities', true);
  }
  
  this.completionMap = completionMap;
}

private isHeaderComplete(): boolean {
  const header = this.formData?.header;
  return !!(header?.dateOfHire && header?.dateOfReview && header?.reviewPeriod);
}

private areKeyResponsibilitiesComplete(): boolean {
  const items = this.formData?.keyResponsibilities || [];
  return items.every(item => 
    item.selfComment && item.selfRating
  );
}
```

### Integration with Form Renderer Service

```typescript
import { FormRendererService } from './form-renderer';

export class AppraisalFormComponent implements OnInit {
  constructor(private formRenderer: FormRendererService) {}

  ngOnInit(): void {
    // Parse template and get rendered sections
    const schema = this.formRenderer.parseTemplateSchema(this.template.schemaJson);
    this.renderedSections = this.formRenderer.renderSections(schema);
    
    // Initialize completion tracking
    this.updateSectionCompletion();
  }
}
```

## Component API

### Inputs

| Property | Type | Description |
|----------|------|-------------|
| `sections` | `RenderedSection[]` | Array of rendered form sections |
| `currentSectionIndex` | `number` | Index of the currently active section (0-based) |
| `formStatus` | `FormStatus` | Current status of the appraisal form |
| `sectionCompletionStatus` | `Map<SectionType, boolean>` | Map tracking completion status of each section |

### Outputs

| Event | Type | Description |
|-------|------|-------------|
| `sectionChange` | `EventEmitter<number>` | Emitted when user navigates to a different section (emits section index) |
| `navigationRequest` | `EventEmitter<'previous' \| 'next'>` | Emitted when user clicks Previous/Next buttons |

### Public Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `isSectionCompleted(section)` | `boolean` | Check if a specific section is marked as completed |
| `getSectionDisplayName(sectionType)` | `string` | Get human-readable name for a section type |
| `canNavigatePrevious()` | `boolean` | Check if previous navigation is available |
| `canNavigateNext()` | `boolean` | Check if next navigation is available |
| `getCurrentSectionType()` | `SectionType \| null` | Get the section type of the current section |
| `getCompletionSummary()` | `string` | Get a text summary of completion status |

## Section Status Indicators

The component displays three visual states for each section:

1. **Completed** (green check icon): Section is marked as complete in `sectionCompletionStatus`
2. **In Progress** (orange radio icon): Section is currently active but not completed
3. **Not Started** (gray radio icon): Section has not been visited or completed

## Progress Calculation

Progress is calculated as:
```
progressPercentage = (completedSections / totalNavigableSections) * 100
```

Note: The `rating_key` section is excluded from navigation and progress calculation as it's display-only.

## Styling Customization

The component uses Angular Material theming. You can customize colors by overriding CSS classes:

```scss
// In your component styles
::ng-deep .form-navigation {
  .section-item.active {
    background: #your-color;
    border-color: #your-border-color;
  }
  
  .progress-percentage {
    color: #your-accent-color;
  }
}
```

## Accessibility

- All interactive elements are keyboard-navigable
- Tooltips provide context for screen readers
- Progress information is announced via ARIA labels
- Color is not the only indicator of status (icons are used)

## Mobile Responsiveness

The component automatically adjusts for mobile devices:
- Reduced padding and font sizes
- Touch-friendly tap targets
- Scrollable section list for small screens

## Example: Complete Integration

```typescript
import { Component, OnInit } from '@angular/core';
import { FormNavigationComponent } from './form-renderer';
import { FormRendererService } from './form-renderer';
import { AppraisalForm, FormData, FormStatus } from './core/models';

@Component({
  selector: 'app-employee-appraisal',
  template: `
    <div class="appraisal-container">
      <app-form-navigation
        [sections]="renderedSections"
        [currentSectionIndex]="currentSectionIndex"
        [formStatus]="form.status"
        [sectionCompletionStatus]="sectionCompletion"
        (sectionChange)="navigateToSection($event)"
        (navigationRequest)="handleNavigation($event)">
      </app-form-navigation>

      <div class="form-content">
        <div *ngIf="currentSection" class="section-container">
          <!-- Render current section based on type -->
          <app-header-section 
            *ngIf="currentSection.sectionType === 'header'"
            [formData]="formData"
            [editable]="canEdit">
          </app-header-section>
          
          <!-- Other section components... -->
        </div>
      </div>

      <div class="form-actions">
        <button mat-raised-button (click)="saveDraft()">Save Draft</button>
        <button mat-raised-button color="primary" (click)="submitForm()">Submit</button>
      </div>
    </div>
  `
})
export class EmployeeAppraisalComponent implements OnInit {
  form!: AppraisalForm;
  formData: FormData = {};
  renderedSections: RenderedSection[] = [];
  currentSectionIndex: number = 0;
  sectionCompletion: Map<SectionType, boolean> = new Map();

  constructor(private formRenderer: FormRendererService) {}

  ngOnInit(): void {
    this.loadForm();
    this.initializeSections();
  }

  get currentSection(): RenderedSection | undefined {
    return this.renderedSections[this.currentSectionIndex];
  }

  get canEdit(): boolean {
    return this.form.status === 'NOT_STARTED' || this.form.status === 'DRAFT_SAVED';
  }

  navigateToSection(index: number): void {
    this.currentSectionIndex = index;
    this.scrollToSection();
  }

  handleNavigation(direction: 'previous' | 'next'): void {
    // Auto-save on navigation
    this.saveDraft();
  }

  private initializeSections(): void {
    const schema = this.formRenderer.parseTemplateSchema(this.form.template.schemaJson);
    this.renderedSections = this.formRenderer.renderSections(schema);
    this.updateSectionCompletion();
  }

  private updateSectionCompletion(): void {
    // Implement your completion logic here
    this.sectionCompletion = new Map([
      ['header', this.isHeaderComplete()],
      ['key_responsibilities', this.areKeyResponsibilitiesComplete()],
      // ... other sections
    ]);
  }

  private scrollToSection(): void {
    // Smooth scroll to section
    const element = document.querySelector('.section-container');
    element?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  private loadForm(): void {
    // Load form from API
  }

  saveDraft(): void {
    // Save draft logic
    this.updateSectionCompletion();
  }

  submitForm(): void {
    // Submit form logic
  }
}
```

## Best Practices

1. **Update Completion Status Frequently**: Call `updateSectionCompletion()` after any form data change
2. **Validate Before Marking Complete**: Only mark sections as complete when all required fields are filled
3. **Handle Navigation Events**: Use `navigationRequest` to auto-save or validate before allowing navigation
4. **Provide Feedback**: Show loading states or success messages when navigating between sections
5. **Preserve State**: Save the current section index so users can resume where they left off

## Testing

The component includes comprehensive unit tests. Run them with:

```bash
ng test --include='**/form-navigation.component.spec.ts'
```

Key test scenarios covered:
- Progress calculation with various completion states
- Navigation boundary conditions (first/last section)
- Section filtering (excluding rating_key)
- Event emission on navigation
- Responsive to input changes
