import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TemplateService } from '../../../core/services/template.service';
import { AppraisalTemplate } from '../../../core/models/appraisal.model';

/**
 * Component for viewing appraisal template details with JSON display.
 * Accessible by HR and Admin roles.
 */
@Component({
  selector: 'app-template-viewer',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTabsModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './template-viewer.component.html',
  styleUrl: './template-viewer.component.scss'
})
export class TemplateViewerComponent implements OnInit {
  template: AppraisalTemplate | null = null;
  loading = false;
  error: string | null = null;
  formattedJson = '';
  parsedSchema: any = null;

  constructor(
    private templateService: TemplateService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const templateId = this.route.snapshot.paramMap.get('id');
    if (templateId) {
      this.loadTemplate(+templateId);
    } else {
      this.error = 'Invalid template ID';
    }
  }

  /**
   * Load template by ID
   */
  loadTemplate(id: number): void {
    this.loading = true;
    this.error = null;

    this.templateService.getTemplateById(id).subscribe({
      next: (response) => {
        this.template = response.data ?? null;
        if (this.template) {
          this.parseJsonSchema();
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load template. Please try again.';
        this.loading = false;
        console.error('Error loading template:', err);
      }
    });
  }

  /**
   * Parse and format JSON schema
   */
  parseJsonSchema(): void {
    if (!this.template?.schemaJson) {
      return;
    }

    try {
      this.parsedSchema = JSON.parse(this.template.schemaJson);
      this.formattedJson = JSON.stringify(this.parsedSchema, null, 2);
    } catch (e) {
      console.error('Error parsing JSON schema:', e);
      this.formattedJson = this.template.schemaJson;
      this.parsedSchema = null;
    }
  }

  /**
   * Navigate back to template list
   */
  goBack(): void {
    this.router.navigate(['/hr/templates']);
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Copy JSON to clipboard
   */
  copyToClipboard(): void {
    if (this.formattedJson) {
      navigator.clipboard.writeText(this.formattedJson).then(() => {
        this.snackBar.open('JSON copied to clipboard', 'Close', { duration: 2000 });
      }).catch(err => {
        console.error('Failed to copy JSON:', err);
        this.snackBar.open('Failed to copy to clipboard', 'Close', { duration: 3000 });
      });
    }
  }
}
