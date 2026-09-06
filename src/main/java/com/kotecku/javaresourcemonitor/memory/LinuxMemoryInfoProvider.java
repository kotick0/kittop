package com.kotecku.javaresourcemonitor.memory;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LinuxMemoryInfoProvider implements MemoryInfoProvider {

    @Override
    public long getTotalMemoryBytes() {
        return memoryReader("MemTotal");
    }

    @Override
    public long getAvailableMemoryBytes() {
        return memoryReader("MemAvailable");
    }

    @Override
    public long getFreeMemoryBytes() {
        return memoryReader("MemFree");
    }

    @Override
    public long getCachedMemoryBytes() {
        return memoryReader("Cached");
    }

    @Override
    public long getUsedMemoryBytes() {
        return getTotalMemoryBytes() - getAvailableMemoryBytes();
    }

    private long memoryReader(String key) {
        return readMemoryInfoFromProcMeminfo().lines()
                .filter(line -> line.startsWith(key + ":"))
                .findFirst()
                .map(line -> line.split(":")[1].replace("kB", "").trim())
                .map(Long::parseLong)
                .orElseThrow(() -> new IllegalStateException("No " + key + " line found in /proc/meminfo")) * 1024;
    }

    private String readMemoryInfoFromProcMeminfo() {
        Path meminfoPath = Path.of("/proc/meminfo");
        try {
            return Files.readString(meminfoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
