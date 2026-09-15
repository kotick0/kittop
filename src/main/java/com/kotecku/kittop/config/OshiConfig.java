package com.kotecku.kittop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.Sensors;
import oshi.software.os.OperatingSystem;

@Configuration
public class OshiConfig {

    @Bean
    public SystemInfo systemInfo() {
        return new SystemInfo();
    }

    @Bean
    public OperatingSystem operatingSystem(SystemInfo systemInfo) {
        return systemInfo.getOperatingSystem();
    }

    @Bean
    public CentralProcessor centralProcessor(SystemInfo systemInfo) {
        return systemInfo.getHardware().getProcessor();
    }

    @Bean
    public Sensors sensors(SystemInfo systemInfo) {
        return systemInfo.getHardware().getSensors();
    }

    @Bean
    public GlobalMemory globalMemory(SystemInfo systemInfo) {
        return systemInfo.getHardware().getMemory();
    }
}

