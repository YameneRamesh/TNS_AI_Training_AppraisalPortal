export interface NotificationTemplate {
  id: number;
  triggerEvent: string;
  subject: string;
  bodyHtml: string;
  updatedBy?: number;
  updatedAt: string;
}

export interface EmailNotificationLog {
  id: number;
  formId?: number;
  cycleId?: number;
  recipientEmail: string;
  subject: string;
  triggerEvent: string;
  status: EmailStatus;
  errorReason?: string;
  sentAt?: string;
  createdAt: string;
}

export type EmailStatus = 'PENDING' | 'SENT' | 'FAILED';
