import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

interface LoginFormData {
  environment: string;
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      
      <div class="login-content">
        <div class="login-card">
        <button class="skip-button" (click)="skipLogin()" type="button">Skip</button>
          <h2 class="login-title">AIQ Device Simulator</h2>
          
          <form (ngSubmit)="login()" #loginForm="ngForm" class="login-form">
            <div class="form-group">
              <label for="environment">Select Environment:</label>
              <select 
                id="environment"
                name="environment"
                [(ngModel)]="loginData.environment"
                class="form-control"
                required>
                <option value="">Select Environment</option>
                <option value="development">Development</option>
                <option value="test">Test</option>
                <option value="production">Production</option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="email">Email:</label>
              <input 
                type="email" 
                id="email"
                name="email"
                [(ngModel)]="loginData.email"
                class="form-control"
                placeholder="Email address"
                required>
            </div>

            <div class="form-group">
              <label for="password">Password:</label>
              <input 
                type="password" 
                id="password"
                name="password"
                [(ngModel)]="loginData.password"
                class="form-control"
                placeholder="Password"
                required>
            </div>

            <button 
              type="submit" 
              class="login-button"
              [disabled]="loginForm.invalid">
              Login
            </button>
          </form>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginData: LoginFormData = {
    environment: '',
    email: '',
    password: ''
  };

  constructor(private router: Router) {}

  login(): void {
    if (this.loginData.environment && this.loginData.email && this.loginData.password) {
      // Since there's no backend, just navigate to dashboard
      this.router.navigate(['/dashboard']);
    }
  }

  skipLogin(): void {
    this.router.navigate(['/dashboard']);
  }
}
