import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EmployeeDashboard, ManagerDashboard, HRDashboard } from '../models/dashboard.model';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

/**
 * Service for fetching role-specific dashboard data.
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly API_URL = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  /**
   * Get employee dashboard data
   */
  getEmployeeDashboard(): Observable<ApiResponse<EmployeeDashboard>> {
    return this.http.get<ApiResponse<EmployeeDashboard>>(`${this.API_URL}/employee`);
  }

  /**
   * Get manager dashboard data
   */
  getManagerDashboard(): Observable<ApiResponse<ManagerDashboard>> {
    return this.http.get<ApiResponse<ManagerDashboard>>(`${this.API_URL}/manager`);
  }

  /**
   * Get HR dashboard data
   */
  getHRDashboard(): Observable<ApiResponse<HRDashboard>> {
    return this.http.get<ApiResponse<HRDashboard>>(`${this.API_URL}/hr`);
  }
}
