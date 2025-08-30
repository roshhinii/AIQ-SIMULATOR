package com.flender.dib.aiq.devices.simulator.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponseDTO {
    private String id;
    private String environment;
    private String status;
    private String type;
}
