import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FieldEditability } from '../form-renderer.models';

@Component({
  selector: 'app-signature-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule
  ],
  template: `
    <div class="section-container signature-section" [formGroup]="formGroup">
      <h2 class="section-title">Signatures and Acknowledgement</h2>
      
      <div class="signature-grid">
        <div class="signature-block">
          <h3 class="block-title">Manager</h3>
          
          <mat-form-field appearance="outline">
            <mat-label>Prepared/Delivered By</mat-label>
            <input 
              matInput 
              formControlName="preparedBy"
              [readonly]="!editability.managerCommentEditable">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Reviewed By</mat-label>
            <input 
              matInput 
              formControlName="reviewedBy"
              [readonly]="!editability.managerCommentEditable">
          </mat-form-field>
        </div>

        <div class="signature-block">
          <h3 class="block-title">Team Member</h3>
          
          <mat-form-field appearance="outline">
            <mat-label>Acknowledgement</mat-label>
            <textarea 
              matInput 
              formControlName="teamMemberAcknowledgement"
              rows="4"
              placeholder="I acknowledge that I have reviewed this appraisal..."
              [readonly]="!editability.selfCommentEditable">
            </textarea>
          </mat-form-field>
        </div>
      </div>

      <div class="signature-note">
        <p><strong>Note:</strong> By providing your acknowledgement, you confirm that you have reviewed and discussed this appraisal with your manager.</p>
      </div>
    </div>
  `,
  styles: [`
    .signature-section {
      margin-bottom: 24px;
      padding: 20px;
      border: 2px solid #333;
      border-radius: 4px;
      background-color: #fafafa;
    }

    .section-title {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 20px;
      color: #333;
      text-align: center;
    }

    .signature-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 24px;
      margin-bottom: 20px;
    }

    .signature-block {
      padding: 16px;
      border: 1px solid #ddd;
      border-radius: 4px;
      background-color: white;
    }

    .block-title {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 12px;
      color: #1976d2;
      border-bottom: 2px solid #1976d2;
      padding-bottom: 8px;
    }

    mat-form-field {
      width: 100%;
      margin-bottom: 12px;
    }

    .signature-note {
      padding: 12px;
      background-color: #fff3cd;
      border: 1px solid #ffc107;
      border-radius: 4px;
      margin-top: 16px;
    }

    .signature-note p {
      margin: 0;
      font-size: 14px;
      color: #856404;
    }
  `]
})
export class SignatureSectionComponent {
  @Input() formGroup!: FormGroup;
  @Input() editability!: FieldEditability;
  @Input() readonly: boolean = false;
  @Input() canEditSelf: boolean = false;
  @Input() canEditManager: boolean = false;
}
