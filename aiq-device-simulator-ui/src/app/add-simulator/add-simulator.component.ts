import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../shared/services/multi-auth.service';
import { SimulatorService, Device } from '../shared/services/simulator.service';
import { CertService } from '../shared/services/cert.service';
import { ComponentService } from '../shared/services/component.service';
import { LoginModalComponent } from '../shared/components/login-modal/login-modal.component';
import { EnvironmentConfigService } from '../config/environment.config';
import { switchMap } from 'rxjs/operators';

interface NewSimulatorForm {
  deviceId: string;
  type: string;
  environment: string;
  description: string;
}

interface LoginFormData {
  username: string;
  password: string;
  isLoggingIn: boolean;
}

enum StepStatus {
  PENDING = 'pending',
  LOADING = 'loading',
  SUCCESS = 'success',
  ERROR = 'error'
}

interface CreationStep {
  id: string;
  title: string;
  description: string;
  status: StepStatus;
  error?: string;
}

interface DeviceCreationState {
  isCreating: boolean;
  currentStepIndex: number;
  steps: CreationStep[];
}

@Component({
  selector: 'app-add-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule, LoginModalComponent],
  template: `
    <div class="add-simulator-container">
      <div class="add-simulator-main">      <div class="add-simulator-header">
        <h3>Add New Simulator</h3>
      </div>

        <div class="add-simulator-content" [class.blurred]="creationState.isCreating">
          <form (ngSubmit)="createSimulator()" #simulatorForm="ngForm" class="simulator-form">
            <div class="form-group">
              <label for="deviceId">Device ID</label>
              <div class="input-with-button">
                <input 
                  type="text" 
                  id="deviceId"
                  name="deviceId"
                  [(ngModel)]="newSimulator.deviceId" 
                  required
                  class="form-control"
                  placeholder="Enter device ID (e.g., AA:BB:CC:DD:EE:FF)">
                <button 
                  type="button" 
                  class="btn btn-generate" 
                  (click)="generateDeviceId()"
                  title="Generate MAC address">
                  Generate
                </button>
              </div>
            </div>

            <div class="form-group">
              <label>Device Type</label>
              <div class="device-type-selection">
                <div 
                  *ngFor="let type of deviceTypes" 
                  class="device-type-option"
                  [class.selected]="isDeviceTypeSelected(type)"
                  (click)="selectDeviceType(type)">
                  <span class="device-type-label">{{ type }}</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>Environment</label>
              <div class="environment-selection">
                <div 
                  *ngFor="let env of environments" 
                  class="environment-option"
                  [class.selected]="isEnvironmentSelected(env)"
                  [style.border-color]="isEnvironmentSelected(env) ? getEnvironmentColors(env).primary : 'var(--border-color)'"
                  [style.background-color]="isEnvironmentSelected(env) ? getEnvironmentColors(env).background : 'transparent'"
                  (click)="selectEnvironment(env)">
                  <span 
                    class="environment-label"
                    [style.color]="isEnvironmentSelected(env) ? getEnvironmentColors(env).primary : 'var(--text-primary)'">
                    {{ getEnvironmentLabel(env) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Session Info for Selected Environment -->
            <div class="form-group">
              <label>Session</label>
              <div class="session-info-display">
                <div *ngIf="getEnvironmentSession(newSimulator.environment); else noSession" class="active-session">
                  <div class="user-details">
                    <span class="user-name">{{ getEnvironmentSession(newSimulator.environment)?.user.fname }} {{ getEnvironmentSession(newSimulator.environment)?.user.lname }}</span>
                    <span class="user-org">{{ getEnvironmentSession(newSimulator.environment)?.user.orgName }}</span>
                  </div>
                  <span class="session-status active">Active Session</span>
                </div>
                <ng-template #noSession>
                  <div class="no-session">
                    <div class="session-placeholder">
                      <span class="session-status inactive">No Active Session</span>
                      <button 
                        type="button" 
                        class="btn btn-login" 
                        (click)="openLoginModal()">
                        Login to {{ getEnvironmentLabel(newSimulator.environment) }}
                      </button>
                    </div>
                  </div>
                </ng-template>
              </div>
            </div>

            <div class="form-group">
              <label for="simulatorDescription">Description (Optional)</label>
              <textarea 
                id="simulatorDescription" 
                name="description"
                [(ngModel)]="newSimulator.description" 
                class="form-control"
                rows="3"
                placeholder="Enter a description for this simulator..."></textarea>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-secondary" (click)="goBack()">
                Cancel
              </button>
              <button 
                type="submit" 
                class="btn btn-primary"
                [disabled]="simulatorForm.invalid || isCreatingSimulator || !getEnvironmentSession(newSimulator.environment)">
                <span *ngIf="!isCreatingSimulator">Create Simulator</span>
                <span *ngIf="isCreatingSimulator" class="creating-text">
                  <span class="creating-spinner"></span>
                  <span>Creating...</span>
                </span>
              </button>
            </div>
          </form>
        </div>
      </div>
      
      <!-- Creation Progress Stepper Overlay -->
      <div *ngIf="creationState.isCreating" class="creation-overlay">
        <div class="creation-stepper">
          <div class="stepper-header">
            <h3>Creating Device Simulator</h3>
            <p>Please wait while we set up your device simulator...</p>
          </div>
          
          <div class="stepper-content">
            <div class="steps-container">
              <div 
                *ngFor="let step of creationState.steps; let i = index" 
                class="step-item"
                [class.current]="i === creationState.currentStepIndex"
                [class.completed]="step.status === 'success'"
                [class.error]="step.status === 'error'"
                [class.loading]="step.status === 'loading'">
                
                <div class="step-indicator">
                  <div class="step-number" *ngIf="step.status === 'pending'">
                    {{ i + 1 }}
                  </div>
                  <div class="step-icon" *ngIf="step.status === 'success'">
                    ✓
                  </div>
                  <div class="step-icon error" *ngIf="step.status === 'error'">
                    ✗
                  </div>
                  <div class="step-spinner" *ngIf="step.status === 'loading'">
                    <div class="spinner"></div>
                  </div>
                </div>
                
                <div class="step-content">
                  <div class="step-title">{{ step.title }}</div>
                  <div class="step-description">{{ step.description }}</div>
                  <div *ngIf="step.error" class="step-error">{{ step.error }}</div>
                </div>
                
                <div class="step-connector" *ngIf="i < creationState.steps.length - 1"></div>
              </div>
            </div>
            
            <!-- Error Actions -->
            <div *ngIf="hasErrorSteps()" class="error-actions">
              <button type="button" class="btn btn-secondary" (click)="cancelCreation()">
                Cancel
              </button>
              <button type="button" class="btn btn-primary" (click)="retryCreation()">
                Retry
              </button>
            </div>
            
            <!-- Success Message -->
            <div *ngIf="allStepsSuccessful()" class="success-message">
              <div class="success-icon">🎉</div>
              <div class="success-text">
                <h4>Simulator Created Successfully!</h4>
                <p>Your device simulator has been created and is ready to use.</p>
              </div>
              <div class="success-actions">
                <button type="button" class="btn btn-primary" (click)="goToDashboard()">
                  Go to Dashboard
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Login Modal -->
      <app-login-modal
        [isVisible]="showLoginModal"
        [environment]="newSimulator.environment"
        (loginSuccess)="onLoginSuccess()"
        (modalClosed)="onLoginModalClosed()">
      </app-login-modal>
    </div>
  `,
  styleUrls: ['./add-simulator.component.scss']
})
export class AddSimulatorComponent implements OnInit {
  environments = EnvironmentConfigService.getEnvironmentKeys();
  deviceTypes = ['AIQ Core', 'AIQ Core Torque'];
  
