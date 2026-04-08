import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    @if (isLoading) {
      <div class="loading-overlay">
        <mat-spinner [diameter]="diameter"></mat-spinner>
        @if (message) {
          <p class="loading-message">{{ message }}</p>
        }
      </div>
    }
  `,
  styles: [`
    .loading-overlay {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }
    .loading-message {
      margin-top: 16px;
      color: rgba(0, 0, 0, 0.54);
    }
  `]
})
export class LoadingComponent {
  @Input() isLoading = false;
  @Input() message?: string;
  @Input() diameter = 50;
}
