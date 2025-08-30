package com.flender.dib.aiq.devices.simulator.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeviceDTO {
    private String id;
    private String environment;
    private String privateKey;
    private String certificate;
}
