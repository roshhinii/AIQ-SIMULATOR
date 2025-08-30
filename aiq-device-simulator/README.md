# Confluence Documentation 
https://flender-group.atlassian.net/wiki/spaces/DB/pages/1014661160/AIQ+Device+Emulator

# Device Simulation Service

A Spring Boot-based IoT device simulation service that manages the lifecycle of simulated devices connecting to Azure IoT Hub. This service provides device provisioning, connection management, telemetry simulation, and status tracking capabilities.

## Overview

This service simulates IoT devices by:
- Provisioning devices with Azure Device Provisioning Service (DPS)
- Managing device connections and reconnections
- Simulating device telemetry and operational cycles
- Tracking device status throughout their lifecycle
- Providing REST APIs for device management

## Architecture

### Package Structure

```
com.flender.dib.aiq.devices.simulator.service/
├── config/                     # Spring configuration classes
│   └── ApplicationShutdownHook.java
├── controller/                 # REST API controllers
├── device/                     # Device management core (NEW)
│   ├── DeviceManager.java     # Individual device lifecycle manager
│   ├── DeviceWorker.java      # Device work simulation logic
│   └── DeviceStatusCallback.java  # Status update callback interface
├── model/                      # Data models and entities
│   └── Device.java            # Device entity
├── repository/                 # Data access layer
│   └── DeviceRepository.java  # Device data repository
└── service/                    # Business logic services
    ├── DeviceService.java     # Device business logic and manager orchestration
    └── DeviceProvisioningService.java  # Azure DPS integration
```

### Key Components

#### Device Package (`device/`)
- **DeviceManager**: Manages individual device lifecycle (connection, reconnection, worker management)
- **DeviceWorker**: Executes device work cycles (telemetry, health checks, configuration updates)
- **DeviceStatusCallback**: Callback interface for decoupled status updates

#### Service Package (`service/`)
- **DeviceService**: High-level device business logic, status management, and device manager orchestration
- **DeviceProvisioningService**: Handles Azure DPS provisioning and certificate management

## Device Lifecycle

### Status Flow
```
STARTING → CONNECTING → CONNECTED → [STOPPED]
    ↑           ↓
    └─── (auto-restart) ───┘
```

### Thread Architecture
Each device runs in its own set of virtual threads:
- **Manager Thread**: Handles connection lifecycle and worker supervision
- **Worker Thread**: Performs device work cycles (telemetry, monitoring)

### Auto-Restart Mechanism
- Workers automatically restart if they finish unexpectedly
- Managers handle connection failures with retry logic
- Clean shutdown prevents unwanted restarts

## Configuration

### Profile Configuration

The service uses Spring profiles to manage different environments:

- **Production Profile (`prod`)**: Default profile optimized for production use
  - Minimal resource usage
  - Fast startup times
  - Reduced logging overhead
  - File-based H2 database

- **Development Profile (`dev`)**: Enhanced development features
  - Verbose logging for debugging
  - H2 console enabled
  - Detailed error messages
  - Optional in-memory database

### Azure IoT Configuration

The service connects to Azure IoT Hub through Device Provisioning Service (DPS):

```java
// DeviceProvisioningService.java
private static final String ID_SCOPE = "0ne006D6377";
private static final String GLOBAL_ENDPOINT = "global.azure-devices-provisioning.net";
```

**Note**: Update these values for your Azure IoT environment.

### Device Authentication

Devices authenticate using X.509 certificates:
- Each device requires a certificate and private key
- Certificates are stored Base64-encoded in the device entity
- The service handles certificate parsing and provisioning

## API Usage

### Starting a Device
```http
POST /api/devices/{deviceId}/start
```

### Stopping a Device
```http
POST /api/devices/{deviceId}/stop
```

### Getting Device Status
```http
GET /api/devices/{deviceId}/status
```

## Development Setup

### Prerequisites
- Java 21+
- Maven 3.8+
- Spring Boot 3.x
- Azure IoT SDK dependencies

### Dependencies
Key dependencies include:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `azure-iot-device-client`
- `azure-iot-provisioning-device-client`
- `lombok`

### Running the Service

#### Production Mode (Default)
```bash
mvn spring-boot:run
```

The default configuration runs with the `prod` profile and is optimized for fast startup with single-user scenarios.

#### Development Mode
For development with enhanced logging and debugging features:
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

#### Further JVM Optimizations
For even faster startup, you can add JVM flags:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx512m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
```

#### Expected Startup Time
- **Standard mode**: ~5-10 seconds (already optimized)
- **With JVM flags**: ~3-7 seconds
- **Native image**: ~1-2 seconds (requires GraalVM)

### Build Optimizations

For even faster startup, consider:

1. **JVM Flags** (recommended):
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms128m -Xmx512m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
```

2. **Native Image** (GraalVM):
```bash
mvn clean package -Pnative
# Results in ~1-2 second startup time
```

