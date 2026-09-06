package com.kotecku.javaresourcemonitor.disk;

import java.util.List;

public interface DiskInfoProvider {
    List<String> getMountPoints();
    List<Long> getTotalDiskSpaceBytes();
    List<Long> getUsedDiskSpaceBytes();
    List<Long> getFreeDiskSpaceBytes();
    //TODO Dodac IO%
}
