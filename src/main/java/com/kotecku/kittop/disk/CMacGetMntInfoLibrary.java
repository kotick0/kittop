package com.kotecku.kittop.disk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

public interface CMacGetMntInfoLibrary extends Library {
    CMacGetMntInfoLibrary INSTANCE = Native.load("c", CMacGetMntInfoLibrary.class);

    int MNT_NOWAIT = 2;

    int getmntinfo(PointerByReference mntbufp, int flags);

    class Statfs extends Structure {
        public int f_bsize;
        public int f_iosize;
        public long f_blocks;
        public long f_bfree;
        public long f_bavail;
        public long f_files;
        public long f_ffree;
        public int[] f_fsid = new int[2];
        public int f_owner;
        public int f_type;
        public int f_flags;
        public int f_fssubtype;
        public byte[] f_fstypename = new byte[16];
        public byte[] f_mntonname = new byte[1024];
        public byte[] f_mntfromname = new byte[1024];
        public int[] f_reserved = new int[8];

        public Statfs(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("f_bsize", "f_iosize", "f_blocks", "f_bfree", "f_bavail",
                    "f_files", "f_ffree", "f_fsid", "f_owner", "f_type",
                    "f_flags", "f_fssubtype", "f_fstypename", "f_mntonname",
                    "f_mntfromname", "f_reserved");
        }

    }
}
