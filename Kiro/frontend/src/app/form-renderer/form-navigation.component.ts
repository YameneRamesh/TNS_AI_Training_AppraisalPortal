import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SectionType, RenderedSection } from './form-renderer.models';
import { FormStatus } from '../core/models';

/**
 * FormNavigationComponent
 * 
 * Provides section-by-section navigation and progress tracking for the appraisal form.
 * 
 * Features:
 * - Visual progress indicator showing completion percentage
 * - Section navigation with previous/next buttons
 * - Section status indicators (not started, in progress, completed)
 * - Jump-to-section capability
 * - Responsive layout for mobile and desktop
 */
@Component({
  selector: 'app-form-navigation',
  standalone: true,
  imports: [
    CommonModule,
    MatStepperModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule
  ],
  template: `
    <div class="form-navigation">
      <!-- Progress Bar -->
      <div class="progress-section">
        <div class="progress-header">
          <span class="progress-label">Form Progress</span>
          <span class="progress-percentage">{{ progressPercentage }}%</span>
        </div>
        <mat-progress-bar 
          mode="determinate" 
          [value]="progressPercentage"
          [color]="progressPercentage === 100 ? 'accent' : 'primary'">
        </mat-progress-bar>
        <div class="progress-details">
          <span>{{ completedSections }} of {{ totalSections }} sections completed</span>
        </div>
      </div>

      <!-- Section Navigation -->
      <div class="section-navigation">
        <div class="section-list">
          <div 
            *ngFor="let section of navigableSections; let i = index"
            class="section-item"
            [class.active]="currentSectionIndex === i"
            [class.completed]="isSectionCompleted(section)"
            [class.in-progress]="currentSectionIndex === i && !isSectionCompleted(section)"
            (click)="navigateToSection(i)"
            [matTooltip]="getSectionTooltip(section)"
            matTooltipPosition="right">
            
            <div class="section-indicator">
              <mat-icon *ngIf="isSectionCompleted(section)" class="status-icon completed">
                check_circle
              </mat-icon>
              <mat-icon *ngIf="!isSectionCompleted(section) && currentSectionIndex === i" class="status-icon in-progress">
                radio_button_checked
              </mat-icon>
              <mat-icon *ngIf="!isSectionCompleted(section) && currentSectionIndex !== i" class="status-icon not-started">
                radio_button_unchecked
              </mat-icon>
            </div>
            
            <div class="section-info">
              <div class="section-number">{{ i + 1 }}</div>
              <div class="section-title">{{ getSectionDisplayName(section.sectionType) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Navigation Controls -->
      <div class="navigation-controls">
        <button 
          mat-stroked-button 
          color="primary"
          [disabled]="!canNavigatePrevious()"
          (click)="navigatePrevious()">
          <mat-icon>arrow_back</mat-icon>
          Previous
        </button>
        
        <div class="current-section-indicator">
          Section {{ currentSectionIndex + 1 }} of {{ totalSections }}
        </div>
        
        <button 
          mat-stroked-button 
          color="primary"
          [disabled]="!canNavigateNext()"
          (click)="navigateNext()">
          Next
          <mat-icon>arrow_forward</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .form-navigation {
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      padding: 20px;
      margin-bottom: 24px;
    }

    .progress-section {
      margin-bottom: 24px;
    }

    .progress-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .progress-label {
      font-weight: 500;
      font-size: 14px;
      color: #333;
    }

    .progress-percentage {
      font-weight: 600;
      font-size: 16px;
      color: #1976d2;
    }

    .progress-details {
      margin-top: 8px;
      font-size: 12px;
      color: #666;
      text-align: center;
    }

    .section-navigation {
      margin-bottom: 24px;
      max-height: 400px;
      overflow-y: auto;
    }

    .section-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .section-item {
      display: flex;
      align-items: center;
      padding: 12px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
      border: 1px solid #e0e0e0;
      background: #fafafa;
    }

    .section-item:hover {
      background: #f5f5f5;
      border-color: #1976d2;
    }

    .section-item.active {
      background: #e3f2fd;
      border-color: #1976d2;
      box-shadow: 0 2px 4px rgba(25, 118, 210, 0.2);
    }

    .section-item.completed {
      background: #e8f5e9;
      border-color: #4caf50;
    }

    .section-item.in-progress {
      background: #fff3e0;
      border-color: #ff9800;
    }

    .section-indicator {
      margin-right: 12px;
    }

    .status-icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .status-icon.completed {
      color: #4caf50;
    }

    .status-icon.in-progress {
      color: #ff9800;
    }

    .status-icon.not-started {
      color: #bdbdbd;
    }

    .section-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
    }

    .section-number {
      font-weight: 600;
      font-size: 14px;
      color: #666;
      min-width: 24px;
    }

    .section-title {
      font-size: 14px;
      color: #333;
      font-weight: 500;
    }

    .navigation-controls {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;
    }

    .current-section-indicator {
      font-size: 14px;
      color: #666;
      font-weight: 500;
    }

    /* Responsive design */
    @media (max-width: 768px) {
      .form-navigation {
        padding: 16px;
      }

      .section-item {
        padding: 10px;
      }

      .section-title {
        font-size: 13px;
      }

      .navigation-controls button {
        font-size: 12px;
        padding: 6px 12px;
      }

      .current-section-indicator {
        font-size: 12px;
      }
    }
  `]
})
export class FormNavigationComponent implements OnInit, OnChanges {
  @Input() sections: RenderedSection[] = [];
  @Input() currentSectionIndex: number = 0;
  @Input() formStatus: FormStatus = 'NOT_STARTED';
  @Input() sectionCompletionStatus: Map<SectionType, boolean> = new Map();
  @Input() canSaveDraft: boolean = false;
  @Input() canSubmit: boolean = false;
  @Input() readonly: boolean = false;
  @Input() saving: boolean = false;