## Design Patterns

### Callback Pattern
The service uses callbacks to decouple device status updates:
```java
DeviceManager manager = new DeviceManager(
    deviceRepository,
    (device, status) -> deviceService.updateDeviceStatusInternal(device, status),
    device
);
```

### Thread Self-Management
Threads handle their own lifecycle:
- No external thread references stored
- Cooperative shutdown via flags and interruption
- Clean resource cleanup in finally blocks

### Virtual Threads
Utilizes Java virtual threads for scalability:
```java
Thread workerThread = startVirtualThread(deviceWorker);
```

## Monitoring and Logging

### Status Tracking
- Device status is centrally managed through `DeviceService`
- Status changes are logged and can be monitored
- Real-time status available via REST API

### Logging Levels
- **INFO**: Device lifecycle events, status changes
- **DEBUG**: Detailed connection and work cycle information
- **WARN**: Recoverable errors, retry attempts
- **ERROR**: Fatal errors, connection failures

### Key Log Patterns
```
Device {deviceId} transitioned to {status}
Worker thread finished for device {deviceId}, restarting...
DeviceManager stopped for device {deviceId}
```

## Thread Safety

### Concurrent Design
- Thread-safe collections (`ConcurrentHashMap`)
- Volatile flags for thread coordination
- Synchronized blocks for critical sections
- Lock-based signaling between manager and worker

### Shutdown Coordination
```java
// Graceful shutdown pattern
shouldStop = true;  // Signal stop
worker.stop();      // Stop worker
synchronized (lock) { lock.notify(); }  // Wake manager
```

## Error Handling

### Connection Errors
- Automatic retry with exponential backoff
- Credential validation
- Device registration verification

### Worker Errors
- Fatal errors trigger worker restart
- Operational errors include recovery logic
- Resource cleanup in finally blocks

### Provisioning Errors
- Certificate parsing errors
- DPS registration failures
- IoT Hub connection issues

## Extending the Service

### Adding New Device Types
1. Extend the `Device` model with type-specific fields
2. Modify `DeviceWorker.performDeviceTasks()` for type-specific logic
3. Update provisioning if different certificate formats needed

### Custom Status Callbacks
Implement `DeviceStatusCallback` for custom status handling:
```java
DeviceStatusCallback customCallback = (device, status) -> {
    // Custom status handling logic
    customStatusHandler.handle(device, status);
};
```

### Additional Work Tasks
Extend `DeviceWorker.doWork()` method:
```java
private void doWork() {
    // Existing tasks...
    performCustomTasks();
}
```

## Troubleshooting

### Common Issues

1. **Device won't start**: Check certificate format and DPS configuration
2. **Connection timeouts**: Verify network connectivity to Azure
3. **Worker stops unexpectedly**: Check logs for exceptions in work cycle
4. **Memory leaks**: Ensure proper thread cleanup on shutdown

### Debug Steps
1. Enable DEBUG logging for detailed lifecycle information
2. Check device status via REST API
3. Verify Azure IoT Hub device registration
4. Test certificate parsing independently

## Performance Considerations

### Startup Time Optimization

The application is pre-configured with optimizations for single-user, sporadic usage:

#### Built-in Optimizations
- **Minimal thread pools**: Tomcat threads limited to 1-5
- **Small connection pools**: Database connections limited to 1-2  
- **Lazy initialization**: Components loaded only when needed
- **Reduced logging**: Framework logging set to WARN level
- **JMX disabled**: Faster startup without management overhead

#### Configuration
All optimizations are in the default `application.properties`:
```properties
# Server optimizations
server.tomcat.threads.max=5
server.tomcat.max-connections=10

# Spring optimizations  
spring.main.lazy-initialization=true
spring.jmx.enabled=false

# Database optimizations
spring.datasource.hikari.maximum-pool-size=2
spring.data.jpa.repositories.bootstrap-mode=lazy
```

#### Development Profile
The `dev` profile adds development-specific features:
- Enhanced logging for debugging (DEBUG level for application classes)
- Detailed error messages and stack traces
- H2 console enabled for database inspection
- Option for in-memory database (commented out by default)

To use the development profile:
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

**Note**: CORS is enabled for all profiles to allow cross-origin requests from web applications.

### Scalability
- Virtual threads enable high device concurrency
- Each device runs independently
- Resource usage scales linearly with device count

### Memory Management
- Devices clean up resources on shutdown
- No external thread references prevent memory leaks
- Connection pooling handled by Azure SDK

### Monitoring Recommendations
- Monitor active thread count
- Track device status distribution
- Monitor connection success rates
- Watch for error patterns in logs

## Contributing

When modifying the service:
1. Maintain thread safety patterns
2. Follow the callback pattern for status updates
3. Ensure proper resource cleanup
4. Add appropriate logging
5. Update this README for significant changes

## License

[Add your license information here]
