package com.kotecku.javaresourcemonitor.memory;

public record MemorySnapshot(
        long totalMemoryBytes,
        long availableMemoryBytes,
        long freeMemoryBytes,
        long cachedMemoryBytes,
        long usedMemoryBytes
) {
}
