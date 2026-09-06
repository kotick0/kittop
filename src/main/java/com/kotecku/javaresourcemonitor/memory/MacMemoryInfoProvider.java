package com.kotecku.javaresourcemonitor.memory;

import com.kotecku.javaresourcemonitor.OnMacOsCondition;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import oshi.hardware.GlobalMemory;

@Component
@Conditional(OnMacOsCondition.class)
@RequiredArgsConstructor
public class MacMemoryInfoProvider implements MemoryInfoProvider {

    private final GlobalMemory memory;

    private MachHostStatisticsLibrary.VMStatistics64 callVmStatistics64() {
        MachHostStatisticsLibrary.VMStatistics64 stats = new MachHostStatisticsLibrary.VMStatistics64();
        IntByReference count = new IntByReference(stats.size() / 4);

        int result = MachHostStatisticsLibrary.INSTANCE.host_statistics64(
                MachHostStatisticsLibrary.INSTANCE.mach_host_self(),
                MachHostStatisticsLibrary.HOST_VM_INFO64,
                stats,
                count
        );

        if (result != MachHostStatisticsLibrary.KERN_SUCCESS) {
            throw new IllegalStateException("host_statistics64(HOST_VM_INFO64) returned an error, code: " + result);
        }
        return stats;
    }

    private long readPageSizeFromSysctl() {
        Memory sizeBuffer = new Memory(8);
        LongByReference sizeLength = new LongByReference(8L);

        int result = MachHostStatisticsLibrary.INSTANCE.sysctlbyname(
                "hw.pagesize",
                sizeBuffer,
                sizeLength,
                null,
                0L
        );

        if (result != MachHostStatisticsLibrary.KERN_SUCCESS) {
            throw new IllegalStateException(
                    "sysctlbyname(\"hw.pagesize\") returned an error, code: " + result);
        }

        return sizeBuffer.getLong(0);
    }

    @Override
    public long getTotalMemoryBytes() {
        return memory.getTotal();
    }

    @Override
    public long getAvailableMemoryBytes() {
        MachHostStatisticsLibrary.VMStatistics64 stats = callVmStatistics64();

        long pageSize = readPageSizeFromSysctl();
        long used = (stats.active_count + (long) stats.wire_count) * pageSize;

        return (memory.getTotal() - used);
    }

    @Override
    public long getFreeMemoryBytes() {
        MachHostStatisticsLibrary.VMStatistics64 stats = callVmStatistics64();
        return (stats.free_count * readPageSizeFromSysctl());
    }

    @Override
    public long getCachedMemoryBytes() {
        MachHostStatisticsLibrary.VMStatistics64 stats = callVmStatistics64();
        return (stats.external_page_count * readPageSizeFromSysctl());
    }

    @Override
    public long getUsedMemoryBytes() {
        MachHostStatisticsLibrary.VMStatistics64 stats = callVmStatistics64();
        return (stats.active_count + (long) stats.wire_count) * readPageSizeFromSysctl();
    }
}
