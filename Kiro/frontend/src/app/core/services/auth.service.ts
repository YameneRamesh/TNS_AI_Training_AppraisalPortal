import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { User, LoginRequest, LoginResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

/**
 * Authentication service for managing user sessions.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  public isAuthenticated$ = this.currentUser$.pipe(
    tap(user => console.log('Auth state:', user ? 'authenticated' : 'not authenticated'))
  );

  constructor(private http: HttpClient) {
    // this.checkAuthStatus(); // Disabled for development
  }

  /**
   * Check if user is currently authenticated by calling /api/auth/me
   */
  private checkAuthStatus(): void {
    this.http.get<User>(`${this.API_URL}/me`).subscribe({
      next: (user) => this.currentUserSubject.next(user),
      error: () => this.currentUserSubject.next(null)
    });
  }

  /**
   * Login with email and password
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => this.currentUserSubject.next(response.user))
    );
  }

  /**
   * Logout current user
   */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/logout`, {}).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  /**
   * Get current user value (synchronous)
   */
  get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user has a specific role
   */
  hasRole(roleName: string): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.some(r => r.name === roleName) : false;
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roleNames: string[]): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.some(r => roleNames.includes(r.name)) : false;
  }
}