  newSimulator: NewSimulatorForm = {
    deviceId: '',
    type: this.deviceTypes[0], // Set first device type as default
    environment: EnvironmentConfigService.getEnvironmentKeys()[0], // Set first environment as default
    description: ''
  };
  
  loginForm: LoginFormData = {
    username: '',
    password: '',
    isLoggingIn: false
  };
  
  isCreatingSimulator = false;
  showLoginModal = false;
  creationState: DeviceCreationState = {
    isCreating: false,
    currentStepIndex: 0,
    steps: [        {
          id: 'get-credentials',
          title: 'Getting Credentials',
          description: 'Requesting device certificate and private key',
          status: StepStatus.PENDING
        },
        {
          id: 'check-enrollment',
          title: 'Checking Enrollment',
          description: 'Verifying device enrollment status',
          status: StepStatus.PENDING
        },
        {
          id: 'create-enrollment',
          title: 'Creating Enrollment',
          description: 'Enrolling device with provisioning service',
          status: StepStatus.PENDING
        },
        {
          id: 'create-simulator',
          title: 'Creating Simulator',
          description: 'Setting up simulator configuration',
          status: StepStatus.PENDING
        },
        {
          id: 'start-simulator',
          title: 'Starting Simulator',
          description: 'Initializing simulator and connecting to IoT Hub',
          status: StepStatus.PENDING
        }
    ]
  };

  constructor(
    private router: Router,
    private authService: AuthService,
    private simulatorService: SimulatorService,
    private certService: CertService,
    private componentService: ComponentService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.resetSimulatorForm();
  }

