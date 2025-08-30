package com.flender.dib.aiq.devices.simulator.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device {
    
    public enum Environment {
        DEV("dev"),
        TEST("test"),
        PROD("prod");
        
        private final String value;
        
        Environment(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum Status {
        STOPPED("stopped"),
        STARTING("starting"),
        CONNECTING("connecting"),
        CONNECTED("connected");
        
        private final String value;
        
        Status(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum Type {
        AIQ_CORE("AIQ Core"),
        AIQ_CORE_TORQUE("AIQ Core Torque");
        
        private final String value;
        
        Type(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    private Environment environment;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Enumerated(EnumType.STRING)
    private Type type;
    
    @Column(length = 4000)
    private String privateKey;
    
    @Column(length = 4000)
    private String certificate;
}
