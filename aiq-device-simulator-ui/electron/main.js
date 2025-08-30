const { app, BrowserWindow, Menu, ipcMain } = require('electron');
const path = require('path');
const isDev = require('electron-is-dev');

let mainWindow;

function createWindow() {
  // Create the browser window
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    frame: false, // Remove window frame
    titleBarStyle: 'hidden', // Hide title bar
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      enableRemoteModule: false,
      preload: path.join(__dirname, 'preload.js')
    },
    icon: path.join(__dirname, '../assets/icon.png'), // Optional: add an icon
    show: false,
    minWidth: 800,
    minHeight: 600,
    resizable: true,
    maximizable: true,
    minimizable: true,
    // Add these to prevent potential responsiveness issues
    webSecurity: true,
    backgroundThrottling: false
  });

  // Load the app
  const startUrl = isDev 
    ? 'http://localhost:4200' 
    : `file://${path.join(__dirname, '../dist/aiq-device-emulator-ui/index.html')}`;
  
  mainWindow.loadURL(startUrl);

  // Overwrite CORS within Electron so we are able to connect to the AIQ Backend on any ENV as well as the Simulator Service
  mainWindow.webContents.session.webRequest.onBeforeSendHeaders(
    (details, callback) => {
      callback({ requestHeaders: { Origin: '*', ...details.requestHeaders } });
    },
  );

  mainWindow.webContents.session.webRequest.onHeadersReceived((details, callback) => {
    callback({
      responseHeaders: {
        'Access-Control-Allow-Origin': ['*'],
        ...details.responseHeaders,
      },
    });
  });


  // Show window when ready to prevent visual flash
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  // Open DevTools in development
  if (isDev) {
    mainWindow.webContents.openDevTools();
  }

  // Handle window closed
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Handle window maximize/unmaximize events
  mainWindow.on('maximize', () => {
    try {
      mainWindow.webContents.send('window-maximized', true);
    } catch (error) {
      console.error('Error sending maximize event:', error);
    }
  });

  mainWindow.on('unmaximize', () => {
    try {
      mainWindow.webContents.send('window-maximized', false);
    } catch (error) {
      console.error('Error sending unmaximize event:', error);
    }
  });

  mainWindow.on('restore', () => {
    try {
      mainWindow.webContents.send('window-maximized', false);
    } catch (error) {
      console.error('Error sending restore event:', error);
    }
  });
}

// This method will be called when Electron has finished initialization
app.whenReady().then(() => {
  createWindow();
  
  // Set up IPC handlers for window controls
  setupWindowControls();
});

// Set up IPC handlers for window controls
function setupWindowControls() {
  // Minimize window
  ipcMain.on('window-minimize', () => {
    console.log('IPC: window-minimize received');
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.minimize();
    }
  });

  // Maximize/unmaximize window
  ipcMain.on('window-maximize', () => {
    console.log('IPC: window-maximize received');
    if (mainWindow && !mainWindow.isDestroyed()) {
      const wasMaximized = mainWindow.isMaximized();
      console.log('Window was maximized:', wasMaximized);
      
      if (wasMaximized) {
        mainWindow.unmaximize();
      } else {
        mainWindow.maximize();
      }
    }
  });

  // Close window
  ipcMain.on('window-close', () => {
    console.log('IPC: window-close received');
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.close();
    }
  });

  // Get window state
  ipcMain.handle('window-is-maximized', () => {
    const isMaximized = mainWindow && !mainWindow.isDestroyed() ? mainWindow.isMaximized() : false;
    console.log('IPC: window-is-maximized requested, returning:', isMaximized);
    return isMaximized;
  });
}

// Quit when all windows are closed
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

// Create application menu
const template = [
  {
    label: 'File',
    submenu: [
      {
        label: 'Quit',
        accelerator: process.platform === 'darwin' ? 'Cmd+Q' : 'Ctrl+Q',
        click: () => {
          app.quit();
        }
      }
    ]
  },
  {
    label: 'View',
    submenu: [
      { role: 'reload' },
      { role: 'forceReload' },
      { role: 'toggleDevTools' },
      { type: 'separator' },
      { role: 'resetZoom' },
      { role: 'zoomIn' },
      { role: 'zoomOut' },
      { type: 'separator' },
      { role: 'togglefullscreen' }
    ]
  }
];

const menu = Menu.buildFromTemplate(template);
Menu.setApplicationMenu(menu);
