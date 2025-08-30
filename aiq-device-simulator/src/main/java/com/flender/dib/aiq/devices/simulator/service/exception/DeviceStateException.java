package com.flender.dib.aiq.devices.simulator.service.exception;

public class DeviceStateException extends RuntimeException {
    
    public DeviceStateException(String message) {
        super(message);
    }
    
    public DeviceStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
