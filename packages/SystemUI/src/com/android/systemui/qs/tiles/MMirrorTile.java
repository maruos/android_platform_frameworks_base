/*
 * Copyright 2015-2016 Preetam J. D'Souza
 * Copyright 2016 The Maru OS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import java.util.HashSet;
import java.util.Set;

/** Quick settings tile: Mirror phone **/
public class MMirrorTile extends QSTile<QSTile.BooleanState> {
    private static final String TAG = "MMirrorTile";

    private static final int mDisabledIcon = R.drawable.ic_mirroring_disabled;
    private static final int mEnabledIcon = R.drawable.ic_mirroring_enabled;

    private final DisplayManager mDisplayManager;

    private final MDisplayListener mDisplayListener;
    private Set<Integer> mPresentationDisplays;
    private boolean mListening = false;

    public MMirrorTile(Host host) {
        super(host);

        mDisplayManager = (DisplayManager) host.getContext()
                .getSystemService(Context.DISPLAY_SERVICE);

        mDisplayListener = new MDisplayListener();
        mPresentationDisplays = new HashSet<Integer>();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        if (mState.value) {
            mDisplayManager.disablePhoneMirroring();
        } else {
            mDisplayManager.enablePhoneMirroring();
        }
        refreshState();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean hasPresentationDisplay = !mPresentationDisplays.isEmpty();
        state.visible = hasPresentationDisplay;
        state.value = mDisplayManager.isPhoneMirroringEnabled();
        state.label = mContext.getString(R.string.quick_settings_mirroring_mode_label);
        state.icon = ResourceIcon.get(state.value ? mEnabledIcon : mDisabledIcon);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_qs_mirroring_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_qs_mirroring_changed_off);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_MMIRROR_TOGGLE;
    }

    @Override
    public void setListening(boolean listening) {
        // defense against duplicate registers
        if (mListening == listening) {
            return;
        }

        if (listening) {
            // Log.d(TAG, "registering mDisplayListener");
            mDisplayListener.sync();
            mDisplayManager.registerDisplayListener(mDisplayListener, null);
        } else {
            // Log.d(TAG, "unregistering mDisplayListener");
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        }
        mListening = listening;
    }

    private class MDisplayListener implements DisplayManager.DisplayListener {
        /**
         * Keep track of public presentation displays. These are displays that will show either
         * Maru Desktop or the mirrored phone screen.
         */

        @Override
        public void onDisplayAdded(int displayId) {
            Display display = mDisplayManager.getDisplay(displayId);

            if (display.isPublicPresentation()) {
                if (mPresentationDisplays.isEmpty()) {
                    // the first presentation display was added
                    refreshState();
                }
                mPresentationDisplays.add(displayId);
            }
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            if (mPresentationDisplays.remove(displayId) && mPresentationDisplays.isEmpty()) {
                // the last presentation display was removed
                refreshState();
            }
        }

        @Override
        public void onDisplayChanged(int displayId) { /* no-op */ }

        /**
         * We may miss a display event since listeners are unregistered
         * when the QS panel is hidden.
         *
         * Call this before registering to make sure the initial
         * state is up-to-date.
         */
        public void sync() {
            mPresentationDisplays.clear();
            Display[] displays = mDisplayManager
                    .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
            for (Display display : displays) {
                if (display.isPublicPresentation()) {
                    mPresentationDisplays.add(display.getDisplayId());
                }
            }
        }
    }
}
