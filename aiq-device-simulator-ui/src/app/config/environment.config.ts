export interface EnvironmentConfig {
  key: string;
  displayName: string;
  backendUrl: string;
  colors: {
    primary: string;
    background: string;
    border: string;
    text: string;
  };
}

export const ENVIRONMENT_CONFIG: Record<string, EnvironmentConfig> = {
  dev: {
    key: 'dev',
    displayName: 'Development',
    backendUrl: 'https://apim-aiq-shared.azure-api.net/dev',
    colors: {
      primary: 'rgb(34, 197, 94)',
      background: 'rgba(34, 197, 94, 0.2)',
      border: 'rgba(34, 197, 94, 0.3)',
      text: 'rgb(134, 239, 172)'
    }
  },
  test: {
    key: 'test',
    displayName: 'Test',
    backendUrl: 'https://apim-aiq-shared.azure-api.net/test',
    colors: {
      primary: 'rgb(251, 191, 36)',
      background: 'rgba(251, 191, 36, 0.2)',
      border: 'rgba(251, 191, 36, 0.3)',
      text: 'rgb(253, 224, 71)'
    }
  },
  prod: {
    key: 'prod',
    displayName: 'Production',
    backendUrl: 'https://apim-aiq-shared.azure-api.net/prod',
    colors: {
      primary: 'rgb(239, 68, 68)',
      background: 'rgba(239, 68, 68, 0.2)',
      border: 'rgba(239, 68, 68, 0.3)',
      text: 'rgb(252, 165, 165)'
    }
  }
};

export class EnvironmentConfigService {
  static getConfig(environment: string): EnvironmentConfig | null {
    return ENVIRONMENT_CONFIG[environment] || null;
  }

  static getDisplayName(environment: string): string {
    const config = this.getConfig(environment);
    return config ? config.displayName : environment;
  }

  static getShortName(environment: string): string {
    const shortNames: { [key: string]: string } = {
      'dev': 'DEV',
      'test': 'TEST', 
      'prod': 'PROD'
    };
    return shortNames[environment] || environment.toUpperCase();
  }

  static getBackendUrl(environment: string): string {
    const config = this.getConfig(environment);
    return config ? config.backendUrl : 'http://localhost:8080';
  }

  static getColors(environment: string) {
    const config = this.getConfig(environment);
    return config ? config.colors : ENVIRONMENT_CONFIG['dev'].colors;
  }

  static getAllEnvironments(): EnvironmentConfig[] {
    return Object.values(ENVIRONMENT_CONFIG);
  }

  static getEnvironmentKeys(): string[] {
    return Object.keys(ENVIRONMENT_CONFIG);
  }

  static getEnvironmentCssClass(environment: string): string {
    return `env-${environment}`;
  }
}
