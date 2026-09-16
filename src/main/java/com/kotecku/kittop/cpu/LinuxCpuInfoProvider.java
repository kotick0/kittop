package com.kotecku.kittop.cpu;

import com.kotecku.kittop.OnLinuxCondition;
import com.kotecku.kittop.exceptions.CpuInfoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import oshi.hardware.CentralProcessor;
import oshi.hardware.Sensors;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Conditional(OnLinuxCondition.class)
public class LinuxCpuInfoProvider implements CpuInfoProvider {

    private final CentralProcessor centralProcessor;
    private final Sensors sensors;

    private static final Set<String> TEMP_DRIVERS = Set.of("coretemp", "k10temp", "k8temp", "zenpower");

    private double[] getCpuTemperaturePerCore() {
        LinkedHashMap<String, Double> cores = new LinkedHashMap<>();
        try (DirectoryStream<Path> hwmons = Files.newDirectoryStream(Paths.get("/sys/class/hwmon"), "hwmon*")) {
            for (Path hwmon : hwmons) {
                String driverName = Files.readString(hwmon.resolve("name")).trim();
                if (TEMP_DRIVERS.stream().anyMatch(driverName::contains)) {
                    try (DirectoryStream<Path> labels = Files.newDirectoryStream(hwmon, "temp*_label")) {
                        for (Path label : labels) {
                            try {
                                String coreName = Files.readString(label).trim();
                                if (!coreName.contains("Package")) {
                                    Path input = hwmon.resolve(label.getFileName().toString().replace("_label", "_input"));
                                    double celsius = Long.parseLong(Files.readString(input).trim()) / 1000.0;
                                    cores.putIfAbsent(coreName, celsius);
                                }
                            } catch (IOException | NumberFormatException e) {
                                log.warn("Skipping hwmon sensor {} in {}", label, hwmon, e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new CpuInfoException("Failed to read CPU temperature sensors from /sys/class/hwmon", e);
        }
        log.debug("Read temperature for {} CPU cores", cores.size());
        return cores.values().stream().mapToDouble(Double::doubleValue).toArray();
    }

    private double getCpuTemperatureMax() {
        return sensors.getCpuTemperature();
    }

    @Override
    public CpuSnapshot getCpuSnapshot() {
        long[][] prevTicksPerCore = centralProcessor.getProcessorCpuLoadTicks();
        long[] prevTicksSystem = centralProcessor.getSystemCpuLoadTicks();

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CpuInfoException("Interrupted while sampling CPU load", e);
        }

        double[] loadPerCore = centralProcessor.getProcessorCpuLoadBetweenTicks(prevTicksPerCore);
        for (int i = 0; i < loadPerCore.length; i++) {
            loadPerCore[i] *= 100;
        }
        double loadPercent = centralProcessor.getSystemCpuLoadBetweenTicks(prevTicksSystem) * 100;

        return new CpuSnapshot(loadPerCore, getCpuTemperaturePerCore(), loadPercent, getCpuTemperatureMax());
    }
}