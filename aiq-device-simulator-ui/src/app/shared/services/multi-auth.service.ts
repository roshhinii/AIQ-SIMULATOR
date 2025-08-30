import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { EnvironmentConfigService } from '../../config/environment.config';

export interface LoginRequest {
  email: string;
  password: string;
  rememberme: boolean;
}

export interface LoginResponse {
  token: string;
  expiryAt: string;
  refreshToken: string;
  refreshExpiryAt: string;
}

export interface LoginErrorResponse {
  message: string;
  code: number;
  traceId: string;
}

export interface UserInfo {
  fname: string;
  lname: string;
  orgName: string;
  email?: string;
}

export interface UserResponse {
  fname: string;
  lname: string;
  orgName: string;
  [key: string]: any;
}

export interface EnvironmentSession {
  token: string;
  user: UserInfo;
  environment: string;
  loginTime: Date;
  expiryAt: string;
}

export interface MultiAuthState {
  sessions: Map<string, EnvironmentSession>;
}

// Legacy interface for backward compatibility
export interface AuthState {
  isAuthenticated: boolean;
  token: string | null;
  user: UserInfo | null;
  environment: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly SESSIONS_KEY = 'aiq_environment_sessions';

  private multiAuthStateSubject = new BehaviorSubject<MultiAuthState>({
    sessions: new Map<string, EnvironmentSession>()
  });

  public multiAuthState$ = this.multiAuthStateSubject.asObservable();

  // Legacy compatibility - provides current active session state
  private authStateSubject = new BehaviorSubject<AuthState>({
    isAuthenticated: false,
    token: null,
    user: null,
    environment: ''
  });

