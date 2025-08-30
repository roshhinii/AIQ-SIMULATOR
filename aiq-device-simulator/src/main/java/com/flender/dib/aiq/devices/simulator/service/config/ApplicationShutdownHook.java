package com.flender.dib.aiq.devices.simulator.service.config;

import com.flender.dib.aiq.devices.simulator.service.service.DeviceService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class ApplicationShutdownHook {
    
    private final DeviceService deviceService;

    public ApplicationShutdownHook(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PreDestroy
    public void onShutdown() {
        deviceService.shutdown();
    }
}
