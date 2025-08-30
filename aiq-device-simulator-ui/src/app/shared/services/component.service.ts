import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './multi-auth.service';
import { EnvironmentConfigService } from '../../config/environment.config';

export interface DeviceEnrollmentResponse {
  enrolled: boolean;
}

export interface DeviceEnrollmentRequest {
  devicePublicKeyPemString: string;
}

@Injectable({
  providedIn: 'root'
})
export class ComponentService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Check if a device is enrolled in the specified environment
   * @param deviceIdentifier The device identifier
   * @param environment The environment to check
   * @returns Observable of enrollment status response
   */
  checkDeviceEnrollment(deviceIdentifier: string, environment: string): Observable<DeviceEnrollmentResponse> {
    const token = this.authService.getTokenForEnvironment(environment);

    if (!token) {
      return throwError(() => new Error(`Not authenticated for ${environment} environment`));
    }

    const backendUrl = EnvironmentConfigService.getBackendUrl(environment);
    const url = `${backendUrl}/component-service/api/devices/${deviceIdentifier}/provisioning`;

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    });

    return this.http.get<DeviceEnrollmentResponse>(url, { headers }).pipe(
      catchError(error => {
        console.error('Error checking device enrollment:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Enroll a device in the specified environment
   * @param deviceIdentifier The device identifier
   * @param environment The environment to enroll in
   * @param devicePublicKey The device public key PEM string
   * @returns Observable of enrollment result
   */
  enrollDevice(deviceIdentifier: string, environment: string, devicePublicKey: string): Observable<any> {
    const token = this.authService.getTokenForEnvironment(environment);

    if (!token) {
      return throwError(() => new Error(`Not authenticated for ${environment} environment`));
    }

    const backendUrl = EnvironmentConfigService.getBackendUrl(environment);
    const url = `${backendUrl}/component-service/api/devices/${deviceIdentifier}/provisioning`;

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    });

    const body: DeviceEnrollmentRequest = {
      devicePublicKeyPemString: devicePublicKey
    };

    return this.http.post(url, body, { headers }).pipe(
      catchError(error => {
        console.error('Error enrolling device:', error);
        return throwError(() => error);
      })
    );
  }
}
