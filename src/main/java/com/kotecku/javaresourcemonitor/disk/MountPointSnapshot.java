package com.kotecku.javaresourcemonitor.disk;

public record MountPointSnapshot(String mountPoint,
                                 long totalDiskSpaceBytes,
                                 long usedDiskSpaceBytes,
                                 long freeDiskSpaceBytes
                                 //TODO Dodac IO%
) {
}
