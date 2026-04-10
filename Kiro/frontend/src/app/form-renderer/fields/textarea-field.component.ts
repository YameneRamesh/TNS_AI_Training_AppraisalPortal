import { Component, input, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

/**
 * TextareaFieldComponent
 * 
 * Reusable textarea field component for multi-line text entries.
 * Used for comments, descriptions, and longer text content in appraisal forms.
 * 
 * Follows Angular 21 coding standards:
 * - Standalone component
 * - Signal-based inputs
 * - OnPush change detection
 */
@Component({
  selector: 'app-textarea-field',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-form-field appearance="outline" [class]="fieldClass()">
      <mat-label>{{ label() }}</mat-label>
      <textarea 
        matInput 
        [formControl]="control()"
        [rows]="rows()"
        [readonly]="readonly()"
        [placeholder]="placeholder()">
      </textarea>
      @if (showCharCount() && maxLength()) {
        <mat-hint>{{ charCount() }} / {{ maxLength() }}</mat-hint>
      }
      @if (showError()) {
        <mat-error>{{ errorMessage() }}</mat-error>
      }
    </mat-form-field>
  `,
  styles: [`
    mat-form-field {
      width: 100%;
    }

    textarea {
      resize: vertical;
      min-height: 60px;
    }
  `]
})
export class TextareaFieldComponent {
  // Signal-based inputs (Angular 21 pattern)
  label = input<string>('');
  control = input.required<FormControl>();
  rows = input<number>(3);
  readonly = input<boolean>(false);
  placeholder = input<string>('');
  fieldClass = input<string>('');
  showCharCount = input<boolean>(false);
  maxLength = input<number | undefined>(undefined);

  // Computed signals for reactive behavior
  charCount = computed(() => {
    const value = this.control().value;
    return value ? value.length : 0;
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
    if (ctrl.hasError('minlength')) {
      const minLength = ctrl.getError('minlength').requiredLength;
      return `Minimum length is ${minLength} characters`;
    }
    if (ctrl.hasError('maxlength')) {
      const maxLength = ctrl.getError('maxlength').requiredLength;
      return `Maximum length is ${maxLength} characters`;
    }
    return 'Invalid input';
  });
}
