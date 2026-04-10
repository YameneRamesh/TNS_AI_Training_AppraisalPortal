import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
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
   * Show a confirmation dialog.
   * Accepts either positional args (title, message, confirmText, cancelText)
   * or a single ConfirmDialogData object for backward compatibility.
   */
  confirm(
    title: string,
    message: string,
    confirmText = 'Confirm',
    cancelText = 'Cancel'
  ): Promise<boolean> {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: { title, message, confirmText, cancelText } as ConfirmDialogData
    });

    return dialogRef.afterClosed().toPromise().then(result => result === true);
  }

  /**
   * Show a simple confirmation with default text
   */
  confirmAction(message: string, title = 'Confirm Action'): Promise<boolean> {
    return this.confirm(title, message, 'Confirm', 'Cancel');
  }
}
