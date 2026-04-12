import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TemplateService } from '../../../core/services/template.service';
import { AppraisalTemplate } from '../../../core/models/appraisal.model';
import { DialogService } from '../../../shared/services/dialog.service';

/**
 * Component for displaying list of appraisal templates.
 * Accessible by HR and Admin roles.
 */
@Component({
  selector: 'app-template-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './template-list.component.html',
  styleUrl: './template-list.component.scss'
})
export class TemplateListComponent implements OnInit {
  templates: AppraisalTemplate[] = [];
  displayedColumns: string[] = ['version', 'isActive', 'createdAt', 'actions'];
  loading = false;
  error: string | null = null;

  constructor(
    private templateService: TemplateService,
    private router: Router,
    private dialogService: DialogService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  /**
   * Load all templates from the backend
   */
  loadTemplates(): void {
    this.loading = true;
    this.error = null;

    this.templateService.getTemplates().subscribe({
      next: (response) => {
        this.templates = response.data || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Failed to load templates. Please try again.';
        this.loading = false;
        console.error('Error loading templates:', err);
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Navigate to template viewer
   */
  viewTemplate(template: AppraisalTemplate): void {
    this.router.navigate(['/hr/templates', template.id]);
  }

  /**
   * Activate a template
   */
  activateTemplate(template: AppraisalTemplate): void {
    const message = `Are you sure you want to activate template version ${template.version}? This will deactivate all other templates.`;
    
    this.dialogService.confirmAction(message, 'Activate Template').subscribe(confirmed => {
      if (confirmed) {
        this.templateService.activateTemplate(template.id).subscribe({
          next: (response) => {
            this.snackBar.open('Template activated successfully', 'Close', { duration: 3000 });
            this.loadTemplates();
          },
          error: (err) => {
            this.snackBar.open('Failed to activate template. Please try again.', 'Close', { duration: 5000 });
            console.error('Error activating template:', err);
          }
        });
      }
    });
  }

  /**
   * Deactivate a template
   */
  deactivateTemplate(template: AppraisalTemplate): void {
    const message = `Are you sure you want to deactivate template version ${template.version}?`;
    
    this.dialogService.confirmAction(message, 'Deactivate Template').subscribe(confirmed => {
      if (confirmed) {
        this.templateService.deactivateTemplate(template.id).subscribe({
          next: (response) => {
            this.snackBar.open('Template deactivated successfully', 'Close', { duration: 3000 });
            this.loadTemplates();
          },
          error: (err) => {
            this.snackBar.open('Failed to deactivate template. Please try again.', 'Close', { duration: 5000 });
            console.error('Error deactivating template:', err);
          }
        });
      }
    });
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
