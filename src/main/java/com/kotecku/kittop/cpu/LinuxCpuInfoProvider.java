package com.kotecku.kittop.cpu;

import com.kotecku.kittop.OnLinuxCondition;
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

    @Override
    public double[] getCpuLoadPerCore() {
        long[][] prevTicks = centralProcessor.getProcessorCpuLoadTicks();
        try {
            TimeUnit.SECONDS.sleep(1);
            double[] loadPerCore = centralProcessor.getProcessorCpuLoadBetweenTicks(prevTicks);
            for (int i = 0; i < loadPerCore.length; i++) {
                loadPerCore[i] *= 100;
            }
            return loadPerCore;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double[] getCpuTemperaturePerCore() {
        LinkedHashMap<String, Double> cores = new LinkedHashMap<>();
        try (DirectoryStream<Path> hwmons = Files.newDirectoryStream(Paths.get("/sys/class/hwmon"), "hwmon*")) {
            for (Path hwmon : hwmons) {
                String driverName = Files.readString(hwmon.resolve("name"));
                if (TEMP_DRIVERS.stream().anyMatch(driverName::contains)) {
                    try (DirectoryStream<Path> labels = Files.newDirectoryStream(hwmon, "temp*_label")) {
                        try {
                            for (Path label : labels) {
                                String coreName = Files.readString(label).trim();
                                if (!coreName.contains("Package")) {
                                    Path input = hwmon.resolve(label.getFileName().toString().replace("_label", "_input"));
                                    double celsius = Long.parseLong(Files.readString(input).trim()) / 1000.0;
                                    cores.putIfAbsent(coreName, celsius);
                                }
                            }
                        } catch (IOException e) {
                            log.warn(e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cores.values().stream().mapToDouble(Double::doubleValue).toArray();
    }

    @Override
    public double getCpuLoadPercent() {
        long[] prevTicks = centralProcessor.getSystemCpuLoadTicks();
        try {
            TimeUnit.SECONDS.sleep(1);
            return centralProcessor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getCpuTemperatureMax() {
        return sensors.getCpuTemperature();
    }
}
