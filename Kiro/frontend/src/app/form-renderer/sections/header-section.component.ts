import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';

@Component({
  selector: 'app-header-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule
  ],
  template: `
    <div class="section-container header-section">
      <h2 class="section-title">Appraisal Form Header</h2>
      
      <div class="header-grid" [formGroup]="headerFormGroup">
        <mat-form-field appearance="outline">
          <mat-label>Team Member Name</mat-label>
          <input matInput [value]="employeeName" readonly>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Date of Hire</mat-label>
          <input matInput formControlName="dateOfHire" [readonly]="readonly">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Designation</mat-label>
          <input matInput [value]="designation" readonly>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Date of Review</mat-label>
          <input matInput formControlName="dateOfReview" [readonly]="readonly">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Manager Name</mat-label>
          <input matInput [value]="managerName" readonly>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Review Period</mat-label>
          <input matInput formControlName="reviewPeriod" [readonly]="readonly">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Type of Review</mat-label>
          <input matInput formControlName="typeOfReview" [readonly]="readonly">
        </mat-form-field>
      </div>
    </div>
  `,
  styles: [`
    .header-section {
      margin-bottom: 24px;
    }

    .section-title {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 16px;
      color: #333;
    }

    .header-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;
    }

    mat-form-field {
      width: 100%;
    }
  `]
})
export class HeaderSectionComponent {
  @Input() formGroup!: FormGroup;
  @Input() formData: any = {};
  @Input() readonly: boolean = false;
  @Input() employeeName: string = '';
  @Input() managerName: string = '';
  @Input() designation: string = '';

  get headerFormGroup(): FormGroup {
    return this.formGroup.get('header') as FormGroup;
  }
}
