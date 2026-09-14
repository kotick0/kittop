package com.kotecku.javaresourcemonitor.disk;

import com.kotecku.javaresourcemonitor.OnLinuxCondition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Conditional(OnLinuxCondition.class)
public class LinuxDiskInfoProvider implements DiskInfoProvider { //TODO Implementacja metod + refactor

    private CLinuxStatVfsLibrary.StatVfs callStatVfs(String path) {
        CLinuxStatVfsLibrary.StatVfs vfs = new CLinuxStatVfsLibrary.StatVfs();
        int result = CLinuxStatVfsLibrary.INSTANCE.statvfs(path, vfs);
        if (result != 0) {
            throw new IllegalStateException("statvfs() returned an error, code: " + result);
        }
        return vfs;
    }

    private List<String> getMountPoints() {
        try {
            return Files.readString(Path.of("/etc/fstab")).lines()
                    .filter(line -> !line.contains("#") && !line.isBlank())
                    .map(StringUtils::normalizeSpace)
                    .map(line -> line.split(" ")[1])
                    .collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HashMap<String, Long> getSwapData() {
        HashMap<String, Long> swapData = new HashMap<>();
        try {
            Files.readString(Path.of("/proc/meminfo")).lines()
                    .filter(line -> line.contains("MemTotal") || line.contains("MemFree"))
                    .map(line -> line.replace("kB", "").replace(" ", ""))
                    .map(line -> line.split(":"))
                    .forEach(line -> swapData.put(line[0], Long.parseLong(line[1]) * 1024));
            return swapData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DiskSnapshot getDiskSnapshot() {
        List<String> mountPoints = getMountPoints();
        List<MountPointSnapshot> mountPointSnapshots = new ArrayList<>();

        for (String mountPoint : mountPoints) {
            CLinuxStatVfsLibrary.StatVfs vfs = callStatVfs(mountPoint);
            long totalSpaceBytes = vfs.f_blocks * vfs.f_frsize;
            long freeSpaceBytes = vfs.f_bfree * vfs.f_frsize;
            long usedSpaceBytes = totalSpaceBytes - freeSpaceBytes;

            mountPointSnapshots.add(new MountPointSnapshot(mountPoint, totalSpaceBytes, usedSpaceBytes, freeSpaceBytes));
        }

        HashMap<String, Long> swapData = getSwapData();
        return new DiskSnapshot(mountPointSnapshots, swapData.get("MemTotal"), swapData.get("MemFree"),
                swapData.get("MemTotal") - swapData.get("MemFree"));
    }
}
