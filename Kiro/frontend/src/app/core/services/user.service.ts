import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { User } from '../models/user.model';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

export interface UserCreateRequest {
  employeeId: string;
  fullName: string;
  email: string;
  password: string;
  designation?: string;
  department?: string;
  managerId?: number;
  roles: string[];
}

export interface UserUpdateRequest {
  fullName?: string;
  email?: string;
  designation?: string;
  department?: string;
  managerId?: number;
}

export interface UserSearchParams {
  search?: string;
  role?: string;
  status?: string;
  page?: number;
  size?: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly API_URL = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  /**
   * Get paginated list of users with optional filters
   */
  getUsers(params: UserSearchParams = {}): Observable<PageResponse<User>> {
    let httpParams = new HttpParams();
    
    if (params.search) {
      httpParams = httpParams.set('searchTerm', params.search);
    }
    if (params.role) {
      httpParams = httpParams.set('role', params.role);
    }
    if (params.status !== undefined && params.status !== '') {
      httpParams = httpParams.set('isActive', params.status === 'active' ? 'true' : 'false');
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<ApiResponse<PageResponse<User>>>(this.API_URL, { params: httpParams }).pipe(
      map(response => response.data ?? { content: [], totalElements: 0, pageNumber: 0, pageSize: 10, totalPages: 0, last: true })
    );
  }

  /**
   * Get user by ID
   */
  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API_URL}/${id}`);
  }

  /**
   * Create new user
   */
  createUser(user: UserCreateRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(this.API_URL, user);
  }

  /**
   * Update existing user
   */
  updateUser(id: number, user: UserUpdateRequest): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.API_URL}/${id}`, user);
  }

  /**
   * Deactivate user
   */
  deactivateUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }

  /**
   * Reactivate user
   */
  reactivateUser(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.API_URL}/${id}/reactivate`, {});
  }

  /**
   * Assign roles to user — sends role names (strings) as backend expects
   */
  assignRoles(userId: number, roleNames: string[]): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.API_URL}/${userId}/roles`, { roles: roleNames });
  }

  /**
   * Get all available roles
   */
  getRoles(): Observable<ApiResponse<{ id: number; name: string }[]>> {
    return this.http.get<ApiResponse<{ id: number; name: string }[]>>(`${environment.apiUrl}/roles`);
  }
}
