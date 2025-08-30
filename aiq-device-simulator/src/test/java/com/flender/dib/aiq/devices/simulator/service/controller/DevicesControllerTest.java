package com.flender.dib.aiq.devices.simulator.service.controller;

import com.flender.dib.aiq.devices.simulator.service.dto.DeviceResponseDTO;
import com.flender.dib.aiq.devices.simulator.service.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DevicesController.class)
class DevicesControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DeviceService deviceService;

    @Test
    void getAllDevices_ShouldReturnListOfDevices() throws Exception {
        List<DeviceResponseDTO> mockDevices = Arrays.asList(
            new DeviceResponseDTO("AA:BB:CC:DD:EE:01", "DEV", "CONNECTED", "AIQ_CORE"),
            new DeviceResponseDTO("AA:BB:CC:DD:EE:02", "TEST", "STOPPED", "AIQ_CORE_TORQUE")
        );
        
        when(deviceService.getAllDevices()).thenReturn(mockDevices);
        
        mockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("AA:BB:CC:DD:EE:01"))
                .andExpect(jsonPath("$[0].environment").value("DEV"))
                .andExpect(jsonPath("$[0].status").value("CONNECTED"))
                .andExpect(jsonPath("$[0].type").value("AIQ_CORE"))
                .andExpect(jsonPath("$[1].id").value("AA:BB:CC:DD:EE:02"))
                .andExpect(jsonPath("$[1].environment").value("TEST"))
                .andExpect(jsonPath("$[1].status").value("STOPPED"))
                .andExpect(jsonPath("$[1].type").value("AIQ_CORE_TORQUE"));
    }

    @Test
    void createDevice_ShouldCreateNewDevice() throws Exception {
        DeviceResponseDTO mockDevice = new DeviceResponseDTO("AA:BB:CC:DD:EE:03", "PROD", "STOPPED", "AIQ_CORE");
        
        when(deviceService.createDevice(any())).thenReturn(mockDevice);
        
        String requestBody = """
        {
            "id": "AA:BB:CC:DD:EE:03",
            "environment": "prod",
            "privateKey": "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7...\\n-----END PRIVATE KEY-----",
            "certificate": "-----BEGIN CERTIFICATE-----\\nMIIDXTCCAkWgAwIBAgIJAL7Z7Z7Z7Z7ZMA0GCSqGSIb3DQEBCwUA...\\n-----END CERTIFICATE-----"
        }
        """;
        
        mockMvc.perform(post("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("AA:BB:CC:DD:EE:03"))
                .andExpect(jsonPath("$.environment").value("PROD"))
                .andExpect(jsonPath("$.status").value("STOPPED"))
                .andExpect(jsonPath("$.type").value("AIQ_CORE"));
    }

    @Test
    void startDevice_ShouldStartStoppedDevice() throws Exception {
        DeviceResponseDTO mockDevice = new DeviceResponseDTO("AA:BB:CC:DD:EE:01", "DEV", "STARTING", "AIQ_CORE");
        
        when(deviceService.startDevice("AA:BB:CC:DD:EE:01")).thenReturn(mockDevice);
        
        mockMvc.perform(put("/devices/AA:BB:CC:DD:EE:01/action/start"))
                .andExpect(status().isOk())
                .andExpect(content().string("Device AA:BB:CC:DD:EE:01 start action initiated. Current status: starting"));
    }
    
    @Test
    void startDevice_ShouldIgnoreIfNotStopped() throws Exception {
        DeviceResponseDTO mockDevice = new DeviceResponseDTO("AA:BB:CC:DD:EE:01", "DEV", "CONNECTED", "AIQ_CORE");
        
        when(deviceService.startDevice("AA:BB:CC:DD:EE:01")).thenReturn(mockDevice);
        
        mockMvc.perform(put("/devices/AA:BB:CC:DD:EE:01/action/start"))
                .andExpect(status().isOk())
                .andExpect(content().string("Device AA:BB:CC:DD:EE:01 start action initiated. Current status: connected"));
    }
    
    @Test
    void stopDevice_ShouldStopRunningDevice() throws Exception {
        DeviceResponseDTO mockDevice = new DeviceResponseDTO("AA:BB:CC:DD:EE:01", "DEV", "STOPPED", "AIQ_CORE");
        
        when(deviceService.stopDevice("AA:BB:CC:DD:EE:01")).thenReturn(mockDevice);
        
        mockMvc.perform(put("/devices/AA:BB:CC:DD:EE:01/action/stop"))
                .andExpect(status().isOk())
                .andExpect(content().string("Device AA:BB:CC:DD:EE:01 stop action initiated. Current status: stopped"));
    }
    
    @Test
    void stopDevice_ShouldIgnoreIfAlreadyStopped() throws Exception {
        DeviceResponseDTO mockDevice = new DeviceResponseDTO("AA:BB:CC:DD:EE:01", "DEV", "STOPPED", "AIQ_CORE");
        
        when(deviceService.stopDevice("AA:BB:CC:DD:EE:01")).thenReturn(mockDevice);
        
        mockMvc.perform(put("/devices/AA:BB:CC:DD:EE:01/action/stop"))
                .andExpect(status().isOk())
                .andExpect(content().string("Device AA:BB:CC:DD:EE:01 stop action initiated. Current status: stopped"));
    }

    @Test
    void startDevice_ShouldReturnBadRequestForEmptyDeviceId() throws Exception {
        mockMvc.perform(put("/devices/ /action/start"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void stopDevice_ShouldReturnBadRequestForEmptyDeviceId() throws Exception {
        mockMvc.perform(put("/devices/ /action/stop"))
                .andExpect(status().isBadRequest());
    }
}
