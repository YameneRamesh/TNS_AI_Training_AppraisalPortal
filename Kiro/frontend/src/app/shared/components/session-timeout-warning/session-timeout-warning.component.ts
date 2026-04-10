import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Component that displays a warning when the user's session is about to expire.
 * Shows a snackbar notification 2 minutes before session timeout.
 */
@Component({
  selector: 'app-session-timeout-warning',
  standalone: true,
  imports: [CommonModule, MatSnackBarModule, MatButtonModule],
  template: ``,
  styles: []
})
export class SessionTimeoutWarningComponent implements OnInit, OnDestroy {
  private warningSubscription?: Subscription;
  private snackBarRef: any;

  constructor(
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Subscribe to session warning events
    this.warningSubscription = this.authService.sessionWarning$.subscribe(showWarning => {
      if (showWarning) {
        this.showWarning();
      } else {
        this.dismissWarning();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.warningSubscription) {
      this.warningSubscription.unsubscribe();
    }
    this.dismissWarning();
  }

  private showWarning(): void {
    // Dismiss any existing warning
    this.dismissWarning();

    // Show warning snackbar
    this.snackBarRef = this.snackBar.open(
      'Your session will expire soon due to inactivity. Please interact with the page to stay logged in.',
      'Stay Active',
      {
        duration: 0, // Don't auto-dismiss
        horizontalPosition: 'center',
        verticalPosition: 'top',
        panelClass: ['session-warning-snackbar']
      }
    );

    // Dismiss warning when user clicks the action button
    this.snackBarRef.onAction().subscribe(() => {
      this.authService.refreshSession();
      this.dismissWarning();
    });
  }

  private dismissWarning(): void {
    if (this.snackBarRef) {
      this.snackBarRef.dismiss();
      this.snackBarRef = null;
    }
  }
}
