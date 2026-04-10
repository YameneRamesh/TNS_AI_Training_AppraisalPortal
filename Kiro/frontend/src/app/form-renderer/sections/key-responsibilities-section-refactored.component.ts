import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RenderedItem, FieldEditability } from '../form-renderer.models';
import { TextareaFieldComponent } from '../fields/textarea-field.component';
import { RatingSelectorComponent } from '../fields/rating-selector.component';

/**
 * KeyResponsibilitiesSectionComponent (Refactored)
 * 
 * Example of using the reusable field components instead of inline Material components.
 * This demonstrates the cleaner, more maintainable approach.
 */
@Component({
  selector: 'app-key-responsibilities-section-refactored',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TextareaFieldComponent,
    RatingSelectorComponent
  ],
  template: `
    <div class="section-container key-responsibilities-section">
      <h2 class="section-title">{{ title }}</h2>
      
      <div class="responsibilities-list" [formGroup]="formGroup">
        <div *ngFor="let item of items; let i = index" class="responsibility-item">
          <h3 class="item-label">{{ item.label }}</h3>
          
          <div class="item-grid" [formGroupName]="i">
            <!-- Using reusable field components -->
            <app-textarea-field
              label="Team Member Comments"
              [control]="getItemControl(i, 'selfComment')"
              [rows]="3"
              [readonly]="!editability.selfCommentEditable"
              fieldClass="full-width">
            </app-textarea-field>

            <app-rating-selector
              label="Self Rating"
              [control]="getItemControl(i, 'selfRating')"
              ratingScale="competency"
              [disabled]="!editability.selfRatingEditable">
            </app-rating-selector>

            <app-textarea-field
              label="Manager Comments"
              [control]="getItemControl(i, 'managerComment')"
              [rows]="3"
              [readonly]="!editability.managerCommentEditable"
              fieldClass="full-width">
            </app-textarea-field>

            <app-rating-selector
              label="Manager Rating"
              [control]="getItemControl(i, 'managerRating')"
              ratingScale="competency"
              [disabled]="!editability.managerRatingEditable">
            </app-rating-selector>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .key-responsibilities-section {
      margin-bottom: 24px;
    }

    .section-title {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 16px;
      color: #333;
    }

    .responsibility-item {
      margin-bottom: 24px;
      padding: 16px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      background-color: #fafafa;
    }

    .item-label {
      font-size: 16px;
      font-weight: 500;
      margin-bottom: 12px;
      color: #555;
    }

    .item-grid {
      display: grid;
      grid-template-columns: 1fr 200px;
      gap: 16px;
    }

    :host ::ng-deep .full-width {
      grid-column: 1 / -1;
    }
  `]
})
export class KeyResponsibilitiesSectionRefactoredComponent {
  @Input() title: string = 'Key Responsibilities';
  @Input() items: RenderedItem[] = [];
  @Input() formGroup!: FormGroup;
  @Input() editability!: FieldEditability;

  /**
   * Helper method to get a specific control from the FormArray
   */
  getItemControl(index: number, controlName: string): FormControl {
    const formArray = this.formGroup.get(controlName) as FormArray ?? this.formGroup as unknown as FormArray;
    // The formGroup here IS the FormArray item group passed via [formGroupName]
    // Access via the parent formGroup treated as a FormArray
    const itemGroup = (this.formGroup as unknown as FormArray).at(index) as FormGroup;
    return (itemGroup.get(controlName) ?? new FormControl()) as FormControl;
  }
}
