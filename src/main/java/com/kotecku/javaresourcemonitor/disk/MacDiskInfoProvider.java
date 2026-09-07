package com.kotecku.javaresourcemonitor.disk;

import com.kotecku.javaresourcemonitor.OnMacOsCondition;
import com.sun.jna.Native;
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

    @Override
    public List<String> getMountPoints() {
        List<String> mountPoints = new ArrayList<>();
        for (CGetMntInfoLibrary.Statfs statfs : callGetMntInfo()) {
            mountPoints.add(Native.toString(statfs.f_mntonname));
        }
        mountPoints.removeAll(List.of("/dev", "/System/Volumes/Data/home"));
//        mountPoints.replaceAll(mountPoint -> mountPoint.replace("/System/Volumes/", "")
//                .replace("/Volumes/", "").replace("/", "root")); //TODO Wyrzucic na "frontend"
        return mountPoints;
    }

    @Override
    public List<Long> getTotalDiskSpaceBytes() {
        List<Long> totalDiskSpace = new ArrayList<>();
        for (String mountPoint : getMountPoints()) {
            CStatVfsLibrary.StatVfs statVfs = callStatVfs(mountPoint);
            long mountPointTotalDiskSpace = statVfs.f_blocks * statVfs.f_frsize;
            totalDiskSpace.add(mountPointTotalDiskSpace);
        }
        return totalDiskSpace;
    }

    @Override
    public List<Long> getUsedDiskSpaceBytes() {
        List<Long> totalDiskSpace = getTotalDiskSpaceBytes();
        List<Long> freeDiskSpace = getFreeDiskSpaceBytes();
        List<Long> usedDiskSpace = new ArrayList<>();
        for(int i = 0; i < totalDiskSpace.size(); i++) {
            long mountPointUsedDiskSpace = totalDiskSpace.get(i) - freeDiskSpace.get(i);
            usedDiskSpace.add(mountPointUsedDiskSpace);
        }
        return usedDiskSpace;
    }

    @Override
    public List<Long> getFreeDiskSpaceBytes() {
        List<Long> freeDiskSpace = new ArrayList<>();
        for (String mountPoint : getMountPoints()) {
            CStatVfsLibrary.StatVfs statVfs = callStatVfs(mountPoint);
            long mountPointFreeDiskSpace = statVfs.f_bfree * statVfs.f_frsize;
            freeDiskSpace.add(mountPointFreeDiskSpace);
        }
        return freeDiskSpace;
    }

}
