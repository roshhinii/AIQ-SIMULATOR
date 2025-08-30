import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './multi-auth.service';
import { EnvironmentConfigService } from '../../config/environment.config';

export interface CertificateResponse {
  certificate: string;
  privateKey: string;
}

@Injectable({
  providedIn: 'root'
})
export class CertService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Get certificate and private key for a specific device in a specified environment
   * @param deviceId The device ID to get the certificate for
   * @param environment The environment to use
   * @returns Observable of certificate response
   */
  getCertificateAndKey(deviceId: string, environment: string): Observable<CertificateResponse> {
    const token = this.authService.getTokenForEnvironment(environment);

    if (!token) {
      return throwError(() => new Error(`Not authenticated for ${environment} environment`));
    }

    const backendUrl = EnvironmentConfigService.getBackendUrl(environment);
    const url = `${backendUrl}/cert-service/api/certs/${deviceId}`;

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    });

    return this.http.get<CertificateResponse>(url, { headers }).pipe(
      catchError(error => {
        console.error('Error fetching certificate:', error);
        return throwError(() => error);
      })
    );
  }
}
