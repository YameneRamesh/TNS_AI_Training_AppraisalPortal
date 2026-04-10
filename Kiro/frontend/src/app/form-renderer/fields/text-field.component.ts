import { Component, input, computed, ChangeDetectionStrategy } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

/**
 * TextFieldComponent
 * 
 * Reusable text input field component for single-line text entries.
 * Used for short text fields like names, dates, designations, etc.
 * 
 * Follows Angular 21 coding standards:
 * - Standalone component
 * - Signal-based inputs
 * - OnPush change detection
 */
@Component({
  selector: 'app-text-field',
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
      <input 
        matInput 
        [formControl]="control()"
        [type]="type()"
        [readonly]="readonly()"
        [placeholder]="placeholder()">
      @if (showError()) {
        <mat-error>{{ errorMessage() }}</mat-error>
      }
    </mat-form-field>
  `,
  styles: [`
    mat-form-field {
      width: 100%;
    }
  `]
})
export class TextFieldComponent {
  // Signal-based inputs (Angular 21 pattern)
  label = input<string>('');
  control = input.required<FormControl>();
  type = input<string>('text');
  readonly = input<boolean>(false);
  placeholder = input<string>('');
  fieldClass = input<string>('');

  // Computed signals for reactive error handling
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
    if (ctrl.hasError('email')) {
      return 'Please enter a valid email';
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
