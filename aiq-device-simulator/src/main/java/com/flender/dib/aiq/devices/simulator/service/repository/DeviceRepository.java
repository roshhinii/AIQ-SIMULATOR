package com.flender.dib.aiq.devices.simulator.service.repository;

import com.flender.dib.aiq.devices.simulator.service.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
}
