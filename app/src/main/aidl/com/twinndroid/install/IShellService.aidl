package com.twinndroid.install;

// Runs inside Shizuku's shell-uid process.
interface IShellService {
    // Reserved transaction code Shizuku uses to tear the service down.
    void destroy() = 16777114;

    String installApks(in List<ParcelFileDescriptor> apks, in long[] sizes) = 1;
}
