package com.kotecku.kittop.cpu;

import com.kotecku.kittop.OnMacOsCondition;
import com.kotecku.kittop.exceptions.CpuInfoException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
@Conditional(OnMacOsCondition.class)
public class MacCpuInfoProvider implements CpuInfoProvider {

    private final CentralProcessor centralProcessor;

    private static final Pattern PMU2_TDIE_PATTERN = Pattern.compile("^PMU2 tdie\\d+$");
    private static final String RESOURCE_PATH = "/native/macos-arm64/cputemp";

    private Path extractedBinary;

    private record CpuLoad(double percent, double[] perCore) {
    }

    @PostConstruct
    public void extractBinary() {
        if (!System.getProperty("os.arch").contains("aarch64") || !System.getProperty("os.name").contains("Mac")) {
            return;
        }
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

            boolean quarantineRemoved = new ProcessBuilder("xattr", "-d", "com.apple.quarantine", extractedBinary.toString())
                    .start()
                    .waitFor(3, TimeUnit.SECONDS);
            if (!quarantineRemoved) {
                log.warn("xattr did not finish in time while removing quarantine flag from {}", extractedBinary);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(extractedBinary);
                } catch (IOException e) {
                    log.warn("Failed to delete temp binary: {}", extractedBinary, e);
                }
            }));

            log.info("cputemp binary extracted to {}", extractedBinary);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to extract cputemp binary", e);
            extractedBinary = null;
        }
    }

    private double[] getCpuTemperaturePerCore() {
        if (extractedBinary == null) {
            log.warn("cputemp binary not available");
            return new double[0];
        }

        try {
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
                        log.warn("Could not parse temperature value on line: {}", line, e);
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
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to read CPU temperatures", e);
            return new double[0];
        }
    }

    private CpuLoad measureCpuLoad() {
        long[] prevSystemTicks = centralProcessor.getSystemCpuLoadTicks();
        long[][] prevPerCoreTicks = centralProcessor.getProcessorCpuLoadTicks();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CpuInfoException("Interrupted while sampling CPU load", e);
        }

        double percent = centralProcessor.getSystemCpuLoadBetweenTicks(prevSystemTicks) * 100;

        double[] perCore = centralProcessor.getProcessorCpuLoadBetweenTicks(prevPerCoreTicks);
        for (int i = 0; i < perCore.length; i++) {
            perCore[i] *= 100;
        }

        return new CpuLoad(percent, perCore);
    }

    @Override
    public CpuSnapshot getCpuSnapshot() {
        CpuLoad cpuLoad = measureCpuLoad();
        double[] cpuTemperaturePerCore = getCpuTemperaturePerCore();
        return new CpuSnapshot(
                cpuLoad.perCore(),
                cpuTemperaturePerCore,
                cpuLoad.percent(),
                Arrays.stream(cpuTemperaturePerCore).max().orElse(0.0)
        );
    }
}