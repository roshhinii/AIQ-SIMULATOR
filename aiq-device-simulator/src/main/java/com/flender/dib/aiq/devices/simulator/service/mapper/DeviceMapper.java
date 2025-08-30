package com.flender.dib.aiq.devices.simulator.service.mapper;

import com.flender.dib.aiq.devices.simulator.service.dto.DeviceResponseDTO;
import com.flender.dib.aiq.devices.simulator.service.model.Device;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeviceMapper {
    
    public DeviceResponseDTO toResponseDTO(Device device) {
        if (device == null) {
            return null;
        }
        
        return new DeviceResponseDTO(
            device.getId(),
            device.getEnvironment().name(),
            device.getStatus().name(),
            device.getType().name()
        );
    }
    
    public List<DeviceResponseDTO> toResponseDTOList(List<Device> devices) {
        return devices.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
