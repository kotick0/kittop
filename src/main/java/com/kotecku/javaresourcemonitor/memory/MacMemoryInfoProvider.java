package com.kotecku.javaresourcemonitor.memory;

import com.kotecku.javaresourcemonitor.OnMacOsCondition;
import com.sun.jna.Memory;
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

    private HashMap<String, Long> extractMemoryInfo() {
        HashMap<String, Long> memoryInfo = new HashMap<>();
        MachHostStatisticsLibrary.VMStatistics64 stats = callVmStatistics64();
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
