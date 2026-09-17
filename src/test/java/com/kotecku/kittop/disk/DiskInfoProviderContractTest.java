package com.kotecku.kittop.disk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class DiskInfoProviderContractTest {

    protected abstract DiskInfoProvider provider();

    @Test
    void getDiskSnapshot_returnsNonEmptyMountPoints() {
        DiskSnapshot snapshot = provider().getDiskSnapshot();

        assertNotNull(snapshot);
        assertFalse(snapshot.mountPoints().isEmpty());
    }

    @Test
    void getDiskSnapshot_mountPointSnapshotsHaveConsistentSpaceValues() {
        DiskSnapshot snapshot = provider().getDiskSnapshot();
        for (MountPointSnapshot mountPointSnapshot : snapshot.mountPoints()) {
            assertNotNull(mountPointSnapshot.mountPoint());
            assertTrue(mountPointSnapshot.totalDiskSpaceBytes() > 0);
            assertEquals(mountPointSnapshot.totalDiskSpaceBytes(),
                    mountPointSnapshot.usedDiskSpaceBytes() + mountPointSnapshot.freeDiskSpaceBytes());
        }
    }

    @Test
    void getDiskSnapshot_swapValuesAreConsistent() {
        DiskSnapshot snapshot = provider().getDiskSnapshot();

        assertEquals(snapshot.swapTotalBytes(),
                snapshot.swapUsedBytes() + snapshot.swapFreeBytes());
    }
}