  private resetSimulatorForm(): void {
    this.newSimulator = {
      deviceId: '',
      type: 'AIQ Core',
      environment: 'dev',
      description: ''
    };
    this.isCreatingSimulator = false;
    this.resetCreationState();
    this.resetLoginForm();
  }

  private resetCreationState(): void {
    this.creationState = {
      isCreating: false,
      currentStepIndex: 0,
      steps: [
        {
          id: 'get-credentials',
          title: 'Getting Credentials',
          description: 'Requesting device certificate and private key',
          status: StepStatus.PENDING
        },
        {
          id: 'check-enrollment',
          title: 'Checking Enrollment',
          description: 'Verifying device enrollment status',
          status: StepStatus.PENDING
        },
        {
          id: 'create-enrollment',
          title: 'Creating Enrollment',
          description: 'Enrolling device with provisioning service',
          status: StepStatus.PENDING
        },
        {
          id: 'create-simulator',
          title: 'Creating Simulator',
          description: 'Setting up simulator configuration',
          status: StepStatus.PENDING
        },
        {
          id: 'start-simulator',
          title: 'Starting Simulator',
          description: 'Initializing simulator and connecting to IoT Hub',
          status: StepStatus.PENDING
        }
      ]
    };
  }

  private resetLoginForm(): void {
    this.loginForm = {
      username: '',
      password: '',
      isLoggingIn: false
    };
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  generateDeviceId(): void {
    this.newSimulator.deviceId = this.generateMacAddress();
  }

  createSimulator(): void {
    if (!this.newSimulator.deviceId || !this.newSimulator.type || !this.newSimulator.environment) {
      return;
    }

    this.isCreatingSimulator = true;
    
    // Add a small delay before showing the overlay for smoother transition
    setTimeout(() => {
      this.creationState.isCreating = true;
      this.creationState.currentStepIndex = 0;
      this.executeCreationSteps();
    }, 100);
  }

  private executeCreationSteps(): void {
    this.executeStep1GetCredentials();
  }

  private executeStep1GetCredentials(): void {
    this.updateStepStatus(0, StepStatus.LOADING);

    this.certService.getCertificateAndKey(this.newSimulator.deviceId, this.newSimulator.environment).subscribe({
      next: (certResponse) => {
        this.updateStepStatus(0, StepStatus.SUCCESS);
        this.creationState.currentStepIndex = 1;
        
        // Store cert response for later steps
        (this as any).certResponse = certResponse;
        
        // Move to next step
        setTimeout(() => this.executeStep2CheckEnrollment(), 500);
      },
      error: (error) => {
        this.updateStepStatus(0, StepStatus.ERROR, error.message || 'Failed to get device credentials');
      }
    });
  }

  private executeStep2CheckEnrollment(): void {
    this.updateStepStatus(1, StepStatus.LOADING);

    this.componentService.checkDeviceEnrollment(this.newSimulator.deviceId, this.newSimulator.environment).subscribe({
      next: (enrollmentResponse) => {
        if (enrollmentResponse.enrolled) {
          // Device is already enrolled - show error
          this.updateStepStatus(1, StepStatus.ERROR, 'Device is already enrolled in this environment');
        } else {
          // Device is not enrolled - proceed to enrollment creation
          this.updateStepStatus(1, StepStatus.SUCCESS);
          this.creationState.currentStepIndex = 2;
          setTimeout(() => this.executeStep3CreateEnrollment(), 500);
        }
      },
      error: (error) => {
        this.updateStepStatus(1, StepStatus.ERROR, error.message || 'Failed to check enrollment status');
      }
    });
  }

  private executeStep3CreateEnrollment(): void {
    this.updateStepStatus(2, StepStatus.LOADING);

    const certResponse = (this as any).certResponse;
    
    this.componentService.enrollDevice(
      this.newSimulator.deviceId, 
      this.newSimulator.environment, 
      certResponse.certificate
    ).subscribe({
      next: () => {
        this.updateStepStatus(2, StepStatus.SUCCESS);
        this.creationState.currentStepIndex = 3;
        setTimeout(() => this.executeStep4CreateSimulator(), 500);
      },
      error: (error) => {
        const errorMessage = error.error?.message || error.message || 'Failed to enroll device';
        this.updateStepStatus(2, StepStatus.ERROR, errorMessage);
      }
    });
  }

  private executeStep4CreateSimulator(): void {
    this.updateStepStatus(3, StepStatus.LOADING);

    const certResponse = (this as any).certResponse;
    
    this.simulatorService.createDevice(
      this.newSimulator.deviceId,
      this.newSimulator.environment,
      certResponse.certificate,
      certResponse.privateKey
    ).subscribe({
      next: (success) => {
        if (success) {
          this.updateStepStatus(3, StepStatus.SUCCESS);
          this.creationState.currentStepIndex = 4;
          
          // Create new device object for the local state
          const newDevice: Device = {
            id: this.newSimulator.deviceId,
            type: this.newSimulator.type,
            status: 'stopped',
            environment: this.newSimulator.environment as 'dev' | 'test' | 'prod',
            lastActivity: new Date()
          };

          console.log('New device created:', newDevice);
          
          // Move to final step
          setTimeout(() => this.executeStep5StartSimulator(), 500);
        } else {
          this.updateStepStatus(3, StepStatus.ERROR, 'Failed to create simulator');
        }
      },
      error: (error) => {
        const errorMessage = error.error?.message || error.message || 'Failed to create simulator';
        this.updateStepStatus(3, StepStatus.ERROR, errorMessage);
      }
    });
  }

  private executeStep5StartSimulator(): void {
    this.updateStepStatus(4, StepStatus.LOADING);
    
    // TODO: When simulator start API is available, replace this with actual API call
    // For now, simulate the step
    setTimeout(() => {
      this.updateStepStatus(4, StepStatus.SUCCESS);
      
      // All steps completed successfully
      setTimeout(() => this.onCreationComplete(), 1000);
    }, 1000);
  }

  private updateStepStatus(stepIndex: number, status: StepStatus, error?: string): void {
    if (stepIndex >= 0 && stepIndex < this.creationState.steps.length) {
      this.creationState.steps[stepIndex].status = status;
      if (error) {
        this.creationState.steps[stepIndex].error = error;
      }
    }
  }

  private onCreationComplete(): void {
    // Just mark creation as complete, don't auto-navigate
    // User will click "Go to Dashboard" button manually
  }

  goToDashboard(): void {
    this.resetCreationState();
    this.isCreatingSimulator = false;
    this.router.navigate(['/dashboard']);
  }

  retryCreation(): void {
    this.resetCreationState();
    this.createSimulator();
  }

  cancelCreation(): void {
    this.resetCreationState();
    this.isCreatingSimulator = false;
  }

  hasErrorSteps(): boolean {
    return this.creationState.steps.some(step => step.status === StepStatus.ERROR);
  }

  allStepsSuccessful(): boolean {
    return this.creationState.steps.every(step => step.status === StepStatus.SUCCESS);
  }

  private generateMacAddress(): string {
    const hexChars = '0123456789ABCDEF';
    // Start with ZZ:ZZ:ZZ to identify simulated devices
    let mac = 'ZZ:ZZ:ZZ';
    
    // Generate the remaining 3 octets with valid hex characters
    for (let i = 0; i < 3; i++) {
      mac += ':';
      mac += hexChars.charAt(Math.floor(Math.random() * 16));
      mac += hexChars.charAt(Math.floor(Math.random() * 16));
    }
    return mac;
  }

  // Environment session methods
  getEnvironmentSession(environment: string): any {
    return this.authService.getSessionForEnvironment(environment);
  }

  onEnvironmentChange(): void {
    // Reset login form when environment changes
    this.resetLoginForm();
  }

  openLoginModal(): void {
    this.showLoginModal = true;
  }

  onLoginSuccess(): void {
    this.showLoginModal = false;
    // The template will automatically update to show the session info
  }

  onLoginModalClosed(): void {
    this.showLoginModal = false;
  }

  selectDeviceType(type: string): void {
    this.newSimulator.type = type;
  }

  isDeviceTypeSelected(type: string): boolean {
    return this.newSimulator.type === type;
  }

  selectEnvironment(env: string): void {
    this.newSimulator.environment = env;
    this.onEnvironmentChange();
  }

  getEnvironmentLabel(env: string): string {
    return EnvironmentConfigService.getDisplayName(env);
  }

  getEnvironmentColors(env: string) {
    return EnvironmentConfigService.getColors(env);
  }

  isEnvironmentSelected(env: string): boolean {
    return this.newSimulator.environment === env;
  }

  loginToEnvironment(environment: string): void {
    if (!this.loginForm.username || !this.loginForm.password) {
      return;
    }

    this.loginForm.isLoggingIn = true;
    
    this.authService.login(this.loginForm.username, this.loginForm.password, environment).subscribe({
      next: (response) => {
        console.log(`Login successful for ${environment}:`, response);
        this.resetLoginForm();
        // The template will automatically update to show the session info
      },
      error: (error) => {
        console.error(`Login failed for ${environment}:`, error);
        this.loginForm.isLoggingIn = false;
        // Show error message - in a real app, you might want to display this in the UI
        alert(`Login failed: ${error.message || 'Please try again.'}`);
      }
    });
  }
}
