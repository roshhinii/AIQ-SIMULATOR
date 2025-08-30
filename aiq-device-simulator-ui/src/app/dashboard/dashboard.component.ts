import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService, AuthState, UserInfo } from '../shared/services/multi-auth.service';
import { SimulatorService, Device } from '../shared/services/simulator.service';
import { EnvironmentConfigService } from '../config/environment.config';
import { Subscription } from 'rxjs';

interface UserSession {
  username: string;
  environment: string;
  loginTime: string;
  token?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-container">
      <main class="dashboard-main">
        <div class="devices-section">
          <div class="section-header">
            <h3>Simulators</h3>
            <div class="section-actions">
              <span class="device-count">{{ devices.length }} simulator{{ devices.length !== 1 ? 's' : '' }}</span>
              <button class="btn btn-refresh" (click)="refreshDevices()" [disabled]="isLoading">
                {{ isLoading ? 'Loading...' : 'Refresh' }}
              </button>
            </div>
          </div>
          
          <div class="devices-grid" *ngIf="!isLoading">
            <!-- Add New Simulator Card -->
            <div class="device-card add-simulator-card" (click)="navigateToAddSimulator()">
              <div class="add-simulator-content">
                <div class="add-icon">+</div>
                <h4>Add New Simulator</h4>
              </div>
            </div>
            
            <!-- Existing Device Cards -->
            <div class="device-card" *ngFor="let device of devices" [class]="'status-' + device.status">
              <div class="device-header">
                <div class="device-type">
                  <span class="device-name">{{ device.type }}</span>
                </div>
              </div>
              
              <div class="device-details">
                <div class="detail-row">
                  <span class="label">Device ID:</span>
                  <span class="value mac-address">{{ device.id }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Environment:</span>
                  <span class="value environment" [style.background-color]="getEnvironmentBackgroundColor(device.environment)" [style.color]="getEnvironmentTextColor(device.environment)">
                    {{ getEnvironmentLabel(device.environment) }}
                  </span>
                </div>
                <div class="detail-row">
                  <span class="label">Status:</span>
                  <span class="value device-status" [class]="'status-' + device.status">
                    {{ getStatusLabel(device.status) }}
                  </span>
                </div>
              </div>
              
              <div class="device-actions">
                <button 
                  class="btn action-btn" 
                  [class]="isDeviceRunning(device.status) ? 'btn-danger' : 'btn-success'"
                  (click)="toggleDeviceState(device)">
                  {{ isDeviceRunning(device.status) ? 'Stop' : 'Start' }}
                </button>
                <button class="btn btn-secondary" (click)="viewDevice(device)">
                  Details
                </button>
              </div>
            </div>
          </div>

          <div class="loading-state" *ngIf="isLoading">
            <div class="loading-spinner"></div>
            <p>Loading devices...</p>
          </div>
        </div>
      </main>
      
      <footer class="dashboard-footer">
        <p>&copy; 2025 AIQ Team. Built with Angular and Electron.</p>
      </footer>
    </div>
  `,
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  userSession: UserSession | null = null;
  authState: AuthState | null = null;
  devices: Device[] = [];
  
  private authSubscription?: Subscription;
  private devicesSubscription?: Subscription;
  private loginTime = new Date();
  isLoading = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private simulatorService: SimulatorService
  ) {}

  ngOnInit(): void {
    this.loadAuthState();
    this.loadDevices();
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
    if (this.devicesSubscription) {
      this.devicesSubscription.unsubscribe();
    }
  }

  private loadAuthState(): void {
    this.authSubscription = this.authService.authState$.subscribe(state => {
      this.authState = state;
      if (state.isAuthenticated) {
        this.userSession = {
          username: state.user?.email || 'Unknown',
          environment: state.environment,
          loginTime: this.loginTime.toISOString(),
          token: state.token || undefined
        };
      }
    });
  }

  private loadDevices(): void {
    this.isLoading = true;
    
    // Subscribe to devices observable for real-time updates
    this.devicesSubscription = this.simulatorService.devices$.subscribe(devices => {
      this.devices = devices;
      this.isLoading = false;
    });

    // Initial load of devices
    this.simulatorService.refreshDevices();
  }

  refreshDevices(): void {
    this.simulatorService.refreshDevices();
  }

  toggleDeviceState(device: Device): void {
    if (this.isDeviceRunning(device.status)) {
      this.simulatorService.stopDevice(device.id).subscribe({
        next: (success) => {
          if (success) {
            console.log(`Successfully stopped device: ${device.id}`);
          } else {
            console.error(`Failed to stop device: ${device.id} - Backend unavailable`);
          }
        },
        error: (error) => {
          console.error(`Failed to stop device: ${device.id}`, error);
        }
      });
    } else {
      this.simulatorService.startDevice(device.id).subscribe({
        next: (success) => {
          if (success) {
            console.log(`Successfully started device: ${device.id}`);
          } else {
            console.error(`Failed to start device: ${device.id} - Backend unavailable`);
          }
        },
        error: (error) => {
          console.error(`Failed to start device: ${device.id}`, error);
        }
      });
    }
  }

  viewDevice(device: Device): void {
    console.log(`Viewing device details: ${device.id}`);
    // In a real app, this would navigate to a device detail page or open a modal
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

  getStatusLabel(status: string): string {
    switch (status.toLowerCase()) {
      case 'stopped': return 'Stopped';
      case 'starting': return 'Starting';
      case 'connecting': return 'Connecting';
      case 'connected': return 'Connected';
      default: return 'Unknown';
    }
  }

  isDeviceRunning(status: string): boolean {
    return status === 'connected' || status === 'connecting' || status === 'starting';
  }

  navigateToAddSimulator(): void {
    this.router.navigate(['/add-simulator']);
  }
}