  @Output() sectionChange = new EventEmitter<number>();
  @Output() navigationRequest = new EventEmitter<'previous' | 'next'>();
  @Output() saveDraft = new EventEmitter<void>();
  @Output() submit = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  navigableSections: RenderedSection[] = [];
  totalSections: number = 0;
  completedSections: number = 0;
  progressPercentage: number = 0;

  ngOnInit(): void {
    this.initializeNavigation();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sections'] || changes['sectionCompletionStatus']) {
      this.initializeNavigation();
    }
  }

  private initializeNavigation(): void {
    // Filter out non-navigable sections (like rating_key which is display-only)
    this.navigableSections = this.sections.filter(s => 
      s.sectionType !== 'rating_key'
    );
    
    this.totalSections = this.navigableSections.length;
    this.calculateProgress();
  }

  private calculateProgress(): void {
    this.completedSections = 0;
    
    this.navigableSections.forEach(section => {
      if (this.isSectionCompleted(section)) {
        this.completedSections++;
      }
    });

    this.progressPercentage = this.totalSections > 0 
      ? Math.round((this.completedSections / this.totalSections) * 100)
      : 0;
  }

  isSectionCompleted(section: RenderedSection): boolean {
    return this.sectionCompletionStatus.get(section.sectionType) || false;
  }

  getSectionDisplayName(sectionType: SectionType): string {
    const displayNames: Record<SectionType, string> = {
      'header': 'Header Information',
      'rating_key': 'Rating Key',
      'overall_evaluation': 'Overall Evaluation',
      'key_responsibilities': 'Key Responsibilities',
      'idp': 'Individual Development Plan',
      'policy_adherence': 'Policy Adherence',
      'goals': 'Goals',
      'next_year_goals': 'Next Year Goals',
      'signature': 'Signature'
    };

    return displayNames[sectionType] || sectionType;
  }

  getSectionTooltip(section: RenderedSection): string {
    if (this.isSectionCompleted(section)) {
      return 'Section completed - Click to review';
    } else if (this.navigableSections[this.currentSectionIndex]?.sectionType === section.sectionType) {
      return 'Current section';
    } else {
      return 'Click to navigate to this section';
    }
  }

  navigateToSection(index: number): void {
    if (index >= 0 && index < this.navigableSections.length) {
      this.sectionChange.emit(index);
    }
  }

  canNavigatePrevious(): boolean {
    return this.currentSectionIndex > 0;
  }

  canNavigateNext(): boolean {
    return this.currentSectionIndex < this.totalSections - 1;
  }

  navigatePrevious(): void {
    if (this.canNavigatePrevious()) {
      this.navigationRequest.emit('previous');
      this.navigateToSection(this.currentSectionIndex - 1);
    }
  }

  navigateNext(): void {
    if (this.canNavigateNext()) {
      this.navigationRequest.emit('next');
      this.navigateToSection(this.currentSectionIndex + 1);
    }
  }

  getCurrentSectionType(): SectionType | null {
    return this.navigableSections[this.currentSectionIndex]?.sectionType || null;
  }

  getCompletionSummary(): string {
    return `${this.completedSections} of ${this.totalSections} sections completed (${this.progressPercentage}%)`;
  }
}
