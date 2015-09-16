/*
 * Copyright (C) 2015 Preetam D'Souza
 *
 */

package android.mperspective;

import android.util.Log;

/**
 * System private Java interface to PerspectiveService.
 *
 * @hide
 */
public final class PerspectiveManager {
    private static final String TAG = "PerspectiveManager";

    // wrapper to sp<IPerspectiveService>
    private long mNativeClient;

    private static native long nativeCreateClient();
    private static native boolean nativeStart(long ptr);
    private static native boolean nativeStop(long ptr);
    private static native boolean nativeIsRunning(long ptr);

    /**
     * TODO: make this a singleton?
     */
    public PerspectiveManager() {
        mNativeClient = nativeCreateClient();
        if (mNativeClient == 0) {
            Log.wtf(TAG, "nativeCreateClient() failed!");
        }
    }

    public boolean startDesktopPerspective() {
        return nativeStart(mNativeClient);
    }

    public boolean stopDesktopPerspective() {
        return nativeStop(mNativeClient);
    }

    public boolean isDesktopRunning() {
        return nativeIsRunning(mNativeClient);
    }
}
