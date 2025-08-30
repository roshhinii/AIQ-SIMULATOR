import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface AiqError {
  code?: string;
  message?: string;
  traceId?: string;
}

@Component({
  selector: 'app-aiq-error',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="aiq-error-container" *ngIf="error">
      <div class="error-header">
        <span class="error-icon">⚠️</span>
        <h4>AIQ Backend Failure</h4>
      </div>
      
      <div class="error-details">
        <div class="error-field" *ngIf="error.code">
          <span class="field-label">Code:</span>
          <span class="field-value">{{ error.code }}</span>
        </div>
        
        <div class="error-field" *ngIf="error.message">
          <span class="field-label">Message:</span>
          <span class="field-value">{{ error.message }}</span>
        </div>
        
        <div class="error-field" *ngIf="error.traceId">
          <span class="field-label">TraceID:</span>
          <span class="field-value trace-id">{{ error.traceId }}</span>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./aiq-error.component.scss']
})
export class AiqErrorComponent {
  @Input() error: AiqError | null = null;
}
