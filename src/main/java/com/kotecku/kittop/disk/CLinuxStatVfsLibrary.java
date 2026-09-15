package com.kotecku.kittop.disk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public interface CLinuxStatVfsLibrary extends Library {
    CLinuxStatVfsLibrary INSTANCE = Native.load("c", CLinuxStatVfsLibrary.class);

    int statvfs(String path, StatVfs buf);

    class StatVfs extends Structure {
        public long f_bsize;
        public long f_frsize;
        public long f_blocks;
        public long f_bfree;
        public long f_bavail;
        public long f_files;
        public long f_ffree;
        public long f_favail;
        public long f_fsid;
        public long f_flag;
        public long f_namemax;
        public int[] __f_spare = new int[6];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("f_bsize", "f_frsize", "f_blocks", "f_bfree", "f_bavail",
                    "f_files", "f_ffree", "f_favail", "f_fsid", "f_flag", "f_namemax", "__f_spare");
        }
    }
}