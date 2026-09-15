package com.kotecku.kittop.disk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.MAC)
@EnabledIfSystemProperty(named = "os.arch", matches = ".*aarch64.*")
class MacDiskInfoProviderTest extends DiskInfoProviderContractTest {
    private static MacDiskInfoProvider provider;

    @BeforeAll
    static void setUp() {
        provider = new MacDiskInfoProvider();
    }

    @Override
    protected MacDiskInfoProvider provider() {
        return provider;
    }
}
