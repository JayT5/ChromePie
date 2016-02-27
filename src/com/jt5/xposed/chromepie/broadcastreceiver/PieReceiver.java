package com.jt5.xposed.chromepie.broadcastreceiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class PieReceiver extends BroadcastReceiver {

    public static final String FULLSCREEN_UPDATED_INTENT = "com.jt5.xposed.chromepie.intent.FULLSCREEN_UPDATED";

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(FULLSCREEN_UPDATED_INTENT)) {
            SharedPreferences prefs = context.getSharedPreferences(
                    context.getPackageName() + "_preferences", Context.MODE_WORLD_READABLE);
            boolean isFullscreen = intent.getBooleanExtra("IS_FULLSCREEN", false);
            prefs.edit().putBoolean("launch_in_fullscreen", isFullscreen).apply();
        }
    }

}
