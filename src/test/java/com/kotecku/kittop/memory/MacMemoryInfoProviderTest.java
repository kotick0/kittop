package com.kotecku.kittop.memory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import oshi.SystemInfo;

@EnabledOnOs(OS.MAC)
@EnabledIfSystemProperty(named = "os.arch", matches = ".*aarch64.*")
class MacMemoryInfoProviderTest extends MemoryInfoProviderContractTest {
    private static MacMemoryInfoProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new MacMemoryInfoProvider(new SystemInfo().getHardware().getMemory());
    }

    @Override
    protected MemoryInfoProvider provider() {
        return provider;
    }
}
