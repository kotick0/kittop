package com.kotecku.kittop.disk;

import com.kotecku.kittop.OnMacOsCondition;
import com.sun.jna.Native;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Conditional(OnMacOsCondition.class)
public class MacDiskInfoProvider implements DiskInfoProvider {

    private List<CMacGetMntInfoLibrary.Statfs> callGetMntInfo() {
        PointerByReference mntbufp = new PointerByReference();
        int count = CMacGetMntInfoLibrary.INSTANCE.getmntinfo(mntbufp, CMacGetMntInfoLibrary.MNT_NOWAIT);

        if (count == 0) {
            throw new IllegalStateException("getmntinfo() returned an error, code: " + count);
        }

        CMacGetMntInfoLibrary.Statfs statfs = new CMacGetMntInfoLibrary.Statfs(mntbufp.getValue());
        return Arrays.asList((CMacGetMntInfoLibrary.Statfs[]) statfs.toArray(count));
    }

    private CMacStatVfsLibrary.StatVfs callStatVfs(String path) {
        CMacStatVfsLibrary.StatVfs vfs = new CMacStatVfsLibrary.StatVfs();
        int result = CMacStatVfsLibrary.INSTANCE.statvfs(path, vfs);
        if (result != 0) {
            throw new IllegalStateException("statvfs() returned an error, code: " + result);
        }
        return vfs;
    }

    private CMacSysctlByNameLibrary.XswUsage callXswUsage() {
        CMacSysctlByNameLibrary.XswUsage xswUsage = new CMacSysctlByNameLibrary.XswUsage();
        LongByReference sizeLength = new LongByReference(xswUsage.size());

        int result = CMacSysctlByNameLibrary.INSTANCE.sysctlbyname("vm.swapusage", xswUsage.getPointer(), sizeLength, null, 0L);

        if (result != CMacSysctlByNameLibrary.KERN_SUCCESS) {
            throw new IllegalStateException("sysctlbyname(\"vm.swapusage\") returned an error, code: " + result);
        }

        xswUsage.read();
        return xswUsage;
    }

    private List<String> getMountPoints(List<CMacGetMntInfoLibrary.Statfs> mntInfo) {
        List<String> mountPoints = new ArrayList<>();
        for (CMacGetMntInfoLibrary.Statfs statfs : mntInfo) {
            if(!Native.toString(statfs.f_mntonname).equals("/dev") && !Native.toString(statfs.f_mntonname).equals("/System/Volumes/Data/home")) {
                mountPoints.add(Native.toString(statfs.f_mntonname));
            }
        }
        return mountPoints;
    }

    @Override
    public DiskSnapshot getDiskSnapshot() {
        List<CMacGetMntInfoLibrary.Statfs> mntInfo = callGetMntInfo();
        List<String> mountPoints = getMountPoints(mntInfo);
        List<MountPointSnapshot> mountPointSnapshots = new ArrayList<>();
        for(String mountPoint : mountPoints) {
            CMacStatVfsLibrary.StatVfs statVfs = callStatVfs(mountPoint);
            long totalSpaceBytes = statVfs.f_blocks * statVfs.f_frsize;
            long freeSpaceBytes = statVfs.f_bfree * statVfs.f_frsize;
            long usedSpaceBytes = totalSpaceBytes - freeSpaceBytes;
            mountPointSnapshots.add(new MountPointSnapshot(mountPoint, totalSpaceBytes, usedSpaceBytes, freeSpaceBytes));
        }

        CMacSysctlByNameLibrary.XswUsage xswUsage = callXswUsage();
        long swapTotalBytes = xswUsage.xsu_total;
        long swapFreeBytes = xswUsage.xsu_avail;
        long swapUsedBytes = xswUsage.xsu_used;

        return new DiskSnapshot(mountPointSnapshots, swapTotalBytes, swapFreeBytes, swapUsedBytes);
    }

}
