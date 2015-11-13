/*
 * Copyright (C) 2015 Preetam D'Souza
 *
 */

package android.mperspective;

/**
 * Perspectives are interfaces to hardware.
 *
 * @hide
 */
public class Perspective {

    public static final int STATE_STOPPED = 0;
    public static final int STATE_STARTING = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_STOPPING = 3;

    public static String stateToString(int state) {
        switch (state) {
            case Perspective.STATE_STARTING:
                return "STARTING";
            case Perspective.STATE_RUNNING:
                return "RUNNING";
            case Perspective.STATE_STOPPING:
                return "STOPPING";
            case Perspective.STATE_STOPPED:
                return "STOPPED";
            default:
                return "UNKNOWN";
        }
    }

}