  public authState$ = this.authStateSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadAuthState();
  }

  // Get all environment sessions
  getSessions(): Map<string, EnvironmentSession> {
    return this.multiAuthStateSubject.value.sessions;
  }

  // Get session for specific environment
  getSessionForEnvironment(environment: string): EnvironmentSession | null {
    return this.multiAuthStateSubject.value.sessions.get(environment) || null;
  }

  // Check if user is logged in to specific environment
  isLoggedInToEnvironment(environment: string): boolean {
    return this.multiAuthStateSubject.value.sessions.has(environment);
  }

  // Get all logged in environments
  getLoggedInEnvironments(): string[] {
    return Array.from(this.multiAuthStateSubject.value.sessions.keys());
  }

  // Login to specific environment
  login(email: string, password: string, environment: string): Observable<LoginResponse> {
    const apiUrl = this.getApiUrl(environment);
    
    if (!apiUrl) {
      return throwError(() => new Error('Invalid environment selected'));
    }

    const loginRequest: LoginRequest = {
      email,
      password,
      rememberme: false
    };

    const headers = new HttpHeaders({
      'Accept': 'application/json, text/plain, */*',
      'Content-Type': 'application/json'
    });

    return this.http.post<LoginResponse>(apiUrl, loginRequest, { headers }).pipe(
      switchMap(response => {
        if (response && response.token) {
          return this.fetchUserInfo(response.token, environment).pipe(
            map(user => {
              this.storeEnvironmentSession(response.token, user, environment, response.expiryAt);
              return response;
            })
          );
        }
        return of(response);
      }),
      catchError(error => {
        console.error('Login error:', error);
        return throwError(() => error);
      })
    );
  }

  // Logout from specific environment
  logoutFromEnvironment(environment: string): void {
    const currentState = this.multiAuthStateSubject.value;
    const newSessions = new Map(currentState.sessions);
    newSessions.delete(environment);

    const updatedState = {
      sessions: newSessions
    };

    this.multiAuthStateSubject.next(updatedState);
    this.saveAuthState();
    this.updateLegacyAuthState();
  }

  // Logout from all environments
  logoutAll(): void {
    const emptyState = {
      sessions: new Map<string, EnvironmentSession>()
    };
    
    this.multiAuthStateSubject.next(emptyState);
    this.clearAuthState();
    this.updateLegacyAuthState();
  }

  // Legacy methods for backward compatibility
  isAuthenticated(): boolean {
    return this.multiAuthStateSubject.value.sessions.size > 0;
  }

  getTokenForEnvironment(environment: string): string | null {
    const session = this.multiAuthStateSubject.value.sessions.get(environment);
    return session ? session.token : null;
  }

  getUserInfoForEnvironment(environment: string): UserInfo | null {
    const session = this.multiAuthStateSubject.value.sessions.get(environment);
    return session ? session.user : null;
  }

  // Private helper methods
  private fetchUserInfo(token: string, environment: string): Observable<UserInfo> {
    const userApiUrl = this.getUserApiUrl(environment);
    
    if (!userApiUrl) {
      return throwError(() => new Error('Invalid environment for user API'));
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    });

    return this.http.get<UserResponse>(userApiUrl, { headers }).pipe(
      map(response => ({
        fname: response.fname,
        lname: response.lname,
        orgName: response.orgName,
        email: response['email']
      })),
      catchError(error => {
        console.error('Error fetching user info:', error);
        return throwError(() => error);
      })
    );
  }

  private storeEnvironmentSession(token: string, user: UserInfo, environment: string, expiryAt: string): void {
    const currentState = this.multiAuthStateSubject.value;
    const newSessions = new Map(currentState.sessions);
    
    const session: EnvironmentSession = {
      token,
      user,
      environment,
      loginTime: new Date(),
      expiryAt
    };

    newSessions.set(environment, session);

    const updatedState = {
      sessions: newSessions
    };

    this.multiAuthStateSubject.next(updatedState);
    this.saveAuthState();
    this.updateLegacyAuthState();
  }

  private updateLegacyAuthState(): void {
    // For legacy compatibility, use the first available session or none
    const sessions = this.multiAuthStateSubject.value.sessions;
    const firstSession = sessions.size > 0 ? Array.from(sessions.values())[0] : null;

    const legacyState: AuthState = {
      isAuthenticated: !!firstSession,
      token: firstSession ? firstSession.token : null,
      user: firstSession ? firstSession.user : null,
      environment: firstSession ? firstSession.environment : ''
    };

    this.authStateSubject.next(legacyState);
  }

  private loadAuthState(): void {
    try {
      const sessionsData = localStorage.getItem(this.SESSIONS_KEY);

      if (sessionsData) {
        const sessionsArray = JSON.parse(sessionsData);
        const sessions = new Map<string, EnvironmentSession>();
        
        sessionsArray.forEach((sessionData: any) => {
          sessions.set(sessionData.environment, {
            ...sessionData,
            loginTime: new Date(sessionData.loginTime)
          });
        });

        this.multiAuthStateSubject.next({
          sessions
        });
      }
    } catch (error) {
      console.error('Error loading auth state:', error);
      this.clearAuthState();
    }
    
    this.updateLegacyAuthState();
  }

  private saveAuthState(): void {
    try {
      const sessions = this.multiAuthStateSubject.value.sessions;
      const sessionsArray = Array.from(sessions.values());
      
      localStorage.setItem(this.SESSIONS_KEY, JSON.stringify(sessionsArray));
    } catch (error) {
      console.error('Error saving auth state:', error);
    }
  }

  private clearAuthState(): void {
    localStorage.removeItem(this.SESSIONS_KEY);
  }

  private getApiUrl(environment: string): string | null {
    const backendUrl = EnvironmentConfigService.getBackendUrl(environment);
    return backendUrl ? `${backendUrl}/auth-service/api/auth/login` : null;
  }

  private getUserApiUrl(environment: string): string | null {
    const backendUrl = EnvironmentConfigService.getBackendUrl(environment);
    return backendUrl ? `${backendUrl}/auth-service/api/user` : null;
  }

  // Utility methods for error handling
  handleLoginError(error: any): LoginErrorResponse {
    if (error.error && typeof error.error === 'object') {
      return {
        message: error.error.message || 'Login failed. Please check your credentials.',
        code: error.error.code || error.status || 0,
        traceId: error.error.traceId || 'N/A'
      };
    } else if (error.message) {
      return {
        message: error.message,
        code: error.status || 0,
        traceId: 'N/A'
      };
    } else {
      return {
        message: 'An unexpected error occurred. Please try again.',
        code: 0,
        traceId: 'N/A'
      };
    }
  }

  // Environment management
  getAvailableEnvironments(): string[] {
    return ['dev', 'test', 'prod'];
  }

  getEnvironmentDisplayName(env: string): string {
    return EnvironmentConfigService.getDisplayName(env);
  }
}
