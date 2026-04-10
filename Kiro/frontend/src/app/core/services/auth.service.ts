import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, interval, Subscription, map } from 'rxjs';
import { User, LoginRequest } from '../models/user.model';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

const USER_SESSION_KEY = 'tns_appraisal_user_session';

/**
 * Authentication service for managing user sessions.
 * Implements 15-minute session timeout with activity tracking and warnings.
 * Stores user in sessionStorage to persist across page refresh but clear on browser close.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly SESSION_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
  private readonly WARNING_BEFORE_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes before timeout

  private currentUserSubject = new BehaviorSubject<User | null>(this.getStoredUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  public isAuthenticated$ = this.currentUser$.pipe(
    map(user => !!user)
  );

  private lastActivityTime: number = Date.now();
  private sessionWarningSubject = new BehaviorSubject<boolean>(false);
  public sessionWarning$ = this.sessionWarningSubject.asObservable();

  private activityCheckSubscription?: Subscription;

  constructor(private http: HttpClient) {
    this.checkAuthStatus();
    this.initializeActivityTracking();
  }

  /**
   * Get stored user from sessionStorage
   */
  private getStoredUser(): User | null {
    try {
      const stored = sessionStorage.getItem(USER_SESSION_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }

  /**
   * Store user in sessionStorage
   */
  private storeUser(user: User | null): void {
    if (user) {
      sessionStorage.setItem(USER_SESSION_KEY, JSON.stringify(user));
    } else {
      sessionStorage.removeItem(USER_SESSION_KEY);
    }
  }

  /**
   * Check if user is currently authenticated by calling /api/auth/me
   */
  private checkAuthStatus(): void {
    this.http.get<ApiResponse<User>>(`${this.API_URL}/me`).subscribe({
      next: (response) => {
        const user = response.data ?? null;
        this.currentUserSubject.next(user);
        this.storeUser(user);
      },
      error: () => {
        this.currentUserSubject.next(null);
        this.storeUser(null);
      }
    });
  }

  login(credentials: LoginRequest): Observable<User> {
    return this.http.post<ApiResponse<User>>(`${this.API_URL}/login`, credentials).pipe(
      map(response => response.data as User),
      tap(user => {
        this.currentUserSubject.next(user);
        this.storeUser(user);
        this.updateActivity();
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/logout`, {}).pipe(
      map(() => void 0),
      tap(() => {
        this.currentUserSubject.next(null);
        this.storeUser(null);
        this.sessionWarningSubject.next(false);
        if (this.activityCheckSubscription) {
          this.activityCheckSubscription.unsubscribe();
        }
      })
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
    return user ? user.roles.includes(roleName) : false;
  }

  /**
   * Check if user has any of the specified roles
   */
  hasAnyRole(roleNames: string[]): boolean {
    const user = this.currentUserValue;
    return user ? user.roles.some(r => roleNames.includes(r)) : false;
  }

  /**
   * Initialize activity tracking for session timeout management.
   */
  private initializeActivityTracking(): void {
    const activityEvents = ['mousedown', 'keydown', 'scroll', 'touchstart', 'click'];
    activityEvents.forEach(event => {
      document.addEventListener(event, () => this.updateActivity(), { passive: true });
    });

    this.activityCheckSubscription = interval(30000).subscribe(() => {
      this.checkSessionTimeout();
    });
  }

  private updateActivity(): void {
    if (this.currentUserValue) {
      this.lastActivityTime = Date.now();
      if (this.sessionWarningSubject.value) {
        this.sessionWarningSubject.next(false);
      }
    }
  }

  private checkSessionTimeout(): void {
    if (!this.currentUserValue) return;

    const timeSinceLastActivity = Date.now() - this.lastActivityTime;
    const timeUntilTimeout = this.SESSION_TIMEOUT_MS - timeSinceLastActivity;

    if (timeUntilTimeout <= this.WARNING_BEFORE_TIMEOUT_MS && timeUntilTimeout > 0) {
      if (!this.sessionWarningSubject.value) {
        this.sessionWarningSubject.next(true);
      }
    }

    if (timeUntilTimeout <= 0) {
      this.handleSessionExpired();
    }
  }

  private handleSessionExpired(): void {
    this.currentUserSubject.next(null);
    this.storeUser(null);
    this.sessionWarningSubject.next(false);
    if (this.activityCheckSubscription) {
      this.activityCheckSubscription.unsubscribe();
    }
  }

  refreshSession(): void {
    this.updateActivity();
  }

  getRemainingSessionTime(): number {
    if (!this.currentUserValue) return 0;
    const timeSinceLastActivity = Date.now() - this.lastActivityTime;
    const timeRemaining = this.SESSION_TIMEOUT_MS - timeSinceLastActivity;
    return Math.max(0, Math.floor(timeRemaining / 1000));
  }

  ngOnDestroy(): void {
    if (this.activityCheckSubscription) {
      this.activityCheckSubscription.unsubscribe();
    }
  }
}
