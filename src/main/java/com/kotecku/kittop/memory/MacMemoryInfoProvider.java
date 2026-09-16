package com.kotecku.kittop.memory;

import com.kotecku.kittop.OnMacOsCondition;
import com.kotecku.kittop.exceptions.MemoryInfoException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import oshi.hardware.GlobalMemory;

import java.util.HashMap;

@Component
@Conditional(OnMacOsCondition.class)
@RequiredArgsConstructor
public class MacMemoryInfoProvider implements MemoryInfoProvider {

    private final GlobalMemory memory;

    private MachMacHostStatisticsLibrary.VMStatistics64 callVmStatistics64() {
        MachMacHostStatisticsLibrary.VMStatistics64 stats = new MachMacHostStatisticsLibrary.VMStatistics64();
        IntByReference count = new IntByReference(stats.size() / 4);

        int result = MachMacHostStatisticsLibrary.INSTANCE.host_statistics64(
                MachMacHostStatisticsLibrary.INSTANCE.mach_host_self(),
                MachMacHostStatisticsLibrary.HOST_VM_INFO64,
                stats,
                count
        );

        if (result != MachMacHostStatisticsLibrary.KERN_SUCCESS) {
            throw new MemoryInfoException("host_statistics64(HOST_VM_INFO64) returned an error: " + Native.getLastError());
        }
        return stats;
    }

    private long readPageSizeFromSysctl() {
        Memory sizeBuffer = new Memory(8);
        LongByReference sizeLength = new LongByReference(8L);

        int result = MachMacHostStatisticsLibrary.INSTANCE.sysctlbyname(
                "hw.pagesize",
                sizeBuffer,
                sizeLength,
                null,
                0L
        );

        if (result != MachMacHostStatisticsLibrary.KERN_SUCCESS) {
            throw new IllegalStateException(
                    "sysctlbyname(\"hw.pagesize\") returned an error: " + Native.getLastError());
        }

        return sizeBuffer.getLong(0);
    }

    private HashMap<String, Long> extractMemoryInfo() {
        HashMap<String, Long> memoryInfo = new HashMap<>();
        MachMacHostStatisticsLibrary.VMStatistics64 stats = callVmStatistics64();
        long pageSize = readPageSizeFromSysctl();
        long totalMemoryBytes = memory.getTotal();
        long availableMemoryBytes = (stats.active_count + (long) stats.wire_count) * pageSize;
        long freeMemoryBytes = stats.free_count * pageSize;
        long cachedMemoryBytes = stats.external_page_count * pageSize;
        long usedMemoryBytes = totalMemoryBytes - availableMemoryBytes;

        memoryInfo.put("MemTotal", totalMemoryBytes);
        memoryInfo.put("MemAvailable", availableMemoryBytes);
        memoryInfo.put("MemFree", freeMemoryBytes);
        memoryInfo.put("Cached", cachedMemoryBytes);
        memoryInfo.put("MemUsed", usedMemoryBytes);
        return memoryInfo;
    }

    @Override
    public MemorySnapshot getMemorySnapshot() {
        HashMap<String, Long> memoryInfo = extractMemoryInfo();
        return new MemorySnapshot(
                memoryInfo.get("MemTotal"),
                memoryInfo.get("MemAvailable"),
                memoryInfo.get("MemFree"),
                memoryInfo.get("Cached"),
                memoryInfo.get("MemUsed")
        );
    }
}
