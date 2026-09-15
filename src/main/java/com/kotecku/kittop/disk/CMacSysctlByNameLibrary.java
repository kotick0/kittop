package com.kotecku.kittop.disk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;

import java.util.List;

public interface CMacSysctlByNameLibrary extends Library {
    CMacSysctlByNameLibrary INSTANCE = Native.load("System", CMacSysctlByNameLibrary.class);

    int KERN_SUCCESS = 0;

    int sysctlbyname(String name, Pointer oldp, LongByReference oldlenp, Pointer newp, long newlen);

    class XswUsage extends Structure {
        public long xsu_total;
        public long xsu_avail;
        public long xsu_used;
        public int xsu_pagesize;
        public int xsu_encrypted;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("xsu_total", "xsu_avail", "xsu_used", "xsu_pagesize", "xsu_encrypted");
        }

    }
}
