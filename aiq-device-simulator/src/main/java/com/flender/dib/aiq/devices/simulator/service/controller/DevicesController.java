package com.flender.dib.aiq.devices.simulator.service.controller;

import com.flender.dib.aiq.devices.simulator.service.dto.CreateDeviceDTO;
import com.flender.dib.aiq.devices.simulator.service.dto.DeviceResponseDTO;
import com.flender.dib.aiq.devices.simulator.service.exception.DeviceStateException;
import com.flender.dib.aiq.devices.simulator.service.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
public class DevicesController {
    
    private final DeviceService deviceService;

    public DevicesController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponseDTO> getAllDevices() {
        return deviceService.getAllDevices();
    }
    
    @PostMapping
    public ResponseEntity<DeviceResponseDTO> createDevice(@RequestBody CreateDeviceDTO request) {
        DeviceResponseDTO createdDevice = deviceService.createDevice(request);
        return ResponseEntity.ok(createdDevice);
    }
    
    @PutMapping("/{deviceId}/action/start")
    public ResponseEntity<String> startDevice(@PathVariable String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new DeviceStateException("Device ID is required");
        }
        
        DeviceResponseDTO result = deviceService.startDevice(deviceId);
        return ResponseEntity.ok("Device " + deviceId + " start action initiated. Current status: " + result.getStatus().toLowerCase());
    }
    
    @PutMapping("/{deviceId}/action/stop")
    public ResponseEntity<String> stopDevice(@PathVariable String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new DeviceStateException("Device ID is required");
        }
        
        DeviceResponseDTO result = deviceService.stopDevice(deviceId);
        return ResponseEntity.ok("Device " + deviceId + " stop action initiated. Current status: " + result.getStatus().toLowerCase());
    }
}
