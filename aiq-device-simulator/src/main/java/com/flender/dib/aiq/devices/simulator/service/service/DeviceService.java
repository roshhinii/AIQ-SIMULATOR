package com.flender.dib.aiq.devices.simulator.service.service;

import com.flender.dib.aiq.devices.simulator.service.device.DeviceManager;
import com.flender.dib.aiq.devices.simulator.service.dto.CreateDeviceDTO;
import com.flender.dib.aiq.devices.simulator.service.dto.DeviceResponseDTO;
import com.flender.dib.aiq.devices.simulator.service.exception.DeviceStateException;
import com.flender.dib.aiq.devices.simulator.service.mapper.DeviceMapper;
import com.flender.dib.aiq.devices.simulator.service.model.Device;
import com.flender.dib.aiq.devices.simulator.service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Thread.startVirtualThread;

@Service
@RequiredArgsConstructor
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    
    // Device manager state - moved from DeviceManagerService
    private final ConcurrentHashMap<String, DeviceManager> deviceManagers = new ConcurrentHashMap<>();


    public List<DeviceResponseDTO> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        return deviceMapper.toResponseDTOList(devices);
    }
    
    public Optional<DeviceResponseDTO> getDeviceById(String id) {
        Optional<Device> device = deviceRepository.findById(id);
        return device.map(deviceMapper::toResponseDTO);
    }
    
    public DeviceResponseDTO createDevice(CreateDeviceDTO request) {
        // Validate required fields
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            throw new DeviceStateException("Device ID is required");
        }
        
        if (request.getEnvironment() == null || request.getEnvironment().trim().isEmpty()) {
            throw new DeviceStateException("Environment is required");
        }
        
        if (request.getPrivateKey() == null || request.getPrivateKey().trim().isEmpty()) {
            throw new DeviceStateException("Private key is required");
        }
        
        if (request.getCertificate() == null || request.getCertificate().trim().isEmpty()) {
            throw new DeviceStateException("Certificate is required");
        }
        
        // Check if device already exists
        if (deviceRepository.existsById(request.getId())) {
            throw new DeviceStateException("Device with ID '" + request.getId() + "' already exists");
        }
        
        // Parse environment
        Device.Environment environment;
        try {
            environment = Device.Environment.valueOf(request.getEnvironment().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DeviceStateException("Invalid environment value. Valid values are: DEV, TEST, PROD");
        }
        
        // Create new device with default values
        Device device = new Device(
            request.getId(),
            environment,
            Device.Status.STOPPED, // Default status
            Device.Type.AIQ_CORE,  // Default type
            request.getPrivateKey(),
            request.getCertificate()
        );
        
        Device savedDevice = deviceRepository.save(device);
        return deviceMapper.toResponseDTO(savedDevice);
    }
     public DeviceResponseDTO updateDeviceStatus(String deviceId, Device.Status newStatus) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            throw new DeviceStateException("Device with ID '" + deviceId + "' not found");
        }

        Device device = deviceOpt.get();
        Device.Status oldStatus = device.getStatus();
        
        // Validate state transitions
        if (!isValidStateTransition(oldStatus, newStatus)) {
            throw new DeviceStateException("Invalid state transition from " + oldStatus + " to " + newStatus);
        }
        
        device.setStatus(newStatus);
        Device savedDevice = deviceRepository.save(device);
        
        // Handle thread management based on new status
        handleThreadManagement(device, newStatus, oldStatus);
        
        return deviceMapper.toResponseDTO(savedDevice);
    }
    
    private boolean isValidStateTransition(Device.Status from, Device.Status to) {
        // Define valid state transitions
        switch (from) {
            case STOPPED:
                return to == Device.Status.STARTING || to == Device.Status.STOPPED;
            case STARTING:
                return to == Device.Status.CONNECTING || to == Device.Status.STOPPED || to == Device.Status.STARTING;
            case CONNECTING:
                return to == Device.Status.CONNECTED || to == Device.Status.STARTING || to == Device.Status.CONNECTING;
            case CONNECTED:
                return to == Device.Status.CONNECTING || to == Device.Status.STARTING || to == Device.Status.STOPPED;
            default:
                return false;
        }
    }
    
    private void handleThreadManagement(Device device, Device.Status newStatus, Device.Status oldStatus) {
        switch (newStatus) {
            case STARTING:
                // Start the device thread when entering STARTING state
                if (oldStatus == Device.Status.STOPPED) {
                    startManager(device);
                }
                break;
            case STOPPED:
                // Stop the device thread when entering STOPPED state
                if (oldStatus != Device.Status.STOPPED) {
                    stopManager(device.getId());
                }
                break;
            // CONNECTING and CONNECTED states keep the thread running
        }
    }
    
    public boolean isDeviceThreadRunning(String deviceId) {
        return isManagerRunning(deviceId);
    }
    
    // === Device Manager Functionality (merged from DeviceManagerService) ===
    
    /**
     * Starts a manager for the specified device if not already running
     * @param device The device
     * @return true if manager was started, false if already running
     */
    private boolean startManager(Device device) {
        String deviceId = device.getId();

        if (isManagerRunning(deviceId)) {
            logger.info("Manager for device {} is already running", deviceId);
            return false;
        }

        DeviceManager manager = new DeviceManager(
            this.deviceRepository,
            this::updateDeviceStatusInternal, // Callback delegate
            device
        );
        this.deviceManagers.put(deviceId, manager);

        // Start the manager in a virtual thread - it will manage its own lifecycle
        startVirtualThread(() -> {
            try {
                manager.run();
            } finally {
                // Clean up when the manager finishes
                onDeviceThreadFinished(deviceId);
            }
        }).setName("DeviceManager-" + deviceId);

        logger.info("Started manager for device {}", deviceId);
        return true;
    }

    /**
     * Stops the manager for the specified device
     * @param deviceId The device ID
     * @return true if manager was stopped, false if not running
     */
    private boolean stopManager(String deviceId) {
        DeviceManager manager = deviceManagers.get(deviceId);
        if (manager == null) {
            logger.info("No manager found for the given device {}", deviceId);
            return false;
        }

        // Signal the DeviceManager to stop gracefully - it will handle its own thread interruption
        manager.stop();
        deviceManagers.remove(deviceId);

        logger.info("Stopped manager for device {}", deviceId);
        return true;
    }

    /**
     * Checks if a manager is currently running for the specified device
     * @param deviceId The device ID
     * @return true if manager is running, false otherwise
     */
    private boolean isManagerRunning(String deviceId) {
        return deviceManagers.containsKey(deviceId);
    }

    /**
     * Gets the status of all device managers
     * @return Map of device IDs to their manager status
     */
    public ConcurrentMap<String, Boolean> getAllManagerStatuses() {
        ConcurrentHashMap<String, Boolean> statuses = new ConcurrentHashMap<>();
        deviceManagers.keySet().forEach(deviceId -> statuses.put(deviceId, true));
        return statuses;
    }

    /**
     * Callback method called when a device manager finishes execution
     * @param deviceId The device ID whose manager has finished
     */
    private void onDeviceThreadFinished(String deviceId) {
        logger.info("Device manager finished for device {}, cleaning up", deviceId);
        deviceManagers.remove(deviceId);
    }

    /**
     * Shuts down the service and stops all running managers
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down device service...");

        // Stop all device managers gracefully - they will handle their own thread interruption
        deviceManagers.forEach((deviceId, manager) -> {
            manager.stop();
            logger.info("Signaled device manager {} to stop", deviceId);
        });
        deviceManagers.clear();

        logger.info("Device service shut down");
    }
    
    /**
     * Initialize device states on application startup
     * Transitions devices that were not STOPPED back to STARTING state
     */
    @PostConstruct
    public void initializeDeviceStates() {
        logger.info("Initializing device states on startup...");

        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            if (device.getStatus() != Device.Status.STOPPED) {
                logger.info("Device {} was in state {}, transitioning to STARTING", device.getId(), device.getStatus());
                device.setStatus(Device.Status.STARTING);
                Device savedDevice = deviceRepository.save(device);

                // Start the thread for this device
                startManager(savedDevice);
            }
        }

        logger.info("Device state initialization completed");
    }
    
    /**
     * Start a device - transitions from STOPPED to STARTING if applicable
     * @param deviceId The device ID
     * @return The updated device response DTO
     */
    public DeviceResponseDTO startDevice(String deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            throw new DeviceStateException("Device with ID '" + deviceId + "' not found");
        }
        
        Device device = deviceOpt.get();
        Device.Status currentStatus = device.getStatus();
        
        if (currentStatus == Device.Status.STOPPED) {
            device.setStatus(Device.Status.STARTING);
            Device savedDevice = deviceRepository.save(device);
            
            // Start the device thread
            startManager(savedDevice);
            logger.info("Device {} started, transitioned from STOPPED to STARTING", deviceId);
            
            return deviceMapper.toResponseDTO(savedDevice);
        } else {
            logger.info("Device {} start action ignored, current status: {}", deviceId, currentStatus);
            return deviceMapper.toResponseDTO(device);
        }
    }
    
    /**
     * Stop a device - transitions to STOPPED and shuts down thread
     * @param deviceId The device ID
     * @return The updated device response DTO
     */
    public DeviceResponseDTO stopDevice(String deviceId) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            throw new DeviceStateException("Device with ID '" + deviceId + "' not found");
        }
        
        Device device = deviceOpt.get();
        Device.Status currentStatus = device.getStatus();
        
        if (currentStatus != Device.Status.STOPPED) {
            device.setStatus(Device.Status.STOPPED);
            Device savedDevice = deviceRepository.save(device);
            
            // Stop the device thread
            stopManager(deviceId);
            logger.info("Device {} stopped, transitioned from {} to STOPPED", deviceId, currentStatus);
            
            return deviceMapper.toResponseDTO(savedDevice);
        } else {
            logger.info("Device {} stop action ignored, already stopped", deviceId);
            return deviceMapper.toResponseDTO(device);
        }
    }
    
    /**
     * Update device status directly in the database (internal use)
     * This method bypasses business logic validation and is used by internal components
     * @param deviceId The device ID
     * @param newStatus The new status to set
     */
    public void updateDeviceStatusInternal(String deviceId, Device.Status newStatus) {
        try {
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                device.setStatus(newStatus);
                deviceRepository.save(device);
                logger.debug("Updated device {} status to {}", deviceId, newStatus);
            } else {
                logger.warn("Device {} not found when trying to update status", deviceId);
            }
        } catch (Exception e) {
            logger.error("Failed to update device {} status to {}: {}", deviceId, newStatus, e.getMessage());
        }
    }

    /**
     * Update device status directly in the database using a Device entity (internal use)
     * @param device The device entity
     * @param newStatus The new status to set
     */
    public void updateDeviceStatusInternal(Device device, Device.Status newStatus) {
        updateDeviceStatusInternal(device.getId(), newStatus);
    }
}
