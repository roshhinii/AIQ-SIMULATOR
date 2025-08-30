import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export interface ApiDevice {
  id: string;
  type: string;
  environment: 'DEV' | 'TEST' | 'PROD';
  status: 'STOPPED' | 'STARTING' | 'CONNECTING' | 'CONNECTED';
}

export interface Device {
  id: string; // MAC address
  type: string;
  environment: 'dev' | 'test' | 'prod';
  status: 'stopped' | 'starting' | 'connecting' | 'connected';
  lastActivity: Date;
}

@Injectable({
  providedIn: 'root'
})
export class SimulatorService {
  private readonly API_BASE_URL = 'http://localhost:8080';
  private devicesSubject = new BehaviorSubject<Device[]>([]);
  public devices$ = this.devicesSubject.asObservable();

  constructor(private http: HttpClient) {}

  getSimulatorServiceUrl(): string {
    return this.API_BASE_URL;
  }

  /**
   * Fetch devices from the API
   */
  getDevices(): Observable<Device[]> {
    return this.http.get<ApiDevice[]>(`${this.API_BASE_URL}/devices`).pipe(
      map(apiDevices => this.mapApiDevicesToDevices(apiDevices)),
      catchError(error => {
        console.error('Error fetching devices:', error);
        // Return empty array when backend is unavailable
        return of([]);
      })
    );
  }

  /**
   * Refresh the devices list and update the subject
   */
  refreshDevices(): void {
    this.getDevices().subscribe(devices => {
      this.devicesSubject.next(devices);
    });
  }

  /**
   * Start a device (API call)
   */
  startDevice(deviceId: string): Observable<boolean> {
    return this.http.put<any>(`${this.API_BASE_URL}/devices/${deviceId}/action/start`, {}).pipe(
      map(() => {
        this.updateDeviceStatus(deviceId, 'connected');
        return true;
      }),
      catchError(error => {
        console.error('Error starting device:', error);
        return of(false);
      })
    );
  }

  /**
   * Stop a device (API call)
   */
  stopDevice(deviceId: string): Observable<boolean> {
    return this.http.put<any>(`${this.API_BASE_URL}/devices/${deviceId}/action/stop`, {}).pipe(
      map(() => {
        this.updateDeviceStatus(deviceId, 'stopped');
        return true;
      }),
      catchError(error => {
        console.error('Error stopping device:', error);
        return of(false);
      })
    );
  }

  /**
   * Create a new device (API call)
   */
  createDevice(deviceId: string, environment: string, certificate: string, privateKey: string): Observable<boolean> {
    const payload = {
      id: deviceId,
      environment: environment,
      privateKey: privateKey,
      certificate: certificate
    };

    return this.http.post<any>(`${this.API_BASE_URL}/devices`, payload).pipe(
      map(() => {
        // Refresh devices list after successful creation
        this.refreshDevices();
        return true;
      }),
      catchError(error => {
        console.error('Error creating device:', error);
        return of(false);
      })
    );
  }

  /**
   * Check simulator service health
   */
  checkHealth(): Observable<boolean> {
    return this.http.get(`${this.API_BASE_URL}/actuator/health`).pipe(
      map(() => true),
      catchError(error => {
        console.error('Simulator service health check failed:', error);
        return of(false);
      })
    );
  }

  /**
   * Map API devices to internal device format
   */
  private mapApiDevicesToDevices(apiDevices: ApiDevice[]): Device[] {
    return apiDevices.map(apiDevice => ({
      id: apiDevice.id,
      type: apiDevice.type,
      environment: apiDevice.environment.toLowerCase() as 'dev' | 'test' | 'prod',
      status: apiDevice.status.toLowerCase() as 'stopped' | 'starting' | 'connecting' | 'connected',
      lastActivity: this.generateRandomLastActivity(apiDevice.status === 'CONNECTED')
    }));
  }

  /**
   * Update device status locally
   */
  private updateDeviceStatus(deviceId: string, newStatus: 'stopped' | 'starting' | 'connecting' | 'connected'): void {
    const currentDevices = this.devicesSubject.value;
    const updatedDevices = currentDevices.map(device => {
      if (device.id === deviceId) {
        return {
          ...device,
          status: newStatus,
          lastActivity: new Date()
        };
      }
      return device;
    });
    this.devicesSubject.next(updatedDevices);
  }

  /**
   * Generate random last activity time
   */
  private generateRandomLastActivity(isConnected: boolean): Date {
    const now = new Date();
    if (isConnected) {
      // Connected devices have recent activity (within last 30 minutes)
      return new Date(now.getTime() - Math.random() * 30 * 60000);
    } else {
      // Stopped/disconnected devices have older activity (within last 24 hours)
      return new Date(now.getTime() - Math.random() * 24 * 60 * 60000);
    }
  }
}
