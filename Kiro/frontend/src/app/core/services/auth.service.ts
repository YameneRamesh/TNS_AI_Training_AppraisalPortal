import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { LoginRequest, UserProfile } from '../models/user.model';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;

  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private sessionWarningSubject = new BehaviorSubject<boolean>(false);
  public sessionWarning$ = this.sessionWarningSubject.asObservable();

  public isAuthenticated$ = this.currentUser$.pipe(
    map(user => !!user)
  );

  constructor(private http: HttpClient) {
    this.checkAuthStatus();
  }

  private checkAuthStatus(): void {
    this.http.get<ApiResponse<UserProfile>>(`${this.API_URL}/me`).subscribe({
      next: (response) => this.currentUserSubject.next(response.data || null),
      error: () => this.currentUserSubject.next(null)
    });
  }

  resolveSessionUser(): Observable<UserProfile | null> {
    return this.http.get<ApiResponse<UserProfile>>(`${this.API_URL}/me`).pipe(
      map(response => response.data || null),
      tap(user => this.currentUserSubject.next(user)),
      catchError(() => {
        this.currentUserSubject.next(null);
        return of(null);
      })
    );
  }

  login(credentials: LoginRequest): Observable<ApiResponse<UserProfile>> {
    return this.http.post<ApiResponse<UserProfile>>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        if (response.data) {
          this.currentUserSubject.next(response.data);
        }
      })
    );
  }

  logout(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/logout`, {}).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  refreshSession(): void {
    this.http.get<ApiResponse<UserProfile>>(`${this.API_URL}/me`).subscribe({
      next: (response) => {
        this.currentUserSubject.next(response.data || null);
        this.sessionWarningSubject.next(false);
      },
      error: () => this.currentUserSubject.next(null)
    });
  }

  clearUser(): void {
    this.currentUserSubject.next(null);
  }

  get currentUserValue(): UserProfile | null {
    return this.currentUserSubject.value;
  }

  hasRole(roleName: string): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.includes(roleName) : false;
  }

  hasAnyRole(roleNames: string[]): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.some(r => roleNames.includes(r)) : false;
  }
}
