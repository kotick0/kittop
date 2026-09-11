package com.kotecku.javaresourcemonitor.memory;

import com.kotecku.javaresourcemonitor.OnLinuxCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@Component
@Conditional(OnLinuxCondition.class)
public class LinuxMemoryInfoProvider implements MemoryInfoProvider {
    private static final Path MEMINFOPATH = Path.of("/proc/meminfo");

    @Override
    public MemorySnapshot getMemorySnapshot() {
        HashMap<String, Long> memoryInfo = extractDataFromProcMeminfo();
        return new MemorySnapshot(
                memoryInfo.get("MemTotal"),
                memoryInfo.get("MemAvailable"),
                memoryInfo.get("MemFree"),
                memoryInfo.get("Cached"),
                memoryInfo.get("MemUsed")
        );
    }

    private HashMap<String, Long> extractDataFromProcMeminfo() {
        try {
            String memInfoContent = Files.readString(MEMINFOPATH);
            HashMap<String, Long> memoryInfo = new HashMap<>();
            memInfoContent.lines()
                    .filter(line -> line.startsWith("MemTotal:") ||
                            line.startsWith("MemAvailable:") ||
                            line.startsWith("MemFree:") ||
                            line.startsWith("Cached:"))
                    .map(line -> line.split(":"))
                    .forEach(parts -> {
                        String key = parts[0].trim();
                        long value = Long.parseLong(parts[1].replace("kB", "").trim());
                        memoryInfo.put(key, value * 1024);
                        });
            memoryInfo.put("MemUsed", memoryInfo.get("MemTotal") - memoryInfo.get("MemAvailable"));
            return memoryInfo;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read /proc/meminfo: ", e);
        }
    }

}
