# AIQ Device Emulator UI

A modern desktop application built with Angular and Electron.

## Features

- 🚀 Angular 17 with standalone components
- ⚡ Electron for cross-platform desktop deployment
- 🎨 Modern, responsive UI design
- 🌙 Dark mode support
- 📦 Easy build and distribution

## Development

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn

### Installation

```bash
npm install
```

### Development Server

To run the application in development mode:

```bash
# Start Angular dev server and Electron
npm run electron:dev
```

This will:
1. Start the Angular development server on http://localhost:4200
2. Launch Electron and load the Angular app

### Building

```bash
# Build Angular app for production
npm run build:prod

# Build and package Electron app
npm run electron:pack

# Build and create distribution packages
npm run electron:dist
```

### Available Scripts

- `npm start` - Start Angular development server only
- `npm run build` - Build Angular app
- `npm run build:prod` - Build Angular app for production
- `npm run electron` - Run Electron (requires built Angular app)
- `npm run electron:dev` - Run in development mode
- `npm run electron:pack` - Package Electron app
- `npm run electron:dist` - Create distribution packages

## Project Structure

```
├── src/                    # Angular source code
│   ├── app/               # Angular app components
│   ├── assets/            # Static assets
│   ├── index.html         # Main HTML file
│   ├── main.ts           # Angular bootstrap
│   └── styles.scss       # Global styles
├── electron/              # Electron main process
│   └── main.js           # Electron main script
├── dist/                 # Build output
├── release/              # Packaged apps
├── package.json          # Dependencies and scripts
└── angular.json          # Angular configuration
```

## Technologies Used

- **Angular 17** - Frontend framework
- **Electron** - Desktop app framework
- **TypeScript** - Programming language
- **SCSS** - Styling
- **Electron Builder** - App packaging

## License

MIT

## Authentication Implementation

This application now includes real API authentication using the AIQ shared API.

### Features Added

1. **AuthService** (`src/app/services/auth.service.ts`)
   - Handles login API calls to the AIQ environment endpoints
   - Fetches detailed user information from user API endpoint
   - Manages JWT token storage and validation
   - Provides authentication state management
   - Environment-specific API URLs (dev/test/prod)
   - Stores user profile data (first name, last name, organization)

2. **Authentication Guard** (`src/app/guards/auth.guard.ts`)
   - Protects routes requiring authentication
   - Redirects unauthenticated users to login

3. **HTTP Interceptor** (`src/app/interceptors/auth.interceptor.ts`)
   - Automatically adds Bearer token to API requests
   - Centralized token management

### API Integration

The login process now makes real HTTP calls to:
- Dev: `https://apim-aiq-shared.azure-api.net/dev/auth-service/api/auth/login`
- Test: `https://apim-aiq-shared.azure-api.net/test/auth-service/api/auth/login`
- Prod: `https://apim-aiq-shared.azure-api.net/prod/auth-service/api/auth/login`

Request format:
```json
{
  "email": "<username>",
  "password": "<password>",
  "rememberme": false
}
```

### Response Format

#### Successful Login Response
```json
{
  "token": "jwt_token_here",
  "expiryAt": "2025-08-21T15:30:00Z",
  "refreshToken": "refresh_token_here",
  "refreshExpiryAt": "2025-08-28T15:30:00Z"
}
```

#### Error Response
```json
{
  "message": "Invalid Credentials",
  "code": 7004,
  "traceId": "c90f6bbdc348154bc00f3c89f1aacf0e"
}
```

### Token Management

- JWT tokens are stored in localStorage
- Token expiration is automatically checked
- Authentication state is managed through RxJS observables
- Automatic logout on token expiration

### Token Management Enhancements

- Stores token expiry time from API response
- Stores refresh token and its expiry
- Visual indicators for token expiry status:
  - Green: Token valid
  - Yellow: Expiring soon (< 1 hour for access token, < 24 hours for refresh)
  - Red: Expired
- Dashboard shows detailed token information

### Error Handling

The application handles various authentication scenarios:
- Invalid credentials (401)
- Access denied (403)
- Network connectivity issues
- Server errors
- Token expiration

### Enhanced Error Display

The login form now displays detailed error information including:
- Primary error message
- Error code (if provided)
- Trace ID (if provided) for debugging

### User Information API

After successful login, the system automatically fetches detailed user information from:
- Dev: `https://apim-aiq-shared.azure-api.net/dev/auth-service/api/user`
- Test: `https://apim-aiq-shared.azure-api.net/test/auth-service/api/user`
- Prod: `https://apim-aiq-shared.azure-api.net/prod/auth-service/api/user`

The user endpoint returns information including:
- `fname` - First name
- `lname` - Last name  
- `orgName` - Organization name

This information is stored securely and displayed in the dashboard.

### Enhanced User Experience

- Dashboard displays full name instead of just email
- Organization information is shown
- Manual refresh option for user information
- Graceful fallback if user information fetch fails

### Usage

1. Select environment (dev/test/prod)
2. Enter username and password
3. System validates credentials with the API
4. On success, user information is fetched and stored
5. Token and user data are stored securely
6. User is redirected to dashboard showing complete profile
7. Dashboard displays authentication status, user details, and token information
8. User can refresh their information manually if needed
9. Logout clears all authentication and user data

### Development

To test the authentication:
1. Run `ng serve`
2. Navigate to the login page
3. Use valid credentials for the selected environment
4. Check browser dev tools for network requests and stored tokens

### UI Layout Updates

**Title Bar Enhancement:**
- User information now displayed in the title bar
- Shows full name (First Last) when available
- Environment badge with color coding:
  - 🟢 **Dev**: Green badge
  - 🟡 **Test**: Yellow badge  
  - 🔴 **Prod**: Red badge
- Logout button integrated into title bar
- Maintains draggable window functionality

**Simplified Dashboard:**
- Removed redundant session information card
- Cleaner, more focused interface
- Emphasis on application features
- Better use of screen real estate

### Device Management Features

**Device Dashboard:**
- Grid layout displaying connected devices
- Real-time device status indicators
- Device type identification with icons
- Environment-specific color coding

**Device Information Display:**
- **Device ID**: MAC address format
- **Device Type**: AIQ Core or AIQ Core Torque
- **Environment**: Dev, Test, or Production
- **Status**: Online, Offline, or Error
- **Last Seen**: Relative time display

**Device Controls:**
- Connect/Disconnect functionality
- View device details
- Status-based action availability
- Hover effects and interactive feedback

**Fake Data Generation:**
- 5 simulated devices with random MAC addresses
- Mixed device types (AIQ Core, AIQ Core Torque)
- Distributed across environments (dev/test/prod)
- Realistic status distribution and timestamps
