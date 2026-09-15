package com.kotecku.kittop.cpu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import oshi.SystemInfo;

@EnabledOnOs(OS.LINUX)
class LinuxCpuInfoProviderTest extends CpuInfoProviderContractTest {
    private static LinuxCpuInfoProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new LinuxCpuInfoProvider(new SystemInfo().getHardware().getProcessor(), new SystemInfo().getHardware().getSensors());
    }

    @Override
    protected CpuInfoProvider provider() { return provider; }

}
