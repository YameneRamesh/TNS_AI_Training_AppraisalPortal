import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RenderedItem, FieldEditability } from '../form-renderer.models';

@Component({
  selector: 'app-key-responsibilities-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  template: `
    <div class="section-container key-responsibilities-section">
      <h2 class="section-title">{{ title }}</h2>
      
      <div class="responsibilities-list" *ngIf="itemsFormArray">
        <div *ngFor="let item of items; let i = index" class="responsibility-item">
          <h3 class="item-label">{{ item.label }}</h3>
          
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

    .full-width {
      grid-column: 1 / -1;
    }

    mat-form-field {
      width: 100%;
    }
  `]
})
export class KeyResponsibilitiesSectionComponent {
  @Input() title: string = 'Key Responsibilities';
  @Input() items: RenderedItem[] = [];
  @Input() formGroup!: FormGroup;
  @Input() readonly: boolean = false;
  @Input() canEditSelf: boolean = false;
  @Input() canEditManager: boolean = false;

  ratingOptions = ['Excels', 'Exceeds', 'Meets', 'Developing'];

  get itemsFormArray(): FormArray {
    return this.formGroup.get('keyResponsibilities') as FormArray;
  }

  getItemFormGroup(index: number): FormGroup {
    return this.itemsFormArray.at(index) as FormGroup;
  }
}
