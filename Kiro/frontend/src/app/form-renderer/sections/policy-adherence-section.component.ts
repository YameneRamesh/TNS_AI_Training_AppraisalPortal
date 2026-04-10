import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RenderedItem, FieldEditability } from '../form-renderer.models';

@Component({
  selector: 'app-policy-adherence-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  template: `
    <div class="section-container policy-adherence-section">
      <h2 class="section-title">{{ title }}</h2>
      <p class="section-description">Rate each item on a scale of 1 to 10</p>
      
      <div class="policy-list" [formGroup]="formGroup">
        <div *ngFor="let item of items; let i = index" class="policy-item">
          <div class="policy-header">
            <span class="item-number">{{ i + 1 }}.</span>
            <h3 class="item-label">{{ item.label }}</h3>
          </div>
          
          <div class="item-grid" [formGroupName]="i">
            <mat-form-field appearance="outline">
              <mat-label>Manager Rating (1-10)</mat-label>
              <mat-select 
                formControlName="managerRating"
                [disabled]="!editability.managerRatingEditable">
                <mat-option *ngFor="let rating of ratingOptions" [value]="rating">
                  {{ rating }}
                </mat-option>
              </mat-select>
            </mat-form-field>
          </div>
        </div>

        <mat-form-field appearance="outline" class="full-width manager-comments">
          <mat-label>Manager's Comments</mat-label>
          <textarea 
            matInput 
            formControlName="managerComments"
            rows="4"
            [readonly]="!editability.managerCommentEditable">
          </textarea>
        </mat-form-field>
      </div>
    </div>
  `,
  styles: [`
    .policy-adherence-section {
      margin-bottom: 24px;
    }

    .section-title {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 8px;
      color: #333;
    }

    .section-description {
      font-size: 14px;
      color: #666;
      margin-bottom: 16px;
      font-style: italic;
    }

    .policy-item {
      margin-bottom: 16px;
      padding: 12px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      background-color: #fffef0;
    }

    .policy-header {
      display: flex;
      align-items: center;
      margin-bottom: 12px;
    }

    .item-number {
      font-size: 18px;
      font-weight: 600;
      color: #1976d2;
      margin-right: 8px;
    }

    .item-label {
      font-size: 15px;
      font-weight: 500;
      margin: 0;
      color: #555;
    }

    .item-grid {
      display: flex;
      justify-content: flex-start;
    }

    mat-form-field {
      width: 200px;
    }

    .manager-comments {
      width: 100%;
      margin-top: 16px;
    }
  `]
})
export class PolicyAdherenceSectionComponent {
  @Input() title: string = 'Company Policies and Business Continuity Support Adherence';
  @Input() items: RenderedItem[] = [];
  @Input() formGroup!: FormGroup;
  @Input() editability!: FieldEditability;
  @Input() readonly: boolean = false;
  @Input() canEditManager: boolean = false;

  ratingOptions = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
}
