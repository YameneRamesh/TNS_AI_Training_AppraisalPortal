import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLog, AuditLogSearchParams } from '../models/audit.model';
import { PageResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly API_URL = `${environment.apiUrl}/audit-logs`;

  constructor(private http: HttpClient) {}

  /**
   * Get paginated audit logs with optional filters
   */
  getAuditLogs(params: AuditLogSearchParams = {}): Observable<PageResponse<AuditLog>> {
    let httpParams = new HttpParams();
    
    if (params.userId !== undefined) {
      httpParams = httpParams.set('userId', params.userId.toString());
    }
    if (params.action) {
      httpParams = httpParams.set('action', params.action);
    }
    if (params.startDate) {
      httpParams = httpParams.set('startDate', params.startDate);
    }
    if (params.endDate) {
      httpParams = httpParams.set('endDate', params.endDate);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<PageResponse<AuditLog>>(this.API_URL, { params: httpParams });
  }
}
