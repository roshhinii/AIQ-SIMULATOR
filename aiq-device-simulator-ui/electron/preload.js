const { contextBridge, ipcRenderer } = require('electron');

console.log('Preload script loaded');

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  minimizeWindow: () => {
    console.log('Minimize window called');
    ipcRenderer.send('window-minimize');
  },
  maximizeWindow: () => {
    console.log('Maximize window called');
    ipcRenderer.send('window-maximize');
  },
  closeWindow: () => {
    console.log('Close window called');
    ipcRenderer.send('window-close');
  },
  isWindowMaximized: () => {
    console.log('Check window maximized called');
    return ipcRenderer.invoke('window-is-maximized');
  },
  onWindowMaximized: (callback) => {
    ipcRenderer.on('window-maximized', (event, isMaximized) => callback(isMaximized));
  },
  removeAllListeners: (channel) => {
    ipcRenderer.removeAllListeners(channel);
  }
});

console.log('electronAPI exposed to window');
