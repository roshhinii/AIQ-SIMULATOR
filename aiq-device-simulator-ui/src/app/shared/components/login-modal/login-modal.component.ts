import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/multi-auth.service';
import { AiqErrorComponent, AiqError } from '../../../aiq-error/aiq-error.component';
import { EnvironmentConfigService } from '../../../config/environment.config';

interface LoginFormData {
  username: string;
  password: string;
  isLoggingIn: boolean;
  error?: string;
  aiqError?: AiqError;
}

@Component({
  selector: 'app-login-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, AiqErrorComponent],
  template: `
    <div class="modal-overlay" *ngIf="isVisible" (click)="onOverlayClick()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Login</h3>
          <button class="modal-close-btn" (click)="close()" type="button">&times;</button>
        </div>
        
        <form (ngSubmit)="login()" #loginForm="ngForm" class="modal-form">
          <div class="form-group">
            <label for="modalEnvironment">Environment</label>
            <div class="environment-field">
              <span class="environment-badge" [style.background-color]="getEnvironmentBackgroundColor(environment)" [style.color]="getEnvironmentTextColor(environment)">
                {{ getEnvironmentLabel(environment) }}
              </span>
            </div>
          </div>
          
          <div class="form-group">
            <label for="modalUsername">Username</label>
            <input 
              type="text" 
              id="modalUsername"
              name="username"
              [(ngModel)]="loginData.username"
              class="form-control"
              placeholder="Enter your username"
              [disabled]="loginData.isLoggingIn"
              required
              autocomplete="username">
          </div>

          <div class="form-group">
            <label for="modalPassword">Password</label>
            <input 
              type="password" 
              id="modalPassword"
              name="password"
              [(ngModel)]="loginData.password"
              class="form-control"
              placeholder="Enter your password"
              [disabled]="loginData.isLoggingIn"
              required
              autocomplete="current-password">
          </div>

          <app-aiq-error [error]="loginData.aiqError || null"></app-aiq-error>

          <div class="error-message" *ngIf="loginData.error && !loginData.aiqError">
            {{ loginData.error }}
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" (click)="close()" [disabled]="loginData.isLoggingIn">
              Cancel
            </button>
            <button 
              type="submit" 
              class="btn btn-primary"
              [disabled]="loginForm.invalid || loginData.isLoggingIn">
              {{ loginData.isLoggingIn ? 'Logging in...' : 'Login' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styleUrls: ['./login-modal.component.scss']
})
export class LoginModalComponent {
  @Input() isVisible = false;
  @Input() environment = '';
  @Output() loginSuccess = new EventEmitter<void>();
  @Output() modalClosed = new EventEmitter<void>();

  loginData: LoginFormData = {
    username: '',
    password: '',
    isLoggingIn: false
  };

  constructor(private authService: AuthService) {}

  onOverlayClick(): void {
    if (!this.loginData.isLoggingIn) {
      this.close();
    }
  }

  close(): void {
    if (!this.loginData.isLoggingIn) {
      this.resetForm();
      this.modalClosed.emit();
    }
  }

  login(): void {
    if (!this.loginData.username || !this.loginData.password || this.loginData.isLoggingIn) {
      return;
    }

    this.loginData.isLoggingIn = true;
    this.loginData.error = undefined;
    this.loginData.aiqError = undefined;
    
    this.authService.login(this.loginData.username, this.loginData.password, this.environment).subscribe({
      next: (response) => {
        console.log(`Login successful for ${this.environment}:`, response);
        this.resetForm();
        this.loginSuccess.emit();
      },
      error: (error) => {
        console.error(`Login failed for ${this.environment}:`, error);
        this.loginData.isLoggingIn = false;
        
        // Parse AIQ backend error if available
        if (this.isAiqBackendError(error)) {
          this.loginData.aiqError = {
            code: error.error?.code || error.status?.toString(),
            message: error.error?.message || error.message,
            traceId: error.error?.traceId || error.error?.trace_id
          };
        } else {
          // Fallback to generic error message
          this.loginData.error = error.message || 'Login failed. Please try again.';
        }
      }
    });
  }

  private isAiqBackendError(error: any): boolean {
    // Check if this looks like an AIQ backend error response
    return error && (
      (error.error && (error.error.code || error.error.message || error.error.traceId || error.error.trace_id)) ||
      (error.status && error.status >= 400)
    );
  }

  getEnvironmentLabel(env: string): string {
    return EnvironmentConfigService.getDisplayName(env);
  }

  getEnvironmentBackgroundColor(env: string): string {
    const colors = EnvironmentConfigService.getColors(env);
    return colors.background;
  }

  getEnvironmentTextColor(env: string): string {
    const colors = EnvironmentConfigService.getColors(env);
    return colors.primary;
  }

  private resetForm(): void {
    this.loginData = {
      username: '',
      password: '',
      isLoggingIn: false
    };
  }
}
