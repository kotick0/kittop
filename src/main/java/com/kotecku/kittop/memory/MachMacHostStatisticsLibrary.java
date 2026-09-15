package com.kotecku.kittop.memory;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import java.util.Arrays;
import java.util.List;

public interface MachMacHostStatisticsLibrary extends Library {
    MachMacHostStatisticsLibrary INSTANCE = Native.load("System", MachMacHostStatisticsLibrary.class);

    int HOST_VM_INFO64 = 4;
    int KERN_SUCCESS = 0;

    int host_statistics64(int hostPort, int hostFlavor, VMStatistics64 info, IntByReference count);
    int mach_host_self();
    int sysctlbyname(String name, Pointer oldp, LongByReference oldlenp, Pointer newp, long newlen);

    class VMStatistics64 extends Structure {
        public int free_count;
        public int active_count;
        public int inactive_count;
        public int wire_count;
        public long zero_fill_count;
        public long reactivations;
        public long pageins;
        public long pageouts;
        public long faults;
        public long cow_faults;
        public long lookups;
        public long hits;
        public long purges;
        public int purgeable_count;
        public int speculative_count;
        public long decompressions;
        public long compressions;
        public long swapins;
        public long swapouts;
        public int compressor_page_count;
        public int throttled_count;
        public int external_page_count;
        public int internal_page_count;
        public long total_uncompressed_pages_in_compressor;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("free_count", "active_count", "inactive_count", "wire_count",
                    "zero_fill_count", "reactivations", "pageins", "pageouts", "faults", "cow_faults",
                    "lookups", "hits", "purges", "purgeable_count", "speculative_count",
                    "decompressions", "compressions", "swapins", "swapouts",
                    "compressor_page_count", "throttled_count", "external_page_count",
                    "internal_page_count", "total_uncompressed_pages_in_compressor");
        }
    }
}
