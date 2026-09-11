package com.kotecku.javaresourcemonitor.disk;

import com.kotecku.javaresourcemonitor.OnLinuxCondition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@Conditional(OnLinuxCondition.class)
public class LinuxDiskInfoProvider implements DiskInfoProvider { //TODO Implementacja metod

    @Override
    public List<String> getMountPoints() {
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

    @Override
    public List<Long> getTotalDiskSpaceBytes() {
        return null;
    }

    @Override
    public List<Long> getUsedDiskSpaceBytes() {
        return null;
    }

    @Override
    public List<Long> getFreeDiskSpaceBytes() {
        return null;
    }

    @Override
    public long getSwapTotalBytes() {
        return 0;
    }

    @Override
    public long getSwapFreeBytes() {
        return 0;
    }
}
