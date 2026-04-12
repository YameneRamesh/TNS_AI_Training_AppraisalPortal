import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppraisalCycle, AppraisalForm } from '../models/appraisal.model';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

export interface TriggerCycleRequest {
  employeeIds: number[];
}

export interface TriggerCycleResult {
  totalEmployees: number;
  successCount: number;
  failureCount: number;
  failures: Array<{
    employeeId: number;
    errorReason: string;
  }>;
}

/**
 * Service for managing appraisal cycles.
 */
@Injectable({
  providedIn: 'root'
})
export class CycleService {
  private readonly API_URL = `${environment.apiUrl}/cycles`;

  constructor(private http: HttpClient) {}

  /**
   * Get all appraisal cycles
   */
  getCycles(): Observable<ApiResponse<AppraisalCycle[]>> {
    return this.http.get<ApiResponse<AppraisalCycle[]>>(this.API_URL);
  }

  /**
   * Get cycle by ID
   */
  getCycleById(id: number): Observable<ApiResponse<AppraisalCycle>> {
    return this.http.get<ApiResponse<AppraisalCycle>>(`${this.API_URL}/${id}`);
  }

  /**
   * Create new cycle
   */
  createCycle(cycle: Partial<AppraisalCycle>): Observable<ApiResponse<AppraisalCycle>> {
    return this.http.post<ApiResponse<AppraisalCycle>>(this.API_URL, cycle);
  }

  /**
   * Update existing cycle
   */
  updateCycle(id: number, cycle: Partial<AppraisalCycle>): Observable<ApiResponse<AppraisalCycle>> {
    return this.http.put<ApiResponse<AppraisalCycle>>(`${this.API_URL}/${id}`, cycle);
  }

  /**
   * Trigger cycle for selected employees
   */
  triggerCycle(cycleId: number, request: TriggerCycleRequest): Observable<ApiResponse<TriggerCycleResult>> {
    return this.http.post<ApiResponse<TriggerCycleResult>>(`${this.API_URL}/${cycleId}/trigger`, request);
  }

  /**
   * Delete a cycle (DRAFT only)
   */
  deleteCycle(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }

  /**
   * Get all forms for a cycle
   */
  getCycleForms(cycleId: number): Observable<ApiResponse<AppraisalForm[]>> {
    return this.http.get<ApiResponse<AppraisalForm[]>>(`${this.API_URL}/${cycleId}/forms`);
  }

  /**
   * Reopen a submitted or completed form
   */
  reopenForm(cycleId: number, formId: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/${cycleId}/reopen/${formId}`, {});
  }

  /**
   * Assign backup reviewer
   */
  assignBackupReviewer(cycleId: number, formId: number, backupReviewerId: number): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.API_URL}/${cycleId}/backup-reviewer`, {
      formId,
      backupReviewerId
    });
  }
}
