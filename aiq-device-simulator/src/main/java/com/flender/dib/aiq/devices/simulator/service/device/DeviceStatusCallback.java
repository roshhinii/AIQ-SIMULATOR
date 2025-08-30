package com.flender.dib.aiq.devices.simulator.service.device;

import com.flender.dib.aiq.devices.simulator.service.model.Device;

/**
 * Callback interface for device status updates.
 * Allows decoupling of device status management from the DeviceManager.
 */
public interface DeviceStatusCallback {
    
    /**
     * Called when a device's status changes
     * @param device The device whose status changed
     * @param newStatus The new status
     */
    void onDeviceStatusChanged(Device device, Device.Status newStatus);
}
