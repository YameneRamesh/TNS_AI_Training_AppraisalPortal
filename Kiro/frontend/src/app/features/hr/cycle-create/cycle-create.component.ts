import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { CycleService } from '../../../core/services/cycle.service';
import { TemplateService } from '../../../core/services/template.service';
import { AppraisalTemplate } from '../../../core/models/appraisal.model';

/**
 * Component for creating a new appraisal cycle.
 * Allows HR to configure cycle name, dates, and select template.
 */
@Component({
  selector: 'app-cycle-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule
  ],
  templateUrl: './cycle-create.component.html',
  styleUrl: './cycle-create.component.scss'
})
export class CycleCreateComponent implements OnInit {
  cycleForm: FormGroup;
  templates: AppraisalTemplate[] = [];
  loading = false;
  submitting = false;
  minStartDate = new Date();

  constructor(
    private fb: FormBuilder,
    private cycleService: CycleService,
    private templateService: TemplateService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.cycleForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      templateId: ['', Validators.required]
    }, { validators: this.dateRangeValidator });
  }

  ngOnInit(): void {
    this.loadTemplates();
  }

  /**
   * Load available templates
   */
  loadTemplates(): void {
    this.loading = true;
    this.templateService.getTemplates().subscribe({
      next: (response) => {
        this.templates = response.data || [];
        this.loading = false;
        
        // Auto-select active template if available
        const activeTemplate = this.templates.find(t => t.isActive);
        if (activeTemplate) {
          this.cycleForm.patchValue({ templateId: activeTemplate.id });
        }
      },
      error: (err) => {
        this.loading = false;
        console.error('Error loading templates:', err);
        this.snackBar.open('Failed to load templates', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Custom validator to ensure end date is after start date
   */
  dateRangeValidator(group: FormGroup): { [key: string]: boolean } | null {
    const startDate = group.get('startDate')?.value;
    const endDate = group.get('endDate')?.value;
    
    if (startDate && endDate && new Date(endDate) <= new Date(startDate)) {
      return { dateRangeInvalid: true };
    }
    return null;
  }

  /**
   * Get minimum end date (must be after start date)
   */
  get minEndDate(): Date | null {
    const startDate = this.cycleForm.get('startDate')?.value;
    if (startDate) {
      const minDate = new Date(startDate);
      minDate.setDate(minDate.getDate() + 1);
      return minDate;
    }
    return this.minStartDate;
  }

  /**
   * Submit form to create cycle
   */
  onSubmit(): void {
    if (this.cycleForm.invalid) {
      this.markFormGroupTouched(this.cycleForm);
      return;
    }

    this.submitting = true;
    const formValue = this.cycleForm.value;
    
    const cycleData = {
      name: formValue.name,
      startDate: this.formatDate(formValue.startDate),
      endDate: this.formatDate(formValue.endDate),
      templateId: formValue.templateId,
      status: 'DRAFT' as const
    };

    this.cycleService.createCycle(cycleData).subscribe({
      next: (response) => {
        this.submitting = false;
        this.snackBar.open('Cycle created successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/hr/cycles']);
      },
      error: (err) => {
        this.submitting = false;
        console.error('Error creating cycle:', err);
        const errorMessage = err.error?.message || 'Failed to create cycle. Please try again.';
        this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
      }
    });
  }

  /**
   * Cancel and navigate back
   */
  onCancel(): void {
    this.router.navigate(['/hr/cycles']);
  }

  /**
   * Format date to ISO string (YYYY-MM-DD)
   */
  private formatDate(date: Date): string {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Mark all form controls as touched to show validation errors
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  /**
   * Get error message for form field
   */
  getErrorMessage(fieldName: string): string {
    const control = this.cycleForm.get(fieldName);
    
    if (control?.hasError('required')) {
      return `${this.getFieldLabel(fieldName)} is required`;
    }
    
    if (control?.hasError('maxlength')) {
      const maxLength = control.errors?.['maxlength'].requiredLength;
      return `${this.getFieldLabel(fieldName)} cannot exceed ${maxLength} characters`;
    }
    
    if (fieldName === 'endDate' && this.cycleForm.hasError('dateRangeInvalid')) {
      return 'End date must be after start date';
    }
    
    return '';
  }

  /**
   * Get human-readable field label
   */
  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      name: 'Cycle name',
      startDate: 'Start date',
      endDate: 'End date',
      templateId: 'Template'
    };
    return labels[fieldName] || fieldName;
  }
}
