import { Component, input, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RatingScale } from '../form-renderer.models';

/**
 * RatingSelectorComponent
 * 
 * Reusable rating selector component that supports two rating scales:
 * - Competency: Excels, Exceeds, Meets, Developing
 * - Policy (1-10): Numeric scale from 1 to 10
 * 
 * Can render as either a dropdown (default) or button toggle group.
 * 
 * Follows Angular 21 coding standards:
 * - Standalone component
 * - Signal-based inputs
 * - OnPush change detection
 */
@Component({
  selector: 'app-rating-selector',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonToggleModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (displayMode() === 'dropdown') {
      <!-- Dropdown mode -->
      <mat-form-field appearance="outline" [class]="fieldClass()">
        <mat-label>{{ label() }}</mat-label>
        <mat-select 
          [formControl]="control()"
          [disabled]="disabled()">
          @for (option of ratingOptions(); track option) {
            <mat-option [value]="option">
              {{ option }}
            </mat-option>
          }
        </mat-select>
        @if (showError()) {
          <mat-error>{{ errorMessage() }}</mat-error>
        }
      </mat-form-field>
    } @else {
      <!-- Button toggle mode -->
      <div class="toggle-container">
        <label class="toggle-label">{{ label() }}</label>
        <mat-button-toggle-group 
          [formControl]="control()"
          [disabled]="disabled()"
          class="rating-toggle-group">
          @for (option of ratingOptions(); track option) {
            <mat-button-toggle 
              [value]="option"
              [class.selected]="control().value === option">
              {{ option }}
            </mat-button-toggle>
          }
        </mat-button-toggle-group>
        @if (showError()) {
          <div class="error-message">{{ errorMessage() }}</div>
        }
      </div>
    }
  `,
  styles: [`
    mat-form-field {
      width: 100%;
    }

    .toggle-container {
      margin-bottom: 16px;
    }

    .toggle-label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.6);
      margin-bottom: 8px;
    }

    .rating-toggle-group {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }

    mat-button-toggle {
      min-width: 80px;
    }

    mat-button-toggle.selected {
      background-color: #3f51b5;
      color: white;
    }

    .error-message {
      color: #f44336;
      font-size: 12px;
      margin-top: 4px;
    }
  `]
})
export class RatingSelectorComponent {
  // Signal-based inputs (Angular 21 pattern)
  label = input<string>('');
  control = input.required<FormControl>();
  ratingScale = input<RatingScale>('competency');
  disabled = input<boolean>(false);
  fieldClass = input<string>('');
  displayMode = input<'dropdown' | 'toggle'>('dropdown');

  // Computed signal for rating options
  ratingOptions = computed(() => {
    if (this.ratingScale() === 'competency') {
      return ['Excels', 'Exceeds', 'Meets', 'Developing'];
    } else {
      return [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
    }
  });

  showError = computed(() => {
    const ctrl = this.control();
    return ctrl.invalid && ctrl.touched;
  });

  errorMessage = computed(() => {
    const ctrl = this.control();
    const labelText = this.label();
    
    if (ctrl.hasError('required')) {
      return `${labelText} is required`;
    }
    return 'Invalid selection';
  });
}
