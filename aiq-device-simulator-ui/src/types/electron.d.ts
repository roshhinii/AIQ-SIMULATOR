// Global type declarations for Electron API
declare global {
  interface Window {
    electronAPI: {
      minimizeWindow: () => void;
      maximizeWindow: () => void;
      closeWindow: () => void;
      isWindowMaximized: () => Promise<boolean>;
      onWindowMaximized: (callback: (isMaximized: boolean) => void) => void;
      removeAllListeners: (channel: string) => void;
    };
  }
}

export {};
