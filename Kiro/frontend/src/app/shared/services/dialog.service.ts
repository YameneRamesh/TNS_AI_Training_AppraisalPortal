import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { ConfirmDialogComponent, ConfirmDialogData } from '../components/confirm-dialog/confirm-dialog.component';

/**
 * Service for showing dialogs throughout the application.
 */
@Injectable({
  providedIn: 'root'
})
export class DialogService {
  private dialog = inject(MatDialog);

  /**
   * Show a confirmation dialog
   */
  confirm(data: ConfirmDialogData): Observable<boolean> {
    const dialogRef = this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      width: '400px',
      data
    });

    return dialogRef.afterClosed() as Observable<boolean>;
  }

  /**
   * Show a simple confirmation with default text
   */
  confirmAction(message: string, title = 'Confirm Action'): Observable<boolean> {
    return this.confirm({
      title,
      message,
      confirmText: 'Confirm',
      cancelText: 'Cancel'
    });
  }
}
