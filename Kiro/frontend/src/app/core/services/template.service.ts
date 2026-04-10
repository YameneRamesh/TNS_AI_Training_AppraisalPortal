import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppraisalTemplate } from '../models/appraisal.model';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

/**
 * Service for managing appraisal templates.
 */
@Injectable({
  providedIn: 'root'
})
export class TemplateService {
  private readonly API_URL = `${environment.apiUrl}/templates`;

  constructor(private http: HttpClient) {}

  /**
   * Get all appraisal templates
   */
  getTemplates(): Observable<ApiResponse<AppraisalTemplate[]>> {
    return this.http.get<ApiResponse<AppraisalTemplate[]>>(this.API_URL);
  }

  /**
   * Get template by ID
   */
  getTemplateById(id: number): Observable<ApiResponse<AppraisalTemplate>> {
    return this.http.get<ApiResponse<AppraisalTemplate>>(`${this.API_URL}/${id}`);
  }

  /**
   * Create new template
   */
  createTemplate(template: Partial<AppraisalTemplate>): Observable<ApiResponse<AppraisalTemplate>> {
    return this.http.post<ApiResponse<AppraisalTemplate>>(this.API_URL, template);
  }

  /**
   * Activate a template (deactivates all other templates)
   */
  activateTemplate(id: number): Observable<ApiResponse<AppraisalTemplate>> {
    return this.http.post<ApiResponse<AppraisalTemplate>>(`${this.API_URL}/${id}/activate`, {});
  }

  /**
   * Deactivate a template
   */
  deactivateTemplate(id: number): Observable<ApiResponse<AppraisalTemplate>> {
    return this.http.post<ApiResponse<AppraisalTemplate>>(`${this.API_URL}/${id}/deactivate`, {});
  }
}
