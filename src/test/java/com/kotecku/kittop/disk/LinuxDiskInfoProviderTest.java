package com.kotecku.kittop.disk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class LinuxDiskInfoProviderTest extends DiskInfoProviderContractTest {
    private static LinuxDiskInfoProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new LinuxDiskInfoProvider();
    }

    @Override
    protected LinuxDiskInfoProvider provider() {
        return provider;
    }
}
