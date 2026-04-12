import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, Role } from '../models/user.model';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

export interface UserSearchParams {
  search?: string;
  department?: string;
  role?: string;
  isActive?: boolean;
  status?: string;
  page?: number;
  size?: number;
}

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
  password?: string;
  designation?: string;
  department?: string;
  managerId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly API_URL = `${environment.apiUrl}/users`;
  private readonly ROLES_URL = `${environment.apiUrl}/roles`;

  constructor(private http: HttpClient) {}

  getUsers(params?: UserSearchParams): Observable<ApiResponse<PageResponse<User>>> {
    let httpParams = new HttpParams();

    if (params?.search) {
      httpParams = httpParams.set('searchTerm', params.search);
    }
    if (params?.department) {
      httpParams = httpParams.set('department', params.department);
    }
    if (params?.role) {
      httpParams = httpParams.set('role', params.role);
    }
    if (params?.isActive !== undefined) {
      httpParams = httpParams.set('isActive', params.isActive.toString());
    }
    if (params?.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params?.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<ApiResponse<PageResponse<User>>>(this.API_URL, { params: httpParams });
  }

  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API_URL}/${id}`);
  }

  createUser(request: UserCreateRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(this.API_URL, request);
  }

  updateUser(id: number, request: UserUpdateRequest): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.API_URL}/${id}`, request);
  }

  deactivateUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }

  reactivateUser(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.API_URL}/${id}/reactivate`, {});
  }

  assignRoles(userId: number, roleNames: string[]): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.API_URL}/${userId}/roles`, { roles: roleNames });
  }

  getRoles(): Observable<ApiResponse<Role[]>> {
    return this.http.get<ApiResponse<Role[]>>(this.ROLES_URL);
  }
}
