package com.kotecku.kittop.memory;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import oshi.SystemInfo;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
abstract class MemoryInfoProviderContractTest {
    private final SystemInfo systemInfo = new SystemInfo();

    protected final long TOTAL_MEMORY_BYTES = systemInfo.getHardware().getMemory().getTotal();

    protected abstract MemoryInfoProvider provider();

    @Test
    void getTotalMemoryBytesValueShouldNotBeNull() {
        assertThat(provider().getMemorySnapshot().totalMemoryBytes()).isNotNull();
    }

    @Test
    void getTotalMemoryBytesValueShouldEqualTotalMemory() {
        assertThat(provider().getMemorySnapshot().totalMemoryBytes()).isEqualTo(TOTAL_MEMORY_BYTES);
    }

    @Test
    void getAvailableMemoryBytesValueShouldNotBeNull() {
        assertThat(provider().getMemorySnapshot().availableMemoryBytes()).isNotNull();
    }

    @Test
    void getAvailableMemoryBytesValueShouldNotBeNegative() {
        assertThat(provider().getMemorySnapshot().availableMemoryBytes()).isNotNegative();
    }

    @Test
    void getFreeMemoryBytesValueShouldNotBeNull() {
        assertThat(provider().getMemorySnapshot().freeMemoryBytes()).isNotNull();
    }

    @Test
    void getFreeMemoryBytesShouldNotBeNegative() {
        assertThat(provider().getMemorySnapshot().freeMemoryBytes()).isNotNegative();
    }

    @Test
    void getCachedMemoryBytesValueShouldNotBeNull() {
        assertThat(provider().getMemorySnapshot().cachedMemoryBytes()).isNotNull();
    }

    @Test
    void getCachedMemoryBytesShouldNotBeNegative() {
        assertThat(provider().getMemorySnapshot().cachedMemoryBytes()).isNotNegative();
    }

    @Test
    void getUsedMemoryBytesValueShouldNotBeNull() {
        assertThat(provider().getMemorySnapshot().usedMemoryBytes()).isNotNull();
    }

    @Test
    void getUsedMemoryBytesShouldNotBeNegative() {
        assertThat(provider().getMemorySnapshot().usedMemoryBytes()).isNotNegative();
    }
}
