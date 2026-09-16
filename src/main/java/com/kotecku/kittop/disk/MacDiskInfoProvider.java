package com.kotecku.kittop.disk;

import com.kotecku.kittop.OnMacOsCondition;
import com.kotecku.kittop.exceptions.DiskInfoException;
import com.sun.jna.Native;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Conditional(OnMacOsCondition.class)
public class MacDiskInfoProvider implements DiskInfoProvider {

    private List<CMacGetMntInfoLibrary.Statfs> callGetMntInfo() {
        PointerByReference mntbufp = new PointerByReference();
        int count = CMacGetMntInfoLibrary.INSTANCE.getmntinfo(mntbufp, CMacGetMntInfoLibrary.MNT_NOWAIT);

        if (count == 0) {
            throw new DiskInfoException("getmntinfo() returned an error: " + Native.getLastError());
        }

        CMacGetMntInfoLibrary.Statfs statfs = new CMacGetMntInfoLibrary.Statfs(mntbufp.getValue());
        return Arrays.asList((CMacGetMntInfoLibrary.Statfs[]) statfs.toArray(count));
    }

    private CMacStatVfsLibrary.StatVfs callStatVfs(String path) {
        CMacStatVfsLibrary.StatVfs vfs = new CMacStatVfsLibrary.StatVfs();
        int result = CMacStatVfsLibrary.INSTANCE.statvfs(path, vfs);
        if (result != 0) {
            throw new DiskInfoException("statvfs() failed for " + path + ", error: " + Native.getLastError());
        }
        return vfs;
    }

    private CMacSysctlByNameLibrary.XswUsage callXswUsage() {
        CMacSysctlByNameLibrary.XswUsage xswUsage = new CMacSysctlByNameLibrary.XswUsage();
        LongByReference sizeLength = new LongByReference(xswUsage.size());

        int result = CMacSysctlByNameLibrary.INSTANCE.sysctlbyname("vm.swapusage", xswUsage.getPointer(), sizeLength, null, 0L);

        if (result != CMacSysctlByNameLibrary.KERN_SUCCESS) {
            throw new DiskInfoException("sysctlbyname(\"vm.swapusage\") returned an error: " + Native.getLastError());
        }

        xswUsage.read();
        return xswUsage;
    }

    private List<String> getMountPoints(List<CMacGetMntInfoLibrary.Statfs> mntInfo) {
        List<String> mountPoints = new ArrayList<>();
        for (CMacGetMntInfoLibrary.Statfs statfs : mntInfo) {
            String mountPoint = Native.toString(statfs.f_mntonname);
            if(!mountPoint.equals("/dev") && !mountPoint.equals("/System/Volumes/Data/home")) {
                mountPoints.add(mountPoint);
            }
        }
        log.debug("Found {} physical mount points", mountPoints.size());
        return mountPoints;
    }

    @Override
    public DiskSnapshot getDiskSnapshot() {
        List<CMacGetMntInfoLibrary.Statfs> mntInfo = callGetMntInfo();
        List<String> mountPoints = getMountPoints(mntInfo);
        List<MountPointSnapshot> mountPointSnapshots = new ArrayList<>();

        for(String mountPoint : mountPoints) {
            try {
                CMacStatVfsLibrary.StatVfs statVfs = callStatVfs(mountPoint);
                long totalSpaceBytes = statVfs.f_blocks * statVfs.f_frsize;
                long freeSpaceBytes = statVfs.f_bfree * statVfs.f_frsize;
                long usedSpaceBytes = totalSpaceBytes - freeSpaceBytes;
                mountPointSnapshots.add(new MountPointSnapshot(mountPoint, totalSpaceBytes, usedSpaceBytes, freeSpaceBytes));
            } catch (DiskInfoException e) {
                log.warn("Skipping mount point {} due to statvfs error", mountPoint, e);
            }
        }

        CMacSysctlByNameLibrary.XswUsage xswUsage = callXswUsage();
        long swapTotalBytes = xswUsage.xsu_total;
        long swapFreeBytes = xswUsage.xsu_avail;
        long swapUsedBytes = xswUsage.xsu_used;

        return new DiskSnapshot(mountPointSnapshots, swapTotalBytes, swapFreeBytes, swapUsedBytes);
    }

}
