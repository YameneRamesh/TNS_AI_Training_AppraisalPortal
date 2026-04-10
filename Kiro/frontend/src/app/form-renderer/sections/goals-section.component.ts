import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RenderedItem, FieldEditability } from '../form-renderer.models';

@Component({
  selector: 'app-goals-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  template: `
    <div class="section-container goals-section">
      <h2 class="section-title">{{ title }}</h2>
      <p class="section-description">Goals carried over from previous year</p>
      
      <div class="goals-list">
        <div *ngIf="itemsFormArray">
          <div *ngFor="let item of items; let i = index" class="goal-item">
            <h3 class="item-label">Goal {{ i + 1 }}: {{ item.label }}</h3>
            
            <div class="item-grid" [formGroup]="getItemFormGroup(i)">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Team Member Comments</mat-label>
                <textarea 
                  matInput 
                  formControlName="selfComment"
                  rows="3"
                  [readonly]="readonly || !canEditSelf">
                </textarea>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Self Rating</mat-label>
                <mat-select 
                  formControlName="selfRating"
                  [disabled]="readonly || !canEditSelf">
                  <mat-option *ngFor="let rating of ratingOptions" [value]="rating">
                    {{ rating }}
                  </mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Manager Comments</mat-label>
                <textarea 
                  matInput 
                  formControlName="managerComment"
                  rows="3"
                  [readonly]="readonly || !canEditManager">
                </textarea>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Manager Rating</mat-label>
                <mat-select 
                  formControlName="managerRating"
                  [disabled]="readonly || !canEditManager">
                  <mat-option *ngFor="let rating of ratingOptions" [value]="rating">
                    {{ rating }}
                  </mat-option>
                </mat-select>
              </mat-form-field>
            </div>
          </div>
        </div>

        <div class="next-year-goals" [formGroup]="formGroup">
          <h3 class="subsection-title">Next Year Goals</h3>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Document upcoming objectives</mat-label>
            <textarea 
              matInput 
              formControlName="nextYearGoals"
              rows="5"
              [readonly]="readonly || !canEditSelf">
            </textarea>
          </mat-form-field>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .goals-section {
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

    .goal-item {
      margin-bottom: 24px;
      padding: 16px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      background-color: #f0fff0;
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

    .full-width {
      grid-column: 1 / -1;
    }

    .next-year-goals {
      margin-top: 24px;
      padding: 16px;
      border: 2px solid #1976d2;
      border-radius: 4px;
      background-color: #e3f2fd;
    }

    .subsection-title {
      font-size: 18px;
      font-weight: 500;
      margin-bottom: 12px;
      color: #1976d2;
    }

    mat-form-field {
      width: 100%;
    }
  `]
})
export class GoalsSectionComponent {
  @Input() title: string = 'Goals';
  @Input() items: RenderedItem[] = [];
  @Input() formGroup!: FormGroup;
  @Input() readonly: boolean = false;
  @Input() canEditSelf: boolean = false;
  @Input() canEditManager: boolean = false;

  ratingOptions = ['Excels', 'Exceeds', 'Meets', 'Developing'];

  get itemsFormArray() {
    return this.formGroup.get('goals') as any;
  }

  getItemFormGroup(index: number): FormGroup {
    return this.itemsFormArray.at(index) as FormGroup;
  }
}
