package com.kotecku.kittop.memory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class LinuxMemoryInfoProviderTest extends MemoryInfoProviderContractTest {
    private static LinuxMemoryInfoProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new LinuxMemoryInfoProvider();
    }

    @Override
    protected MemoryInfoProvider provider() { return provider;}
}
