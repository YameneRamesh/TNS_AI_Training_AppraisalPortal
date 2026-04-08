export interface AuditLog {
  id: number;
  userId?: number;
  userName?: string;
  action: string;
  entityType?: string;
  entityId?: number;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface AuditLogSearchParams {
  userId?: number;
  action?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}
