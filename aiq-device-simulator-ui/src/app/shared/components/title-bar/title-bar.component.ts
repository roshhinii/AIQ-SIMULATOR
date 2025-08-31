import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService, MultiAuthState, EnvironmentSession, AuthState, UserInfo } from '../../services/multi-auth.service';
import { SimulatorService } from '../../services/simulator.service';
import { LoginModalComponent } from '../login-modal/login-modal.component';
import { EnvironmentConfigService } from '../../../config/environment.config';
import { Subscription } from 'rxjs';

declare global {
  interface Window {
    require: any;
  }
}

@Component({
  selector: 'app-title-bar',
  standalone: true,
  imports: [CommonModule, LoginModalComponent],
  template: `
    <div class="title-bar" (mousedown)="startDrag($event)">
      <div class="title-bar-content">
        <div class="window-controls">
          <button 
            class="control-btn minimize-btn" 
            (click)="minimizeWindow()"
            title="Minimize">
            <span class="control-icon">−</span>
          </button>
          <button 
            class="control-btn maximize-btn" 
            (click)="maximizeWindow()"
            [title]="isMaximized ? 'Restore' : 'Maximize'">
            <span class="control-icon" [class.maximized]="isMaximized">
              {{ isMaximized ? '□' : '❐' }}
            </span>
          </button>
          <button 
            class="control-btn close-btn" 
            (click)="closeWindow()"
            title="Close">
            <span class="control-icon">×</span>
          </button>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./title-bar.component.scss']
})
export class TitleBarComponent implements OnInit, OnDestroy {
  isMaximized = false;
  multiAuthState: MultiAuthState | null = null;
  environments = EnvironmentConfigService.getEnvironmentKeys();
  isSimulatorServiceHealthy = false;
  tooltipVisible = false;
  hoveredEnv: string | null = null;
  tooltipPosition = { x: 0, y: 0 };
  showLoginModal = false;
  loginEnvironment = '';
  
  private multiAuthSubscription?: Subscription;
  private electronAPI: any;
  private isElectron = false;
  private hideTooltipTimeout?: any;

  constructor(
    private authService: AuthService,
    private router: Router,
    private simulatorService: SimulatorService
  ) {}

  ngOnInit() {
    this.initializeElectron();
    this.subscribeToMultiAuthState();
    this.checkSimulatorServiceHealth();
    
    // Check simulator service health periodically
    setInterval(() => {
      this.checkSimulatorServiceHealth();
    }, 30000); // Check every 30 seconds
  }

  ngOnDestroy() {
    if (this.multiAuthSubscription) {
      this.multiAuthSubscription.unsubscribe();
    }
    if (this.hideTooltipTimeout) {
      clearTimeout(this.hideTooltipTimeout);
    }
    
    // Clean up Electron event listeners
    if (this.isElectron && this.electronAPI && this.electronAPI.removeAllListeners) {
      try {
        this.electronAPI.removeAllListeners('window-maximized');
      } catch (error) {
        console.log('Error removing Electron listeners:', error);
      }
    }
  }

  private initializeElectron(): void {
    // Check if we're running in Electron
    console.log('Initializing Electron...');
    console.log('window.electronAPI available:', !!(window as any).electronAPI);
    console.log('window.require available:', !!(window as any).require);
    
    if (typeof window !== 'undefined' && (window as any).electronAPI) {
      try {
        this.electronAPI = (window as any).electronAPI;
        this.isElectron = true;
        console.log('Electron API initialized successfully');

        // Check initial maximized state
        this.electronAPI.isWindowMaximized().then((maximized: boolean) => {
          this.isMaximized = maximized;
          console.log('Initial maximized state:', maximized);
        }).catch((error: any) => {
          console.error('Error checking maximized state:', error);
        });

        // Listen for window maximize/restore events
        this.electronAPI.onWindowMaximized((isMaximized: boolean) => {
          this.isMaximized = isMaximized;
          console.log('Window maximized state changed:', isMaximized);
        });
      } catch (error) {
        console.log('Error initializing Electron:', error);
        this.isElectron = false;
      }
    } else if (typeof window !== 'undefined' && (window as any).require) {
      // Fallback for development or older Electron versions
      try {
        const { ipcRenderer } = (window as any).require('electron');
        this.electronAPI = {
          minimizeWindow: () => ipcRenderer.send('window-minimize'),
          maximizeWindow: () => ipcRenderer.send('window-maximize'),
          closeWindow: () => ipcRenderer.send('window-close'),
          isWindowMaximized: () => ipcRenderer.invoke('window-is-maximized'),
          onWindowMaximized: (callback: (isMaximized: boolean) => void) => {
            ipcRenderer.on('window-maximized', (_event: any, isMaximized: boolean) => callback(isMaximized));
          },
          removeAllListeners: (channel: string) => {
            ipcRenderer.removeAllListeners(channel);
          }
        };
        this.isElectron = true;
        console.log('Electron fallback API initialized');

        // Check initial maximized state
        this.electronAPI.isWindowMaximized().then((maximized: boolean) => {
          this.isMaximized = maximized;
          console.log('Initial maximized state (fallback):', maximized);
        }).catch((error: any) => {
          console.error('Error checking maximized state (fallback):', error);
        });

        // Listen for window maximize/restore events
        this.electronAPI.onWindowMaximized((isMaximized: boolean) => {
          this.isMaximized = isMaximized;
          console.log('Window maximized state changed (fallback):', isMaximized);
        });
      } catch (error) {
        console.log('Error initializing Electron fallback:', error);
        this.isElectron = false;
      }
    } else {
      console.log('Not running in Electron environment or electronAPI not available');
      this.isElectron = false;
    }
  }

  private subscribeToMultiAuthState(): void {
    this.multiAuthSubscription = this.authService.multiAuthState$.subscribe(state => {
      this.multiAuthState = state;
    });
  }

  // Environment management methods
  isLoggedInToEnvironment(environment: string): boolean {
    return this.authService.isLoggedInToEnvironment(environment);
  }

  getEnvironmentColors(env: string) {
    return EnvironmentConfigService.getColors(env);
  }

  getEnvironmentLabel(env: string): string {
    return EnvironmentConfigService.getDisplayName(env);
  }

  getEnvironmentShortName(env: string): string {
    return EnvironmentConfigService.getShortName(env);
  }

  getEnvironmentSession(environment: string) {
    return this.multiAuthState?.sessions.get(environment) || null;
  }

  getBackendUrl(): string {
    return this.simulatorService.getSimulatorServiceUrl();
  }

  // Simulator Service health check
  private checkSimulatorServiceHealth(): void {
    this.simulatorService.checkHealth().subscribe({
      next: (isHealthy: boolean) => {
        this.isSimulatorServiceHealthy = isHealthy;
      },
      error: () => {
        this.isSimulatorServiceHealthy = false;
      }
    });
  }

  showTooltip(env: string, event: MouseEvent): void {
    // Clear any pending hide timeout
    if (this.hideTooltipTimeout) {
      clearTimeout(this.hideTooltipTimeout);
      this.hideTooltipTimeout = undefined;
    }

    this.hoveredEnv = env;
    this.tooltipVisible = true;
    
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    this.tooltipPosition = {
      x: rect.left + rect.width / 2,
      y: rect.bottom + 5
    };
  }

  showBackendTooltip(event: MouseEvent): void {
    // Clear any pending hide timeout
    if (this.hideTooltipTimeout) {
      clearTimeout(this.hideTooltipTimeout);
      this.hideTooltipTimeout = undefined;
    }

    this.hoveredEnv = 'backend';
    this.tooltipVisible = true;
    
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    this.tooltipPosition = {
      x: rect.left + rect.width / 2,
      y: rect.bottom + 5
    };
  }

  hideTooltip(): void {
    // Add a small delay to allow moving to the tooltip
    this.hideTooltipTimeout = setTimeout(() => {
      this.tooltipVisible = false;
      this.hoveredEnv = null;
    }, 150);
  }

  onTooltipEnter(): void {
    // Clear the hide timeout when mouse enters the tooltip
    if (this.hideTooltipTimeout) {
      clearTimeout(this.hideTooltipTimeout);
      this.hideTooltipTimeout = undefined;
    }
    this.tooltipVisible = true;
  }

  onTooltipLeave(): void {
    // Immediately hide tooltip when mouse leaves the tooltip
    if (this.hideTooltipTimeout) {
      clearTimeout(this.hideTooltipTimeout);
      this.hideTooltipTimeout = undefined;
    }
    this.tooltipVisible = false;
    this.hoveredEnv = null;
  }

  logoutFromEnvironment(environment: string): void {
    this.authService.logoutFromEnvironment(environment);
    this.hideTooltip();
  }

  loginToEnvironment(environment: string): void {
    this.hideTooltip();
    this.loginEnvironment = environment;
    this.showLoginModal = true;
  }

  onLoginSuccess(): void {
    this.showLoginModal = false;
    // The tooltip and auth state will automatically update to show the new session
  }

  onLoginModalClosed(): void {
    this.showLoginModal = false;
  }

  logout(): void {
    this.authService.logoutAll();
    // Stay on dashboard since we removed the login route
    // this.router.navigate(['/login']);
  }

  startDrag(event: MouseEvent) {
    // Prevent dragging when clicking on control buttons
    const target = event.target as HTMLElement;
    if (target.closest('.window-controls')) {
      return;
    }

    if (this.isElectron && this.electronAPI) {
      // This will be handled by CSS -webkit-app-region: drag
      // No additional JavaScript needed
    }
  }

  minimizeWindow() {
    console.log('Minimize window clicked', { isElectron: this.isElectron, hasElectronAPI: !!this.electronAPI });
    if (this.isElectron && this.electronAPI) {
      this.electronAPI.minimizeWindow();
    } else {
      console.log('Minimize window (web fallback)');
    }
  }

  async maximizeWindow() {
    console.log('Maximize window clicked', { isElectron: this.isElectron, hasElectronAPI: !!this.electronAPI });
    if (this.isElectron && this.electronAPI) {
      this.electronAPI.maximizeWindow();
      // State will be updated automatically via the event listener
    } else {
      console.log('Maximize window (web fallback)');
    }
  }

  closeWindow() {
    console.log('Close window clicked', { isElectron: this.isElectron, hasElectronAPI: !!this.electronAPI });
    if (this.isElectron && this.electronAPI) {
      this.electronAPI.closeWindow();
    } else {
      console.log('Close window (web fallback)');
      // In web environment, you might want to navigate away or show a message
    }
  }
}
