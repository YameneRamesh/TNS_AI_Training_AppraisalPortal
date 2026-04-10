import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-rating-key-section',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: `
    <div class="section-container rating-key-section">
      <h2 class="section-title">Rating Key</h2>
      
      <div class="rating-cards">
        <mat-card *ngFor="let rating of ratingLevels" class="rating-card">
          <mat-card-header>
            <mat-card-title>{{ rating.level }}</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>{{ rating.description }}</p>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .rating-key-section {
      margin-bottom: 24px;
    }

    .section-title {
      font-size: 20px;
      font-weight: 500;
      margin-bottom: 16px;
      color: #333;
    }

    .rating-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 16px;
    }

    .rating-card {
      background-color: #f5f5f5;
    }

    mat-card-title {
      font-weight: 600;
      color: #1976d2;
    }

    mat-card-content p {
      margin: 0;
      font-size: 14px;
      color: #666;
    }
  `]
})
export class RatingKeySectionComponent {
  ratingLevels = [
    {
      level: 'Excels',
      description: 'Consistently exceeds expectations and demonstrates exceptional performance'
    },
    {
      level: 'Exceeds',
      description: 'Frequently exceeds expectations and shows strong performance'
    },
    {
      level: 'Meets',
      description: 'Consistently meets expectations and performs at expected level'
    },
    {
      level: 'Developing',
      description: 'Working toward meeting expectations, requires development'
    }
  ];
}
