/*
 * Copyright (C) 2015 Preetam D'Souza
 *
 * Inspired by AirplaneModeTile.java
 *
 */

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings.Global;
import android.util.Log;

import android.mperspective.PerspectiveManager;
import android.hardware.display.DisplayManager;
import android.view.Display;

import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Maru Desktop **/
public class MDesktopTile extends QSTile<QSTile.BooleanState> {
    private static final String TAG = "MDesktopTile";

    private static final int mDisabledIcon = R.drawable.ic_mdesktop_off;
    private static final int mEnabledIcon = R.drawable.ic_mdesktop_on;

    private final DisplayManager mDisplayManager;
    private final PerspectiveManager mPerspectiveManager;

    private final MDisplayListener mDisplayListener;
    // com.android.internal.R.string.display_manager_hdmi_display_name
    private final String mHDMIDisplayName;
    // track the hdmi display id to check if it has been removed later
    private int mHdmiDisplayId = -1;

    private boolean mListening = false;

    public MDesktopTile(Host host) {
        super(host);

        mDisplayManager = (DisplayManager) host.getContext()
                .getSystemService(Context.DISPLAY_SERVICE);
        mPerspectiveManager = new PerspectiveManager();

        mDisplayListener = new MDisplayListener();
        mHDMIDisplayName = host.getContext().getResources()
                .getString(com.android.internal.R.string.display_manager_hdmi_display_name);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        if (mState.value) {
            Log.d(TAG, "calling stop()!");
            boolean res = mPerspectiveManager.stopDesktopPerspective();
            Log.d(TAG, "...returned " + res);
        } else {
            Log.d(TAG, "calling start()!");
            boolean res = mPerspectiveManager.startDesktopPerspective();
            Log.d(TAG, "...returned " + res);
        }
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean isRunning = mPerspectiveManager.isDesktopRunning();
        final boolean hasHdmiDisplay = mHdmiDisplayId != -1;
        Log.d(TAG, "hasHdmiDisplay: " + hasHdmiDisplay);
        state.visible = true; //hasHdmiDisplay || isRunning;
        state.value = isRunning;
        state.label = mContext.getString(R.string.quick_settings_mdesktop_mode_label);
        state.icon = ResourceIcon.get(state.value ? mEnabledIcon : mDisabledIcon);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_qs_mdesktop_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_qs_mdesktop_changed_off);
        }
    }

    @Override
    public void setListening(boolean listening) {
        // defense against duplicate registers
        if (mListening == listening) {
            return;
        }

        if (listening) {
            Log.d(TAG, "registering mDisplayListener");
            mDisplayManager.registerDisplayListener(mDisplayListener, null);
        } else {
            Log.d(TAG, "unregistering mDisplayListener");
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        }
        mListening = listening;
    }

    private class MDisplayListener implements DisplayManager.DisplayListener {
        @Override
        public void onDisplayAdded(int displayId) {
            Display display = mDisplayManager.getDisplay(displayId);
            Log.d(TAG, "Display added: " + display);
            final boolean hdmiDisplayAdded = mHDMIDisplayName.equals(display.getName()) &&
                        display.getState() == Display.STATE_ON;

            if (hdmiDisplayAdded) {
                if (mHdmiDisplayId == -1) {
                    mHdmiDisplayId = displayId;
                    refreshState();
                }
            }
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            if (displayId == mHdmiDisplayId) {
                if (mHdmiDisplayId != -1) {
                    mHdmiDisplayId = -1;
                    refreshState();
                }
            }
        }

        @Override
        public void onDisplayChanged(int displayId) { /* no-op */ }
    }
}
