package com.kotecku.kittop.disk;

import com.kotecku.kittop.OnLinuxCondition;
import com.kotecku.kittop.exceptions.DiskInfoException;
import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@Conditional(OnLinuxCondition.class)
public class LinuxDiskInfoProvider implements DiskInfoProvider {

    private CLinuxStatVfsLibrary.StatVfs callStatVfs(String path) {
        CLinuxStatVfsLibrary.StatVfs vfs = new CLinuxStatVfsLibrary.StatVfs();
        int result = CLinuxStatVfsLibrary.INSTANCE.statvfs(path, vfs);
        if (result != 0) {
            throw new DiskInfoException("statvfs() failed for " + path + ", error: " + Native.getLastError());
        }
        return vfs;
    }

    private List<String> getMountPoints() {
        List<String> physicalMounts;
        try {
            physicalMounts = Files.readString(Path.of("/proc/filesystems")).lines()
                    .filter(line -> !line.contains("squashfs") && !line.contains("nullfs"))
                    .map(line -> line.replace("nodev", ""))
                    .map(String::trim)
                    .toList();
        } catch (IOException e) {
            throw new DiskInfoException("Failed to read /proc/filesystems", e);
        }

        List<String> mountPoints = new ArrayList<>();
        List<String> fstabLines;
        try {
            fstabLines = Files.readString(Path.of("/etc/fstab")).lines()
                    .filter(line -> !line.contains("#") && !line.isBlank())
                    .map(StringUtils::normalizeSpace)
                    .toList();
        } catch (IOException e) {
            throw new DiskInfoException("Failed to read /etc/fstab", e);
        }

        for (String line : fstabLines) {
            String[] fields = line.split(" ");
            if (fields.length < 3) {
                log.warn("Skipping malformed /etc/fstab line: {}", line);
                continue;
            }
            if (physicalMounts.contains(fields[2])) {
                mountPoints.add(fields[1]);
            }
        }

        log.debug("Found {} physical mount points", mountPoints.size());
        return mountPoints;
    }

    private HashMap<String, Long> getSwapData() {
        HashMap<String, Long> swapData = new HashMap<>();
        try {
            Files.readString(Path.of("/proc/meminfo")).lines()
                    .filter(line -> line.contains("SwapTotal") || line.contains("SwapFree"))
                    .map(line -> line.replace("kB", "").replace(" ", ""))
                    .map(line -> line.split(":"))
                    .forEach(fields -> {
                        try {
                            swapData.put(fields[0], Long.parseLong(fields[1]) * 1024);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse /proc/meminfo line for {}", fields[0], e);
                        }
                    });
        } catch (IOException e) {
            throw new DiskInfoException("Failed to read /proc/meminfo", e);
        }

        if (!swapData.containsKey("SwapTotal") || !swapData.containsKey("SwapFree")) {
            throw new DiskInfoException("Required fields missing in /proc/meminfo: " + swapData.keySet());
        }
        return swapData;
    }

    private String resolveSymlink(String path) {
        try {
            return Path.of(path).toRealPath().toString();
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new DiskInfoException("Failed to resolve symlink for: " + path, e);
        }
    }

    private HashMap<String, String> getMountPointDevices(List<String> mountPoints) {
        HashMap<String, String> deviceNames = new HashMap<>();

        try {
            Files.readString(Path.of("/proc/mounts")).lines()
                    .map(line -> line.split(" "))
                    .filter(line -> mountPoints.contains(line[1]))
                    .forEach(line -> deviceNames.put(line[1], resolveSymlink(line[0])));
        } catch (IOException e) {
            throw new DiskInfoException("Failed to read /proc/mounts", e);
        }
        return deviceNames;
    }

    @Override
    public DiskSnapshot getDiskSnapshot() {
        List<String> mountPoints = getMountPoints();
        List<MountPointSnapshot> mountPointSnapshots = new ArrayList<>();
        HashMap<String, String> mountPointDevices = getMountPointDevices(mountPoints);

        for (String mountPoint : mountPoints) {
            try {
                CLinuxStatVfsLibrary.StatVfs vfs = callStatVfs(mountPoint);
                long totalSpaceBytes = vfs.f_blocks * vfs.f_frsize;
                long freeSpaceBytes = vfs.f_bfree * vfs.f_frsize;
                long usedSpaceBytes = totalSpaceBytes - freeSpaceBytes;

                String mountPointDevice = mountPointDevices.get(mountPoint);

                mountPointSnapshots.add(new MountPointSnapshot(mountPoint, mountPointDevice, totalSpaceBytes, usedSpaceBytes, freeSpaceBytes));
            } catch (DiskInfoException e) {
                log.warn("Skipping mount point {} due to statvfs error", mountPoint, e);
            }
        }

        HashMap<String, Long> swapData = getSwapData();
        long memTotal = swapData.get("SwapTotal");
        long memFree = swapData.get("SwapFree");
        return new DiskSnapshot(mountPointSnapshots, memTotal, memFree, memTotal - memFree);
    }
}