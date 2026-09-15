package com.kotecku.kittop.cpu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import oshi.SystemInfo;

@EnabledOnOs(OS.MAC)
@EnabledIfSystemProperty(named = "os.arch", matches = ".*aarch64.*")
class MacCpuInfoProviderTest extends CpuInfoProviderContractTest {
    private static MacCpuInfoProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new MacCpuInfoProvider(new SystemInfo().getHardware().getProcessor());
        provider.extractBinary();
    }

    @Override
    protected CpuInfoProvider provider() { return provider; }
}
