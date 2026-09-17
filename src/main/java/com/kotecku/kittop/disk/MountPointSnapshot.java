package com.kotecku.kittop.disk;

public record MountPointSnapshot(String mountPoint,
                                 String device,
                                 long totalDiskSpaceBytes,
                                 long usedDiskSpaceBytes,
                                 long freeDiskSpaceBytes
                                 //TODO Dodac IO%
) {
}
