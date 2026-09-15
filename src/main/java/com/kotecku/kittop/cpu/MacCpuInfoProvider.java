package com.kotecku.kittop.cpu;

import com.kotecku.kittop.OnMacOsCondition;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import oshi.hardware.CentralProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Conditional(OnMacOsCondition.class)
public class MacCpuInfoProvider implements CpuInfoProvider {

    private final CentralProcessor centralProcessor;

    private static final Logger log = LoggerFactory.getLogger(MacCpuInfoProvider.class);
    private static final Pattern PMU2_TDIE_PATTERN = Pattern.compile("^PMU2 tdie\\d+$");
    private static final String RESOURCE_PATH = "/native/macos-arm64/cputemp";

    private Path extractedBinary;

    @PostConstruct
    public void extractBinary() {
        if (System.getProperty("os.arch").contains("aarch64") && System.getProperty("os.name").contains("Mac")) {
            try (InputStream in = getClass().getResourceAsStream(RESOURCE_PATH)) {
                if (in == null) {
                    log.warn("cputemp binary not found in classpath at {}", RESOURCE_PATH);
                    return;
                }
                extractedBinary = Files.createTempFile("cputemp", "");
                Files.copy(in, extractedBinary, StandardCopyOption.REPLACE_EXISTING);
                if (!extractedBinary.toFile().setExecutable(true)) {
                    throw new IOException("Couldn't set the right permissions for file: " + extractedBinary);
                }

                new ProcessBuilder("xattr", "-d", "com.apple.quarantine", extractedBinary.toString())
                        .start()
                        .waitFor(3, TimeUnit.SECONDS);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Files.deleteIfExists(extractedBinary);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp binary: {}", e.getMessage());
                    }
                }));

                log.info("cputemp binary extracted to {}", extractedBinary);
            } catch (Exception e) {
                log.warn("Failed to extract cputemp binary: {}", e.getMessage());
            }
        }
    }

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
        try {
            if (extractedBinary == null) {
                log.warn("cputemp binary not available");
                return new double[0];
            }

            Process process = new ProcessBuilder(extractedBinary.toString())
                    .redirectErrorStream(false)
                    .start();

            Map<Integer, Double> coreMap = new TreeMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(": ");
                    if (parts.length != 2) continue;

                    String name = parts[0].trim();
                    if (!PMU2_TDIE_PATTERN.matcher(name).matches()) continue;

                    try {
                        double value = Double.parseDouble(parts[1].trim());
                        if (value < 0.0 || value > 150.0) continue;

                        int coreIndex = Integer.parseInt(name.replaceAll("\\D+", "")) - 1;
                        coreMap.put(coreIndex, value);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse temperature value on line: {}", line);
                    }
                }
            }

            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("cputemp process timed out");
                return new double[0];
            }

            if (coreMap.isEmpty()) {
                log.warn("No valid PMU2 tdie sensor values found");
                return new double[0];
            }

            return coreMap.values().stream().mapToDouble(Double::doubleValue).toArray();
        } catch (Exception e) {
            log.warn("Failed to read CPU temperatures: {}", e.getMessage());
            return new double[0];
        }
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
        double[] coreTemps = getCpuTemperaturePerCore();
        return java.util.Arrays.stream(coreTemps).max().orElse(0.0);
    }
}
