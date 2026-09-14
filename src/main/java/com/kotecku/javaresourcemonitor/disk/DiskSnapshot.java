package com.kotecku.javaresourcemonitor.disk;

import java.util.List;

public record DiskSnapshot(List<MountPointSnapshot> mountPoints,
                           long swapTotalBytes,
                           long swapFreeBytes,
                           long swapUsedBytes) {
}
