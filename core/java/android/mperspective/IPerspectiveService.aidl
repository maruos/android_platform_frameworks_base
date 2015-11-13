/*
 * Copyright (C) 2015 Preetam D'Souza
 *
 */

package android.mperspective;

import android.mperspective.IPerspectiveServiceCallback;

/**
 * @hide
 */
interface IPerspectiveService {

    void startDesktopPerspective();

    void stopDesktopPerspective();

    boolean isDesktopRunning();

    void registerCallback(IPerspectiveServiceCallback callback);
}
