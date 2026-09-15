package com.kotecku.kittop.memory;

public record MemorySnapshot(
        long totalMemoryBytes,
        long availableMemoryBytes,
        long freeMemoryBytes,
        long cachedMemoryBytes,
        long usedMemoryBytes
) {
}
