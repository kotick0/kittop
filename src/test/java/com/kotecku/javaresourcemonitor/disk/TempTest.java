package com.kotecku.javaresourcemonitor.disk;

import org.junit.jupiter.api.Test;

public class TempTest {

    private final MacDiskInfoProvider provider = new MacDiskInfoProvider();

    @Test
    void testMountPointNames() {
        System.out.println(provider.getMountPoints());
    }

    @Test
    void testTotalDiskSpace() {
        System.out.println(provider.getTotalDiskSpaceBytes());
    }

    @Test
    void testUsedDiskSpace() {
        System.out.println(provider.getUsedDiskSpaceBytes());
    }

    @Test
    void testFreeDiskSpace() {
        System.out.println(provider.getFreeDiskSpaceBytes());
    }

}
