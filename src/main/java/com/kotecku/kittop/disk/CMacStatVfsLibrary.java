package com.kotecku.kittop.disk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public interface CMacStatVfsLibrary extends Library {
    CMacStatVfsLibrary INSTANCE = Native.load("c", CMacStatVfsLibrary.class);

    int statvfs(String path, StatVfs buf);

    class StatVfs extends Structure {
        public long f_bsize;
        public long f_frsize;
        public int f_blocks;
        public int f_bfree;
        public int f_bavail;
        public int f_files;
        public int f_ffree;
        public int f_favail;
        public long f_fsid;
        public long f_flag;
        public long f_namemax;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("f_bsize", "f_frsize", "f_blocks", "f_bfree", "f_bavail",
                    "f_files", "f_ffree", "f_favail", "f_fsid", "f_flag", "f_namemax");
        }
    }
}
