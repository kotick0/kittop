package com.kotecku.javaresourcemonitor.disk;

import com.kotecku.javaresourcemonitor.OnMacOsCondition;
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

    private List<CGetMntInfoLibrary.Statfs> callGetMntInfo() {
        PointerByReference mntbufp = new PointerByReference();
        int count = CGetMntInfoLibrary.INSTANCE.getmntinfo(mntbufp, CGetMntInfoLibrary.MNT_NOWAIT);

        if (count == 0) {
            throw new IllegalStateException("getmntinfo() returned an error, code: " + count);
        }

        CGetMntInfoLibrary.Statfs statfs = new CGetMntInfoLibrary.Statfs(mntbufp.getValue());
        return Arrays.asList((CGetMntInfoLibrary.Statfs[]) statfs.toArray(count));
    }

    private CStatVfsLibrary.StatVfs callStatVfs(String path) {
        CStatVfsLibrary.StatVfs vfs = new CStatVfsLibrary.StatVfs();
        int result = CStatVfsLibrary.INSTANCE.statvfs(path, vfs);
        if (result != 0) {
            throw new IllegalStateException("statvfs() returned an error, code: " + result);
        }
        return vfs;
    }

    private CSysctlByNameLibrary.XswUsage callXswUsage() {
        CSysctlByNameLibrary.XswUsage xswUsage = new CSysctlByNameLibrary.XswUsage();
        LongByReference sizeLength = new LongByReference(xswUsage.size());

        int result = CSysctlByNameLibrary.INSTANCE.sysctlbyname("vm.swapusage", xswUsage.getPointer(), sizeLength, null, 0L);

        if (result != CSysctlByNameLibrary.KERN_SUCCESS) {
            throw new IllegalStateException("sysctlbyname(\"vm.swapusage\") returned an error, code: " + result);
        }

        xswUsage.read();
        return xswUsage;
    }

    private List<String> getMountPoints(List<CGetMntInfoLibrary.Statfs> mntInfo) {
        List<String> mountPoints = new ArrayList<>();
        for (CGetMntInfoLibrary.Statfs statfs : mntInfo) {
            if(!Native.toString(statfs.f_mntonname).equals("/dev") && !Native.toString(statfs.f_mntonname).equals("/System/Volumes/Data/home")) {
                mountPoints.add(Native.toString(statfs.f_mntonname));
            }
        }
        return mountPoints;
    }

    @Override
    public DiskSnapshot getDiskSnapshot() {
        List<CGetMntInfoLibrary.Statfs> mntInfo = callGetMntInfo();
        List<String> mountPoints = getMountPoints(mntInfo);
        List<MountPointSnapshot> mountPointSnapshots = new ArrayList<>();
        for(String mountPoint : mountPoints) {
            CStatVfsLibrary.StatVfs statVfs = callStatVfs(mountPoint);
            long totalSpaceBytes = statVfs.f_blocks * statVfs.f_frsize;
            long freeSpaceBytes = statVfs.f_bfree * statVfs.f_frsize;
            long usedSpaceBytes = totalSpaceBytes - freeSpaceBytes;
            mountPointSnapshots.add(new MountPointSnapshot(mountPoint, totalSpaceBytes, usedSpaceBytes, freeSpaceBytes));
        }

        CSysctlByNameLibrary.XswUsage xswUsage = callXswUsage();
        long swapTotalBytes = xswUsage.xsu_total;
        long swapFreeBytes = xswUsage.xsu_avail;
        long swapUsedBytes = xswUsage.xsu_used;

        return new DiskSnapshot(mountPointSnapshots, swapTotalBytes, swapFreeBytes, swapUsedBytes);
    }

}
