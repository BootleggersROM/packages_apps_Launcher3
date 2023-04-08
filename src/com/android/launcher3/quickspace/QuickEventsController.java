/*
 * Copyright (C) 2018 CypherOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.quickspace;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

import java.util.Calendar;
import java.util.Random;

public class QuickEventsController {

    private static final String SETTING_DEVICE_INTRO_COMPLETED = "device_introduction_completed";
    private Context mContext;

    private String mEventTitle;
    private String mEventTitleSub;
    private String mGreetings;
    private String mClockExt;
    private OnClickListener mEventTitleSubAction = null;
    private int mEventSubIcon;

    private boolean mIsQuickEvent = false;
    private boolean mRunning = true;
    private boolean mRegistered = false;

    // Device Intro
    private boolean mIsFirstTimeDone = false;
    private SharedPreferences mPreferences;

    // PSA + Personality
    private String[] mPSAMorningStr;
    private String[] mPSAEvenStr;
    private String[] mPSAAfterNoonStr;
    private String[] mPSAMidniteStr;
    private String[] mPSARandomStr;
    private String[] mPSAEarlyEvenStr;
    private BroadcastReceiver mPSAListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            psonalityEvent();
        }
    };

    // NowPlaying
    private boolean mEventNowPlaying = false;
    private String mNowPlayingTitle;
    private String mNowPlayingArtist;
    private boolean mClientLost = true;
    private boolean mPlayingActive = false;

    public QuickEventsController(Context context) {
        mContext = context;
        initQuickEvents();
    }

    public void initQuickEvents() {
        mPreferences = mContext.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mIsFirstTimeDone = mPreferences.getBoolean(SETTING_DEVICE_INTRO_COMPLETED, false);
        registerPSAListener();
        updateQuickEvents();
    }

    private void registerPSAListener() {
        if (mRegistered) return;
        mRegistered = true;
        IntentFilter psonalityIntent = new IntentFilter();
        psonalityIntent.addAction(Intent.ACTION_TIME_TICK);
        psonalityIntent.addAction(Intent.ACTION_TIME_CHANGED);
        psonalityIntent.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        mContext.registerReceiver(mPSAListener, psonalityIntent);
    }

    private void unregisterPSAListener() {
        if (!mRegistered) return;
        mRegistered = false;
        mContext.unregisterReceiver(mPSAListener);
    }

    public void updateQuickEvents() {
        deviceIntroEvent();
        nowPlayingEvent();
        initNowPlayingEvent();
        psonalityEvent();
    }

    private void deviceIntroEvent() {
        if (!mRunning) return;

        if (mIsFirstTimeDone) return;
        mIsQuickEvent = true;
        mEventTitle = mContext.getResources().getString(R.string.quick_event_rom_intro_welcome);
        mEventTitleSub = mContext.getResources().getStringArray(R.array.welcome_message_variants)[getLuckyNumber(0,6)];
        mGreetings = mContext.getResources().getString(R.string.quickspace_grt_general);
        mClockExt = mContext.getResources().getString(R.string.quickspace_ext_three);
        mEventSubIcon = R.drawable.ic_quickspace_crdroid;

        mEventTitleSubAction = new OnClickListener() {
            @Override
            public void onClick(View view) {
                mContext.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(SETTING_DEVICE_INTRO_COMPLETED, true)
                        .commit();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    Launcher.getLauncher(mContext).startActivitySafely(view, intent, null);
                } catch (ActivityNotFoundException ex) {
                }
                mIsQuickEvent = false;
            }
        };
    }

    public void nowPlayingEvent() {
        if (mEventNowPlaying) {
            boolean infoExpired = !mPlayingActive || mClientLost;
            if (infoExpired) {
                mIsQuickEvent = false;
                mEventNowPlaying = false;
            }
        }
    }

    public void initNowPlayingEvent() {
        if (!mRunning) return;

        if (!mIsFirstTimeDone) return;

        if (!Utilities.isQuickspaceNowPlaying(mContext)) return;

        if (!mPlayingActive) return;

        if (mNowPlayingTitle == null) return;
        
        mEventTitle = mNowPlayingTitle;
        mGreetings = mContext.getResources().getString(R.string.qe_now_playing_ext_one);
        mClockExt = mContext.getResources().getString(R.string.qe_now_playing_ext_two);
        if (mNowPlayingArtist == null ) {
            mEventTitleSub = mContext.getResources().getString(R.string.qe_now_playing_unknown_artist);
        } else {
            mEventTitleSub = mNowPlayingArtist;
        }
        mEventSubIcon = R.drawable.ic_music_note_24dp;
        mIsQuickEvent = true;
        mEventNowPlaying = true;

        mEventTitleSubAction = new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayingActive) {
                    // Work required for local media actions
                    Intent npIntent = new Intent(Intent.ACTION_MAIN);
                    npIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
                    npIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        Launcher.getLauncher(mContext).startActivitySafely(view, npIntent, null);
                    } catch (ActivityNotFoundException ex) {
                    }
                }
            }
        };
    }

    public void psonalityEvent() {
        if (!mIsFirstTimeDone || mEventNowPlaying) return;
	
	mEventTitle = Utilities.formatDateTime(mContext);
        mPSAMorningStr = mContext.getResources().getStringArray(R.array.quickspace_psa_morning);
        mPSAEvenStr = mContext.getResources().getStringArray(R.array.quickspace_psa_evening);
        mPSAEarlyEvenStr = mContext.getResources().getStringArray(R.array.quickspace_psa_early_evening);
        mPSAMidniteStr = mContext.getResources().getStringArray(R.array.quickspace_psa_midnight);
        mPSAAfterNoonStr = mContext.getResources().getStringArray(R.array.quickspace_psa_noon);
        mPSARandomStr = mContext.getResources().getStringArray(R.array.quickspace_psa_random);
        mEventSubIcon = R.drawable.ic_quickspace_crdroid;
        int psaLength;

        // Clean the onClick event to avoid any weird behavior
        mEventTitleSubAction = new OnClickListener() {
            @Override
            public void onClick(View view) {
                // haha yes
            }
        };

        switch (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        
            case 5: case 6: case 7: case 8: case 9: case 10:
                psaLength = mPSAMorningStr.length - 1;
                mEventTitleSub = mPSAMorningStr[getLuckyNumber(0, psaLength)];
                mGreetings = mContext.getResources().getString(R.string.quickspace_grt_morning);
                mClockExt = mContext.getResources().getString(R.string.quickspace_ext_one);
                mIsQuickEvent = true;
                break;

            case 21: case 22: case 23:
                psaLength = mPSAEvenStr.length - 1;
                mEventTitleSub = mPSAEvenStr[getLuckyNumber(0, psaLength)];
                mGreetings = mContext.getResources().getString(R.string.quickspace_grt_evening);
                mClockExt = mContext.getResources().getString(R.string.quickspace_ext_three);
                mIsQuickEvent = true;
                break;

             case 18: case 19: case 20:
                psaLength = mPSAEarlyEvenStr.length - 1;
                mEventTitleSub = mPSAEarlyEvenStr[getLuckyNumber(0, psaLength)];
                mGreetings = mContext.getResources().getString(R.string.quickspace_grt_evening);
                mClockExt = mContext.getResources().getString(R.string.quickspace_ext_two);
                mIsQuickEvent = true;
                break;

            case 15: case 16: case 17:
                psaLength = mPSAAfterNoonStr.length - 1;
                mEventTitleSub = mPSAAfterNoonStr[getLuckyNumber(0, psaLength)];
                mGreetings = mContext.getResources().getString(R.string.quickspace_grt_afternoon);
                mClockExt = mContext.getResources().getString(R.string.quickspace_ext_three);
                mIsQuickEvent = true;
                break;

           case 0: case 1: case 2: case 3: case 4:
                psaLength = mPSAMidniteStr.length - 1;
                mEventTitleSub = mPSAMidniteStr[getLuckyNumber(0, psaLength)];
                mGreetings = mContext.getResources().getString(R.string.quickspace_grt_midnight);
                mClockExt = mContext.getResources().getString(R.string.quickspace_ext_three);
                mIsQuickEvent = true;
                break;

           case 11: case 12: case 13: case 14:
                psaLength = mPSARandomStr.length - 1;
                mEventTitleSub = mPSARandomStr[getLuckyNumber(0, psaLength)];
                mGreetings = mContext.getResources().getString(R.string.quickspace_grt_general);
                mClockExt = mContext.getResources().getString(R.string.quickspace_ext_three);
                mIsQuickEvent = true;
                break;

            default:
                if (getLuckyNumber(13) == 7) {
                    psaLength = mPSARandomStr.length - 1;
                    mEventTitleSub = mPSARandomStr[getLuckyNumber(0, psaLength)];
                    mIsQuickEvent = true;
                } else {
                    mIsQuickEvent = false;
                }
                break;
        }
    }

    public boolean isQuickEvent() {
        return mIsQuickEvent;
    }

    public boolean isDeviceIntroCompleted() {
        return mIsFirstTimeDone;
    }

    public String getTitle() {
        return mEventTitle;
    }

    public String getActionTitle() {
        return mEventTitleSub;
    }

    public String getClockExt() {
        return mClockExt;
    }

    public String getGreetings() {
        return mGreetings;
    }

    public OnClickListener getAction() {
        return mEventTitleSubAction;
    }

    public int getActionIcon() {
        return mEventSubIcon;
    }

    public int getLuckyNumber(int max) {
        return getLuckyNumber(0, max);
    }

    public int getLuckyNumber(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public void setMediaInfo(String title, String artist, boolean clientLost, boolean activePlayback) {
        mNowPlayingTitle = title;
        mNowPlayingArtist = artist;
        mClientLost = clientLost;
        mPlayingActive = activePlayback;
    }

    public boolean isNowPlaying() {
        return mPlayingActive;
    }

    public void onPause() {
        mRunning = false;
        unregisterPSAListener();
    }

    public void onResume() {
        mRunning = true;
        registerPSAListener();
    }
}
